package certifiedcarry_api.auth.api;

import certifiedcarry_api.notification.service.NotificationOrchestratorService;
import certifiedcarry_api.shared.ActorRequestResolver;
import certifiedcarry_api.shared.RequestAttributeParser;
import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

  private final NotificationOrchestratorService notificationOrchestratorService;
  private final UserService userService;

  public AuthController(
      NotificationOrchestratorService notificationOrchestratorService, UserService userService) {
    this.notificationOrchestratorService = notificationOrchestratorService;
    this.userService = userService;
  }

  @GetMapping("/me")
  public Map<String, Object> me(Authentication authentication, HttpServletRequest request) {
    String firebaseUid = RequestAttributeParser.attributeAsString(request.getAttribute("firebaseUid"));
    String firebaseEmail =
        RequestAttributeParser.attributeAsString(request.getAttribute("firebaseEmail"));
    String backendUserId =
        RequestAttributeParser.attributeAsString(request.getAttribute("backendUserId"));
    String backendUserRole =
        RequestAttributeParser.attributeAsString(request.getAttribute("backendUserRole"));

    boolean isAuthenticated =
        authentication != null && authentication.isAuthenticated() && firebaseUid != null;

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("authenticated", isAuthenticated);
    payload.put("firebaseUid", firebaseUid);
    payload.put("firebaseEmail", firebaseEmail);
    payload.put("backendUserId", backendUserId);
    payload.put("backendUserRole", backendUserRole);
    payload.put("principal", authentication != null ? authentication.getName() : null);

    Long parsedBackendUserId = ActorRequestResolver.optionalBackendUserId(request);
    if (parsedBackendUserId != null) {
      notificationOrchestratorService.recordUserActivity(parsedBackendUserId);
    }

    return payload;
  }

  @PostMapping("/session/login")
  public Map<String, Object> markLogin(Authentication authentication, HttpServletRequest request) {
    long backendUserId = ActorRequestResolver.requireActorUserId(authentication, request);
    notificationOrchestratorService.markUserLoggedIn(backendUserId);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("ok", true);
    payload.put("backendUserId", String.valueOf(backendUserId));
    payload.put("state", "LOGGED_IN");
    return payload;
  }

  @PostMapping("/session/logout")
  public Map<String, Object> markLogout(Authentication authentication, HttpServletRequest request) {
    long backendUserId = ActorRequestResolver.requireActorUserId(authentication, request);
    notificationOrchestratorService.markUserLoggedOut(backendUserId);

    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("ok", true);
    payload.put("backendUserId", String.valueOf(backendUserId));
    payload.put("state", "LOGGED_OUT");
    return payload;
  }

  @PostMapping("/signup/complete")
  public UserResponse completeSignup(
      @Valid @RequestBody CreateUserRequest request, HttpServletRequest servletRequest) {
    return userService.createUser(
        request,
        RequestAttributeParser.attributeAsString(servletRequest.getAttribute("firebaseUid")),
        RequestAttributeParser.attributeAsString(servletRequest.getAttribute("firebaseEmail")),
        RequestAttributeParser.attributeAsBoolean(
            servletRequest.getAttribute("firebaseEmailVerified")));
  }
}
