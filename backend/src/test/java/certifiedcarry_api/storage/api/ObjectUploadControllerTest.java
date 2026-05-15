package certifiedcarry_api.storage.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import certifiedcarry_api.chat.service.ChatService;
import certifiedcarry_api.storage.service.ObjectUploadService;
import certifiedcarry_api.storage.service.ObjectUploadService.PresignedUpload;
import certifiedcarry_api.storage.service.ObjectUploadService.UploadAssetType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ObjectUploadControllerTest {

  @Mock private ObjectUploadService objectUploadService;
  @Mock private ChatService chatService;

  @Test
  void playerProfileUploadDelegatesWithoutChatLookup() {
    ObjectUploadController controller = new ObjectUploadController(objectUploadService, chatService);
    MockHttpServletRequest request = actorRequest("PLAYER");
    PresignedUpload upload =
        new PresignedUpload(
            "https://upload.example",
            "https://cdn.example/file.png",
            "object-key",
            Map.of("Content-Type", "image/png"),
            OffsetDateTime.parse("2026-05-13T00:00:00Z"));
    when(objectUploadService.createPresignedUpload(9L, UploadAssetType.PROFILE_IMAGE, "file.png", "image/png", 123L))
        .thenReturn(upload);

    var response =
        controller.createPresignedUpload(
            new ObjectUploadController.PresignedUploadRequest(
                "PROFILE_IMAGE", "file.png", "image/png", 123L, null),
            playerAuth(),
            request);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("https://cdn.example/file.png", response.getBody().publicUrl());
    verifyNoInteractions(chatService);
  }

  @Test
  void chatImageUploadRequiresPositiveThreadAndParticipation() {
    ObjectUploadController controller = new ObjectUploadController(objectUploadService, chatService);
    MockHttpServletRequest request = actorRequest("PLAYER");

    ResponseStatusException badThread =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.createPresignedUpload(
                    new ObjectUploadController.PresignedUploadRequest(
                        "CHAT_IMAGE", "chat.png", "image/png", 100L, 0L),
                    playerAuth(),
                    request));
    assertEquals(HttpStatus.BAD_REQUEST, badThread.getStatusCode());

    when(chatService.isThreadParticipant(14L, 9L)).thenReturn(false);
    ResponseStatusException forbidden =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.createPresignedUpload(
                    new ObjectUploadController.PresignedUploadRequest(
                        "CHAT_IMAGE", "chat.png", "image/png", 100L, 14L),
                    playerAuth(),
                    request));

    assertEquals(HttpStatus.FORBIDDEN, forbidden.getStatusCode());
    verify(chatService).isThreadParticipant(14L, 9L);
  }

  @Test
  void nonPlayerProfileUploadIsForbidden() {
    ObjectUploadController controller = new ObjectUploadController(objectUploadService, chatService);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () ->
                controller.createPresignedUpload(
                    new ObjectUploadController.PresignedUploadRequest(
                        "PROFILE_IMAGE", "file.png", "image/png", 123L, null),
                    recruiterAuth(),
                    actorRequest("RECRUITER")));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    verifyNoInteractions(objectUploadService);
  }

  private MockHttpServletRequest actorRequest(String role) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("backendUserId", "9");
    request.setAttribute("backendUserRole", role);
    return request;
  }

  private UsernamePasswordAuthenticationToken playerAuth() {
    return new UsernamePasswordAuthenticationToken(
        "player", "n/a", List.of(new SimpleGrantedAuthority("ROLE_PLAYER")));
  }

  private UsernamePasswordAuthenticationToken recruiterAuth() {
    return new UsernamePasswordAuthenticationToken(
        "recruiter", "n/a", List.of(new SimpleGrantedAuthority("ROLE_RECRUITER")));
  }
}
