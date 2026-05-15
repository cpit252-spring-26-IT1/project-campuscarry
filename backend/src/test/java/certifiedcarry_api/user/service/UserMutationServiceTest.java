package certifiedcarry_api.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import certifiedcarry_api.notification.service.NotificationOrchestratorService;
import certifiedcarry_api.user.api.UpdateUserRequest;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import certifiedcarry_api.user.repo.UserRepository;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserMutationServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private FirebaseAccountDeletionQueueService firebaseAccountDeletionQueueService;

  @Mock
  private NotificationOrchestratorService notificationOrchestratorService;

  @InjectMocks
  private UserMutationService userMutationService;

  @Test
  void patchesPlayerFieldsAndNotifiesOnApprovalTransition() {
    UserEntity player = player(13L);
    when(userRepository.findById(13L)).thenReturn(Optional.of(player));
    when(userRepository.existsByUsernameIgnoreCaseAndIdNot("newtag", 13L)).thenReturn(false);
    when(userRepository.existsByEmailIgnoreCaseAndIdNot("new@example.com", 13L)).thenReturn(false);
    when(userRepository.existsByPersonalEmailIgnoreCaseAndIdNot("new@example.com", 13L))
        .thenReturn(false);
    when(passwordEncoder.encode("secret")).thenReturn("hashed");
    when(userRepository.save(any(UserEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    userMutationService.patchUser(
        "13",
        new UpdateUserRequest(
            "New Name",
            "newtag",
            "new@example.com",
            null,
            null,
            null,
            "secret",
            UserStatus.APPROVED,
            "declined",
            OffsetDateTime.parse("2026-05-13T00:00:00Z"),
            null));

    assertEquals("New Name", player.getFullName());
    assertEquals("newtag", player.getUsername());
    assertEquals("new@example.com", player.getPersonalEmail());
    assertEquals("hashed", player.getPasswordHash());
    assertEquals(UserStatus.APPROVED, player.getStatus());
    verify(notificationOrchestratorService).registerAccountApprovedAfterCommit(player, UserStatus.PENDING);
  }

  @Test
  void rejectsRoleSpecificFieldUpdatesAndDuplicateConflicts() {
    UserEntity recruiter = recruiter(20L);
    when(userRepository.findById(20L)).thenReturn(Optional.of(recruiter));

    ResponseStatusException playerFieldFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                userMutationService.patchUser(
                    "20",
                    new UpdateUserRequest(
                        null, "not-allowed", null, null, null, null, null, null, null, null, null)));
    assertEquals("username can only be updated for PLAYER users.", playerFieldFailure.getReason());

    UserEntity player = player(13L);
    when(userRepository.findById(13L)).thenReturn(Optional.of(player));
    when(userRepository.existsByUsernameIgnoreCaseAndIdNot("duplicate", 13L)).thenReturn(true);
    ResponseStatusException duplicateFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                userMutationService.patchUser(
                    "13",
                    new UpdateUserRequest(
                        null, "duplicate", null, null, null, null, null, null, null, null, null)));
    assertEquals("This username is already taken.", duplicateFailure.getReason());
  }

  @Test
  void getsAndUpdatesRecruiterDmOpenness() {
    UserEntity recruiter = recruiter(20L);
    when(userRepository.findById(20L)).thenReturn(Optional.of(recruiter));
    when(userRepository.save(recruiter)).thenReturn(recruiter);

    assertEquals(RecruiterDmOpenness.CLOSED, userMutationService.getRecruiterDmOpenness(20L));
    assertEquals(
        RecruiterDmOpenness.OPEN_VERIFIED_PLAYERS,
        userMutationService.updateRecruiterDmOpenness(20L, RecruiterDmOpenness.OPEN_VERIFIED_PLAYERS));
    assertEquals(RecruiterDmOpenness.OPEN_VERIFIED_PLAYERS, recruiter.getRecruiterDmOpenness());
  }

  @Test
  void rejectsAdminDeletionAndPatchConflictFailures() {
    UserEntity admin = recruiter(1L);
    admin.setRole(UserRole.ADMIN);
    when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

    ResponseStatusException adminDeleteFailure =
        assertThrows(ResponseStatusException.class, () -> userMutationService.deleteUser("1"));
    assertEquals("Admin accounts cannot be deleted from this endpoint.", adminDeleteFailure.getReason());

    UserEntity player = player(13L);
    when(userRepository.findById(13L)).thenReturn(Optional.of(player));
    when(userRepository.save(any(UserEntity.class))).thenThrow(new DataIntegrityViolationException("boom"));
    ResponseStatusException conflictFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                userMutationService.patchUser(
                    "13",
                    new UpdateUserRequest(
                        "Name", null, null, null, null, null, null, null, null, null, null)));
    assertEquals("Patch violates a unique or integrity constraint.", conflictFailure.getReason());
  }

  @Test
  void deleteUserPurgesKnownTablesAndTriggersFirebaseCleanupImmediatelyInUnitTests() {
    UserEntity recruiter = recruiter(20L);
    recruiter.setFirebaseUid("firebase-uid");
    when(userRepository.findById(20L)).thenReturn(Optional.of(recruiter));
    when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyString()))
        .thenReturn("chat_messages")
        .thenReturn("chat_threads")
        .thenReturn(null)
        .thenReturn("pending_ranks")
        .thenReturn(null)
        .thenReturn("player_profiles")
        .thenReturn(null);

    userMutationService.deleteUser("20");

    verify(jdbcTemplate)
        .update(anyString(), eq(20L), eq(20L));
    verify(jdbcTemplate)
        .update(anyString(), eq(20L), eq(20L), eq(20L), eq(20L));
    verify(jdbcTemplate, times(2)).update(anyString(), eq(20L));
    verify(userRepository).delete(recruiter);
    verify(userRepository).flush();
    verify(firebaseAccountDeletionQueueService)
        .deleteOrQueue(20L, "firebase-uid", "recruiter@example.com");
  }

  @Test
  void deleteUserMapsDataIntegrityAndRecruiterDmOpennessValidatesRoleAndRequiredValue() {
    UserEntity player = player(13L);
    when(userRepository.findById(13L)).thenReturn(Optional.of(player));
    when(jdbcTemplate.queryForObject(anyString(), eq(String.class), anyString()))
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null);
    org.mockito.Mockito.doThrow(new DataIntegrityViolationException("boom")).when(userRepository).flush();

    ResponseStatusException deleteConflict =
        assertThrows(ResponseStatusException.class, () -> userMutationService.deleteUser("13"));
    assertEquals("Unable to delete user due to related records.", deleteConflict.getReason());
    verify(firebaseAccountDeletionQueueService, never()).deleteOrQueue(anyLong(), anyString(), anyString());

    ResponseStatusException missingOpenness =
        assertThrows(
            ResponseStatusException.class,
            () -> userMutationService.updateRecruiterDmOpenness(20L, null));
    assertEquals("recruiterDmOpenness is required.", missingOpenness.getReason());

    when(userRepository.findById(13L)).thenReturn(Optional.of(player));
    ResponseStatusException recruiterRoleFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> userMutationService.getRecruiterDmOpenness(13L));
    assertEquals("Recruiter role is required.", recruiterRoleFailure.getReason());
  }

  private UserEntity player(Long id) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setFullName("Player");
    user.setUsername("player" + id);
    user.setPersonalEmail("player@example.com");
    user.setRole(UserRole.PLAYER);
    user.setStatus(UserStatus.PENDING);
    user.setPasswordHash("old");
    return user;
  }

  private UserEntity recruiter(Long id) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setFullName("Recruiter");
    user.setEmail("recruiter@example.com");
    user.setRole(UserRole.RECRUITER);
    user.setStatus(UserStatus.PENDING);
    user.setRecruiterDmOpenness(RecruiterDmOpenness.CLOSED);
    return user;
  }
}
