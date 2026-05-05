package certifiedcarry_api.storage.service;

import certifiedcarry_api.shared.TextNormalization;
import certifiedcarry_api.storage.config.ObjectStorageProperties;
import java.net.URI;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
public class ObjectUploadService {

  private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES =
      Set.of("image/jpeg", "image/png", "image/webp", "image/gif", "image/heic", "image/heif");

  private static final Map<String, String> CONTENT_TYPE_EXTENSIONS =
      Map.of(
          "image/jpeg", ".jpg",
          "image/png", ".png",
          "image/webp", ".webp",
          "image/gif", ".gif",
          "image/heic", ".heic",
          "image/heif", ".heif");

  private final ObjectStorageProperties objectStorageProperties;
  private final ObjectProvider<S3Presigner> s3PresignerProvider;

  public ObjectUploadService(
      ObjectStorageProperties objectStorageProperties, ObjectProvider<S3Presigner> s3PresignerProvider) {
    this.objectStorageProperties = objectStorageProperties;
    this.s3PresignerProvider = s3PresignerProvider;
  }

  public PresignedUpload createPresignedUpload(
      long actorUserId,
      UploadAssetType assetType,
      String fileName,
      String contentType,
      Long fileSizeBytes) {
    if (!objectStorageProperties.isEnabled()) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Object storage upload is not enabled.");
    }

    if (actorUserId <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Actor user id must be positive.");
    }

    String normalizedBucket = requireNonBlank(objectStorageProperties.getBucket(), "OBJECT_STORAGE_BUCKET");
    String normalizedContentType = normalizeContentType(contentType);
    validateFileSize(fileSizeBytes);

    S3Presigner s3Presigner = s3PresignerProvider.getIfAvailable();
    if (s3Presigner == null) {
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Object storage signer is not initialized.");
    }

    String objectKey = buildObjectKey(actorUserId, assetType, fileName, normalizedContentType);

    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder()
            .bucket(normalizedBucket)
            .key(objectKey)
            .contentType(normalizedContentType)
        .acl(ObjectCannedACL.PUBLIC_READ)
            .build();

