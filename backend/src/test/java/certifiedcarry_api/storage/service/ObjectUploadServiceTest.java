package certifiedcarry_api.storage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import certifiedcarry_api.storage.config.ObjectStorageProperties;
import java.net.URL;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

class ObjectUploadServiceTest {

  @Test
  void createPresignedUploadValidatesAvailabilityActorContentTypeAndSize() {
    ObjectStorageProperties disabledProperties = new ObjectStorageProperties();
    disabledProperties.setEnabled(false);
    ObjectUploadService disabledService =
        new ObjectUploadService(disabledProperties, providerWith(null));

    ResponseStatusException disabledFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                disabledService.createPresignedUpload(
                    7L,
                    ObjectUploadService.UploadAssetType.PROFILE_IMAGE,
                    "avatar.png",
                    "image/png",
                    1024L));
    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, disabledFailure.getStatusCode());

    ObjectStorageProperties properties = baseProperties();
    ObjectUploadService service = new ObjectUploadService(properties, providerWith(null));

    ResponseStatusException actorFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createPresignedUpload(
                    0L,
                    ObjectUploadService.UploadAssetType.PROFILE_IMAGE,
                    "avatar.png",
                    "image/png",
                    1024L));
    assertEquals("Actor user id must be positive.", actorFailure.getReason());

    ResponseStatusException typeFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createPresignedUpload(
                    7L,
                    ObjectUploadService.UploadAssetType.PROFILE_IMAGE,
                    "avatar.bmp",
                    "image/bmp",
                    1024L));
    assertEquals("Only image uploads are supported.", typeFailure.getReason());

    ResponseStatusException sizeFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createPresignedUpload(
                    7L,
                    ObjectUploadService.UploadAssetType.PROFILE_IMAGE,
                    "avatar.png",
                    "image/png",
                    properties.getMaxUploadBytes() + 1));
    assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, sizeFailure.getStatusCode());
  }

  @Test
  void createPresignedUploadBuildsDigitalOceanAndConfiguredPublicUrls() throws Exception {
    ObjectStorageProperties digitalOceanProperties = baseProperties();
    S3Presigner presigner = mock(S3Presigner.class);
    PresignedPutObjectRequest presignedRequest = mock(PresignedPutObjectRequest.class);
    when(presignedRequest.url()).thenReturn(new URL("https://upload.example.com/put"));
    when(presigner.presignPutObject(any(PutObjectPresignRequest.class))).thenReturn(presignedRequest);

    ObjectUploadService digitalOceanService =
        new ObjectUploadService(digitalOceanProperties, providerWith(presigner));

    ObjectUploadService.PresignedUpload digitalOceanUpload =
        digitalOceanService.createPresignedUpload(
            7L,
            ObjectUploadService.UploadAssetType.PROFILE_IMAGE,
            "avatar.jpg",
            "image/jpg",
            2048L);

    assertEquals("https://upload.example.com/put", digitalOceanUpload.uploadUrl());
    assertTrue(digitalOceanUpload.objectKey().startsWith("media/users/7/profile-image/"));
    assertTrue(digitalOceanUpload.objectKey().endsWith(".jpg"));
    assertTrue(
        digitalOceanUpload.publicUrl().startsWith("https://carry-bucket.nyc3.digitaloceanspaces.com/media/users/7/profile-image/"));
    assertEquals("image/jpeg", digitalOceanUpload.requiredHeaders().get("Content-Type"));
    assertEquals("public-read", digitalOceanUpload.requiredHeaders().get("x-amz-acl"));
    assertNotNull(digitalOceanUpload.expiresAt());

    ObjectStorageProperties customPublicBase = baseProperties();
    customPublicBase.setPublicBaseUrl("https://cdn.certifiedcarry.me/assets/");
    ObjectUploadService customPublicBaseService =
        new ObjectUploadService(customPublicBase, providerWith(presigner));

    ObjectUploadService.PresignedUpload customUpload =
        customPublicBaseService.createPresignedUpload(
            9L,
            ObjectUploadService.UploadAssetType.CHAT_IMAGE,
            "proof",
            "image/png",
            null);

    assertTrue(customUpload.objectKey().startsWith("media/users/9/chat-image/"));
    assertTrue(customUpload.objectKey().endsWith(".png"));
    assertTrue(customUpload.publicUrl().startsWith("https://cdn.certifiedcarry.me/assets/"));
  }

  @Test
  void uploadAssetTypeParsingRejectsUnsupportedValues() {
    assertEquals(
        ObjectUploadService.UploadAssetType.CHAT_IMAGE,
        ObjectUploadService.UploadAssetType.fromRequestValue("chat_image"));

    ResponseStatusException unsupported =
        assertThrows(
            ResponseStatusException.class,
            () -> ObjectUploadService.UploadAssetType.fromRequestValue("video"));
    assertEquals("Unsupported assetType.", unsupported.getReason());
  }

  private ObjectStorageProperties baseProperties() {
    ObjectStorageProperties properties = new ObjectStorageProperties();
    properties.setEnabled(true);
    properties.setBucket("carry-bucket");
    properties.setEndpoint("https://nyc3.digitaloceanspaces.com");
    properties.setKeyPrefix("/media/");
    properties.setPresignExpirySeconds(120);
    properties.setMaxUploadBytes(4096L);
    return properties;
  }

  private ObjectProvider<S3Presigner> providerWith(S3Presigner presigner) {
    return new ObjectProvider<>() {
      @Override
      public S3Presigner getObject(Object... args) {
        return presigner;
      }

      @Override
      public S3Presigner getIfAvailable() {
        return presigner;
      }

      @Override
      public S3Presigner getIfUnique() {
        return presigner;
      }

      @Override
      public S3Presigner getObject() {
        return presigner;
      }
    };
  }
}
