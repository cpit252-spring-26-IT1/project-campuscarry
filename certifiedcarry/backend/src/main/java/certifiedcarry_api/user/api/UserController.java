package certifiedcarry_api.user.api;

import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import certifiedcarry_api.shared.ActorRequestResolver;
import certifiedcarry_api.shared.AuthGuards;
import certifiedcarry_api.shared.RequestAttributeParser;
import certifiedcarry_api.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/users")
public class UserController {

  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @GetMapping
  public List<UserResponse> getUsers(
      @RequestParam(required = false) String id,
      @RequestParam(required = false) UserRole role,
      @RequestParam(required = false) UserStatus status,
      Authentication authentication,
      HttpServletRequest request) {
    boolean isAdmin = isAdmin(authentication);
    Long actorUserId =
        isAdmin ? null : ActorRequestResolver.requireActorUserId(authentication, request);
    return userService.getUsersForActor(id, role, status, actorUserId, isAdmin);
  }

  @PostMapping
  public ResponseEntity<UserResponse> createUser(
      @Valid @RequestBody CreateUserRequest request, HttpServletRequest servletRequest) {
    UserResponse created = userService.createUser(
        request,
        RequestAttributeParser.attributeAsString(servletRequest.getAttribute("firebaseUid")),
        RequestAttributeParser.attributeAsString(
            servletRequest.getAttribute("firebaseEmail")),
        RequestAttributeParser.attributeAsBoolean(
            servletRequest.getAttribute("firebaseEmailVerified")));
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PatchMapping("/{userId}")
  public UserResponse patchUser(
      @PathVariable String userId, @RequestBody UpdateUserRequest request) {
    return userService.patchUser(userId, request);
  }

  @GetMapping("/me/dm-openness")
  public RecruiterDmOpennessResponse getCurrentRecruiterDmOpenness(
      HttpServletRequest request, Authentication authentication) {
    long actorUserId = ActorRequestResolver.requireActorUserId(authentication, request);
    return new RecruiterDmOpennessResponse(userService.getRecruiterDmOpenness(actorUserId));
  }

  @PatchMapping("/me/dm-openness")
  public RecruiterDmOpennessResponse updateCurrentRecruiterDmOpenness(
      HttpServletRequest request,
      Authentication authentication,
      @Valid @RequestBody UpdateRecruiterDmOpennessRequest updateRequest) {
    long actorUserId = ActorRequestResolver.requireActorUserId(authentication, request);
    return new RecruiterDmOpennessResponse(
        userService.updateRecruiterDmOpenness(actorUserId, updateRequest.recruiterDmOpenness()));
  }

  @DeleteMapping("/{userId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteUser(@PathVariable String userId) {
    userService.deleteUser(userId);
  }

  @DeleteMapping("/me")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteCurrentUser(HttpServletRequest request, Authentication authentication) {
    if (isAdmin(authentication)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin role is required.");
    }

    long actorUserId = ActorRequestResolver.requireActorUserId(authentication, request);
    userService.deleteUser(String.valueOf(actorUserId));
  }

  private boolean isAdmin(Authentication authentication) {
    return AuthGuards.hasAdminRole(authentication);
  }
}