    int expirySeconds = Math.max(60, objectStorageProperties.getPresignExpirySeconds());
    PutObjectPresignRequest presignRequest =
        PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(expirySeconds))
            .putObjectRequest(putObjectRequest)
            .build();

    PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

    return new PresignedUpload(
        presignedRequest.url().toString(),
        buildPublicUrl(objectKey, normalizedBucket),
        objectKey,
      Map.of("Content-Type", normalizedContentType, "x-amz-acl", "public-read"),
        OffsetDateTime.now().plusSeconds(expirySeconds));
  }

  private String normalizeContentType(String contentType) {
    String normalized = requireNonBlank(contentType, "contentType").toLowerCase(Locale.ROOT);
    if ("image/jpg".equals(normalized)) {
      normalized = "image/jpeg";
    }

    if (!ALLOWED_IMAGE_CONTENT_TYPES.contains(normalized)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image uploads are supported.");
    }

    return normalized;
  }

  private void validateFileSize(Long fileSizeBytes) {
    if (fileSizeBytes == null) {
      return;
    }

    if (fileSizeBytes <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileSizeBytes must be positive.");
    }

    long maxUploadBytes = Math.max(1L, objectStorageProperties.getMaxUploadBytes());
    if (fileSizeBytes > maxUploadBytes) {
      throw new ResponseStatusException(
          HttpStatus.PAYLOAD_TOO_LARGE,
          "File exceeds max upload size of " + maxUploadBytes + " bytes.");
    }
  }

  private String buildObjectKey(
      long actorUserId, UploadAssetType assetType, String fileName, String contentType) {
    String extension = resolveExtension(fileName, contentType);
    String normalizedPrefix = normalizePrefix(objectStorageProperties.getKeyPrefix());

    return normalizedPrefix
        + "/users/"
        + actorUserId
        + "/"
        + assetType.pathSegment()
        + "/"
        + UUID.randomUUID()
        + extension;
  }

  private String resolveExtension(String fileName, String contentType) {
    String normalizedFileName = String.valueOf(fileName == null ? "" : fileName).trim();
    int extensionSeparator = normalizedFileName.lastIndexOf('.');

    if (extensionSeparator > -1 && extensionSeparator < normalizedFileName.length() - 1) {
      String rawExtension = normalizedFileName.substring(extensionSeparator + 1).toLowerCase(Locale.ROOT);
      if (rawExtension.matches("[a-z0-9]{1,10}")) {
        return "." + rawExtension;
      }
    }

    return CONTENT_TYPE_EXTENSIONS.getOrDefault(contentType, "");
  }

  private String normalizePrefix(String rawPrefix) {
    String normalized = normalizeOptionalText(rawPrefix);
    if (normalized == null) {
      return "player-assets";
    }

    if (normalized.startsWith("/")) {
      normalized = normalized.substring(1);
    }

    return normalized.isEmpty() ? "player-assets" : normalized;
  }

  private String buildPublicUrl(String objectKey, String bucket) {
    String configuredPublicBaseUrl = normalizeOptionalText(objectStorageProperties.getPublicBaseUrl());
    if (configuredPublicBaseUrl != null) {
      return configuredPublicBaseUrl + "/" + objectKey;
    }

    String endpoint = requireNonBlank(objectStorageProperties.getEndpoint(), "OBJECT_STORAGE_ENDPOINT");
    URI endpointUri = URI.create(endpoint);

    String scheme = endpointUri.getScheme() == null ? "https" : endpointUri.getScheme();
    String host = endpointUri.getHost();
    if (host == null || host.isBlank()) {
      throw new ResponseStatusException(
          HttpStatus.INTERNAL_SERVER_ERROR, "Object storage endpoint host is invalid.");
    }

    if (host.endsWith("digitaloceanspaces.com")) {
      return scheme + "://" + bucket + "." + host + "/" + objectKey;
    }

    StringBuilder fallbackUrl = new StringBuilder();
    fallbackUrl.append(scheme).append("://").append(host);
    if (endpointUri.getPort() > -1) {
      fallbackUrl.append(":").append(endpointUri.getPort());
    }
    fallbackUrl.append("/").append(bucket).append("/").append(objectKey);
    return fallbackUrl.toString();
  }

  private String requireNonBlank(String value, String fieldName) {
    String normalized = TextNormalization.trimToNull(value);
    if (normalized == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " cannot be blank.");
    }

    return normalized;
  }

  private String normalizeOptionalText(String value) {
    String normalized = TextNormalization.trimToNull(value);
    if (normalized == null) {
      return null;
    }

    while (normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }

    return normalized.isBlank() ? null : normalized;
  }

  public enum UploadAssetType {
    PROFILE_IMAGE("profile-image"),
    PROOF_IMAGE("proof-image"),
    CHAT_IMAGE("chat-image");

    private final String pathSegment;

    UploadAssetType(String pathSegment) {
      this.pathSegment = pathSegment;
    }

    public String pathSegment() {
      return pathSegment;
    }

    public static UploadAssetType fromRequestValue(String value) {
      String normalized = String.valueOf(value == null ? "" : value).trim().toUpperCase(Locale.ROOT);

      return switch (normalized) {
        case "PROFILE_IMAGE" -> PROFILE_IMAGE;
        case "PROOF_IMAGE" -> PROOF_IMAGE;
        case "CHAT_IMAGE" -> CHAT_IMAGE;
        default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported assetType.");
      };
    }
  }

  public record PresignedUpload(
      String uploadUrl,
      String publicUrl,
      String objectKey,
      Map<String, String> requiredHeaders,
      OffsetDateTime expiresAt) {}
}
