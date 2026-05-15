package certifiedcarry_api.user.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.user.model.ConsentSource;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import certifiedcarry_api.user.service.UserService;
import java.time.OffsetDateTime;
import java.util.List;
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
class UserControllerTest {

  @Mock private UserService userService;

  @Test
  void getUsersUsesActorScopeForNonAdminAndNullActorForAdmin() {
    UserController controller = new UserController(userService);
    UserResponse response = userResponse();
    MockHttpServletRequest playerRequest = actorRequest();
    when(userService.getUsersForActor("9", UserRole.PLAYER, UserStatus.APPROVED, 9L, false))
        .thenReturn(List.of(response));
    when(userService.getUsersForActor(null, null, null, null, true)).thenReturn(List.of(response));

    List<UserResponse> nonAdmin =
        controller.getUsers("9", UserRole.PLAYER, UserStatus.APPROVED, playerAuth(), playerRequest);
    List<UserResponse> admin =
        controller.getUsers(null, null, null, adminAuth(), new MockHttpServletRequest());

    assertEquals(1, nonAdmin.size());
    assertEquals(1, admin.size());
    verify(userService).getUsersForActor("9", UserRole.PLAYER, UserStatus.APPROVED, 9L, false);
    verify(userService).getUsersForActor(null, null, null, null, true);
  }

  @Test
  void createUserAndRecruiterDmEndpointsDelegateUsingResolvedActor() {
    UserController controller = new UserController(userService);
    MockHttpServletRequest request = actorRequest();
    request.setAttribute("firebaseUid", "firebase-uid");
    request.setAttribute("firebaseEmail", "player@example.com");
    request.setAttribute("firebaseEmailVerified", true);
    CreateUserRequest createRequest =
        new CreateUserRequest(
            null,
            "Player Name",
            "player",
            "player.personal@example.com",
            "player@example.com",
            null,
            null,
            null,
            "secret",
            UserRole.PLAYER,
            true,
            OffsetDateTime.parse("2026-05-13T00:00:00Z"),
            "en",
            ConsentSource.REGISTER_PAGE,
            "v1",
            "v1");
    UserResponse createdUser = userResponse();
    when(userService.createUser(createRequest, "firebase-uid", "player@example.com", true))
        .thenReturn(createdUser);
    when(userService.getRecruiterDmOpenness(9L)).thenReturn(RecruiterDmOpenness.OPEN_ALL_PLAYERS);
    when(userService.updateRecruiterDmOpenness(9L, RecruiterDmOpenness.OPEN_VERIFIED_PLAYERS))
        .thenReturn(RecruiterDmOpenness.OPEN_VERIFIED_PLAYERS);

    var created = controller.createUser(createRequest, request);
    var openness = controller.getCurrentRecruiterDmOpenness(request, playerAuth());
    var updated =
        controller.updateCurrentRecruiterDmOpenness(
            request,
            playerAuth(),
            new UpdateRecruiterDmOpennessRequest(RecruiterDmOpenness.OPEN_VERIFIED_PLAYERS));

    assertEquals(HttpStatus.CREATED, created.getStatusCode());
    assertEquals("9", created.getBody().id());
    assertEquals(RecruiterDmOpenness.OPEN_ALL_PLAYERS, openness.recruiterDmOpenness());
    assertEquals(RecruiterDmOpenness.OPEN_VERIFIED_PLAYERS, updated.recruiterDmOpenness());
  }

  @Test
  void deleteCurrentUserRejectsAdminAndDeletesResolvedActorForPlayer() {
    UserController controller = new UserController(userService);

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> controller.deleteCurrentUser(new MockHttpServletRequest(), adminAuth()));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    controller.deleteCurrentUser(actorRequest(), playerAuth());
    verify(userService).deleteUser("9");
  }

  private UserResponse userResponse() {
    return new UserResponse(
        "9",
        "Player Name",
        "player",
        "player.personal@example.com",
        "player@example.com",
        null,
        UserRole.PLAYER,
        UserStatus.APPROVED,
        null,
        null,
        RecruiterDmOpenness.OPEN_ALL_PLAYERS,
        OffsetDateTime.parse("2026-05-13T00:00:00Z"),
        "en",
        "v1",
        "v1");
  }

  private MockHttpServletRequest actorRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("backendUserId", "9");
    request.setAttribute("backendUserRole", "PLAYER");
    return request;
  }

  private UsernamePasswordAuthenticationToken playerAuth() {
    return new UsernamePasswordAuthenticationToken(
        "player", "n/a", List.of(new SimpleGrantedAuthority("ROLE_PLAYER")));
  }

  private UsernamePasswordAuthenticationToken adminAuth() {
    return new UsernamePasswordAuthenticationToken(
        "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }
}
