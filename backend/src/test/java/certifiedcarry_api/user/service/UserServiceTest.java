package certifiedcarry_api.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.api.UpdateUserRequest;
import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import certifiedcarry_api.user.repo.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private UserRegistrationService userRegistrationService;

  @Mock
  private UserMutationService userMutationService;

  @InjectMocks
  private UserService userService;

  @Test
  void getUsersFiltersByIdRoleAndStatus() {
    UserEntity recruiter = entity(9L, UserRole.RECRUITER, UserStatus.PENDING);
    when(userRepository.findById(9L)).thenReturn(Optional.of(recruiter));

    List<UserResponse> responses = userService.getUsers(" 9 ", UserRole.RECRUITER, UserStatus.PENDING);

    assertEquals(1, responses.size());
    assertEquals("9", responses.get(0).id());
  }

  @Test
  void getUsersForActorRequiresAdminForRestrictedQueriesAndSanitizesForNonAdmin() {
    ResponseStatusException forbidden =
        assertThrows(
            ResponseStatusException.class,
            () -> userService.getUsersForActor(null, UserRole.RECRUITER, null, 13L, false));
    assertEquals("Admin role is required.", forbidden.getReason());

    UserEntity player = entity(13L, UserRole.PLAYER, UserStatus.APPROVED);
    UserEntity otherRecruiter = entity(14L, UserRole.RECRUITER, UserStatus.APPROVED);
    when(userRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
        .thenReturn(List.of(player, otherRecruiter));

    List<UserResponse> sanitized = userService.getUsersForActor(null, null, null, 13L, false);

    assertEquals("13", sanitized.get(0).id());
    assertEquals("player13@example.com", sanitized.get(0).personalEmail());
    assertEquals("14", sanitized.get(1).id());
    assertEquals(null, sanitized.get(1).personalEmail());
    assertEquals(RecruiterDmOpenness.OPEN_ALL_PLAYERS, sanitized.get(1).recruiterDmOpenness());
  }

  @Test
  void getUsersForActorRequiresBackendUserWhenNonAdmin() {
    when(userRepository.findAll(Sort.by(Sort.Direction.ASC, "id")))
        .thenReturn(List.of(entity(14L, UserRole.PLAYER, UserStatus.APPROVED)));

    ResponseStatusException forbidden =
        assertThrows(
            ResponseStatusException.class,
            () -> userService.getUsersForActor(null, null, null, null, false));

    assertEquals("Authenticated user is not linked to backend user data.", forbidden.getReason());
  }

  @Test
  void delegatesMutatingOperations() {
    CreateUserRequest createRequest = null;
    UpdateUserRequest updateRequest = new UpdateUserRequest("name", null, null, null, null, null, null, null, null, null, null);
    UserResponse created = response("1", UserRole.PLAYER);
    UserResponse updated = response("2", UserRole.RECRUITER);
    when(userRegistrationService.createUser(createRequest, "uid", "mail", true)).thenReturn(created);
    when(userMutationService.patchUser("2", updateRequest)).thenReturn(updated);
    when(userMutationService.getRecruiterDmOpenness(99L)).thenReturn(RecruiterDmOpenness.CLOSED);
    when(userMutationService.updateRecruiterDmOpenness(99L, RecruiterDmOpenness.OPEN_ALL_PLAYERS))
        .thenReturn(RecruiterDmOpenness.OPEN_ALL_PLAYERS);

    assertSame(created, userService.createUser(createRequest, "uid", "mail", true));
    assertSame(updated, userService.patchUser("2", updateRequest));
    assertEquals(RecruiterDmOpenness.CLOSED, userService.getRecruiterDmOpenness(99L));
    assertEquals(
        RecruiterDmOpenness.OPEN_ALL_PLAYERS,
        userService.updateRecruiterDmOpenness(99L, RecruiterDmOpenness.OPEN_ALL_PLAYERS));
    userService.deleteUser("2");

    verify(userMutationService).deleteUser("2");
  }

  private UserEntity entity(Long id, UserRole role, UserStatus status) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setFullName("User " + id);
    user.setUsername(role == UserRole.PLAYER ? "player" + id : null);
    user.setPersonalEmail("player" + id + "@example.com");
    user.setEmail("recruiter" + id + "@example.com");
    user.setOrganizationName("Org");
    user.setRole(role);
    user.setStatus(status);
    user.setRecruiterDmOpenness(RecruiterDmOpenness.OPEN_ALL_PLAYERS);
    user.setUpdatedAt(OffsetDateTime.parse("2026-05-13T00:00:00Z"));
    user.setLegalConsentLocale("en");
    user.setTermsVersionAccepted("terms-v1");
    user.setPrivacyVersionAccepted("privacy-v1");
    return user;
  }

  private UserResponse response(String id, UserRole role) {
    return new UserResponse(
        id,
        "User",
        null,
        null,
        null,
        null,
        role,
        UserStatus.APPROVED,
        null,
        null,
        null,
        OffsetDateTime.parse("2026-05-13T00:00:00Z"),
        null,
        null,
        null);
  }
}
