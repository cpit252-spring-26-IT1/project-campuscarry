package certifiedcarry_api.auth.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.notification.service.NotificationOrchestratorService;
import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.model.ConsentSource;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import certifiedcarry_api.user.service.UserService;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

  @Mock
  private NotificationOrchestratorService notificationOrchestratorService;

  @Mock
  private UserService userService;

  @Test
  void meReturnsPayloadAndRecordsActivityWhenBackendUserExists() {
    AuthController controller = new AuthController(notificationOrchestratorService, userService);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken("firebase-uid", null, java.util.List.of());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("firebaseUid", "firebase-uid");
    request.setAttribute("firebaseEmail", "player@example.com");
    request.setAttribute("backendUserId", "7");
    request.setAttribute("backendUserRole", "PLAYER");

    Map<String, Object> payload = controller.me(authentication, request);

    assertEquals(true, payload.get("authenticated"));
    assertEquals("firebase-uid", payload.get("firebaseUid"));
    assertEquals("player@example.com", payload.get("firebaseEmail"));
    assertEquals("7", payload.get("backendUserId"));
    assertEquals("PLAYER", payload.get("backendUserRole"));
    assertEquals("firebase-uid", payload.get("principal"));
    verify(notificationOrchestratorService).recordUserActivity(7L);
  }

  @Test
  void loginLogoutAndSignupDelegateToServices() {
    AuthController controller = new AuthController(notificationOrchestratorService, userService);
    Authentication authentication =
        new UsernamePasswordAuthenticationToken("firebase-uid", null, java.util.List.of());
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("backendUserId", "7");
    request.setAttribute("firebaseUid", "firebase-uid");
    request.setAttribute("firebaseEmail", "player@example.com");
    request.setAttribute("firebaseEmailVerified", true);

    assertEquals("LOGGED_IN", controller.markLogin(authentication, request).get("state"));
    assertEquals("LOGGED_OUT", controller.markLogout(authentication, request).get("state"));
    verify(notificationOrchestratorService).markUserLoggedIn(7L);
    verify(notificationOrchestratorService).markUserLoggedOut(7L);

    CreateUserRequest signupRequest =
        new CreateUserRequest(
            null,
            "Bluy",
            "bluy",
            "player@example.com",
            null,
            null,
            null,
            null,
            "secret",
            UserRole.PLAYER,
            true,
            OffsetDateTime.parse("2026-05-13T00:00:00Z"),
            "en",
            ConsentSource.REGISTER_PAGE,
            "terms-v1",
            "privacy-v1");
    UserResponse response =
        new UserResponse(
            "7",
            "Bluy",
            "bluy",
            "player@example.com",
            null,
            null,
            UserRole.PLAYER,
            UserStatus.APPROVED,
            null,
            null,
            null,
            OffsetDateTime.parse("2026-05-13T00:00:00Z"),
            "en",
            "terms-v1",
            "privacy-v1");

    when(userService.createUser(signupRequest, "firebase-uid", "player@example.com", true))
        .thenReturn(response);

    assertEquals(response, controller.completeSignup(signupRequest, request));
    verify(userService).createUser(signupRequest, "firebase-uid", "player@example.com", true);
  }
}
