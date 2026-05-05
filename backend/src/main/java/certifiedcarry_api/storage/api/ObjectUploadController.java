package certifiedcarry_api.storage.api;

import certifiedcarry_api.chat.service.ChatService;
import certifiedcarry_api.shared.ActorRequestContext;
import certifiedcarry_api.shared.ActorRequestResolver;
import certifiedcarry_api.storage.service.ObjectUploadService;
import certifiedcarry_api.storage.service.ObjectUploadService.PresignedUpload;
import certifiedcarry_api.storage.service.ObjectUploadService.UploadAssetType;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/media/uploads")
public class ObjectUploadController {

  private final ObjectUploadService objectUploadService;
  private final ChatService chatService;

  public ObjectUploadController(ObjectUploadService objectUploadService, ChatService chatService) {
    this.objectUploadService = objectUploadService;
    this.chatService = chatService;
  }

  @PostMapping("/presign")
  public ResponseEntity<PresignedUploadResponse> createPresignedUpload(
      @RequestBody PresignedUploadRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActorWithRole(authentication, servletRequest);

    UploadAssetType assetType = UploadAssetType.fromRequestValue(request.assetType());
    String backendUserRole = actor.requireBackendUserRole();

    if (assetType == UploadAssetType.CHAT_IMAGE) {
      long threadId = requirePositiveLong(request.threadId(), "threadId");

      if (!chatService.isThreadParticipant(threadId, actor.userId())) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "You can only upload chat images for conversations you participate in.");
      }
    } else if (!"PLAYER".equals(backendUserRole)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Only player accounts can upload profile images.");
    }

    PresignedUpload presignedUpload = objectUploadService.createPresignedUpload(
        actor.userId(),
        assetType,
        request.fileName(),
        request.contentType(),
        request.fileSizeBytes());

    return ResponseEntity.ok(
        new PresignedUploadResponse(
            presignedUpload.uploadUrl(),
            presignedUpload.publicUrl(),
            presignedUpload.objectKey(),
            presignedUpload.requiredHeaders(),
            presignedUpload.expiresAt().toString()));
  }

  private long requirePositiveLong(Long value, String fieldName) {
    if (value == null || value <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " must be positive.");
    }

    return value;
  }

  public record PresignedUploadRequest(
      String assetType, String fileName, String contentType, Long fileSizeBytes, Long threadId) {
  }

  public record PresignedUploadResponse(
      String uploadUrl,
      String publicUrl,
      String objectKey,
      Map<String, String> requiredHeaders,
      String expiresAt) {
  }
}
