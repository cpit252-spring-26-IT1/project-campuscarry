package certifiedcarry_api.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.shared.LegalVersionValidator;
import certifiedcarry_api.shared.TextNormalization;
import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.factory.PlayerUserCreationFactory;
import certifiedcarry_api.user.factory.RecruiterUserCreationFactory;
import certifiedcarry_api.user.factory.UserFactorySelector;
import certifiedcarry_api.user.model.ConsentSource;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import certifiedcarry_api.user.repo.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserRegistrationServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  private UserRegistrationService firebaseEnabledService;
  private UserRegistrationService firebaseDisabledService;

  @BeforeEach
  void setUp() {
    UserFactorySelector selector =
        new UserFactorySelector(
            List.of(new PlayerUserCreationFactory(), new RecruiterUserCreationFactory()));
    LegalVersionValidator validator = new LegalVersionValidator("terms-v1", "privacy-v1");
    firebaseEnabledService =
        new UserRegistrationService(userRepository, selector, passwordEncoder, validator, true);
    firebaseDisabledService =
        new UserRegistrationService(userRepository, selector, passwordEncoder, validator, false);
  }

  @Test
  void rejectsAdminAndMissingLegalConsentBeforeDoingWork() {
    ResponseStatusException adminFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> firebaseDisabledService.createUser(adminRequest(), null, null, null));
    assertEquals("Admin accounts cannot be created from registration.", adminFailure.getReason());

    ResponseStatusException consentFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                firebaseDisabledService.createUser(
                    playerRequest(false, "player@example.com", "bluy"), null, null, null));
    assertEquals("You must accept legal terms before registration.", consentFailure.getReason());

    verify(userRepository, never()).save(any());
  }

  @Test
  void enforcesFirebasePresenceAndEmailMatchingWhenEnabled() {
    ResponseStatusException missingFirebaseFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                firebaseEnabledService.createUser(
                    playerRequest(true, "player@example.com", "bluy"), null, null, null));
    assertEquals(
        "Firebase authentication is required for registration.", missingFirebaseFailure.getReason());

    ResponseStatusException mismatchFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                firebaseEnabledService.createUser(
                    playerRequest(true, "player@example.com", "bluy"),
                    "firebase-uid",
                    "other@example.com",
                    true));
    assertEquals(
        "Firebase token email does not match the submitted registration email.",
        mismatchFailure.getReason());
  }

  @Test
  void enforcesFirebaseVerificationAndTokenEmailPresenceWhenUidIsProvided() {
    ResponseStatusException unverifiedFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                firebaseEnabledService.createUser(
                    playerRequest(true, "player@example.com", "bluy"),
                    "firebase-uid",
                    "player@example.com",
                    false));
    assertEquals(
        "Firebase account email must be verified before registration can be linked.",
        unverifiedFailure.getReason());

    ResponseStatusException missingEmailFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                firebaseEnabledService.createUser(
                    playerRequest(true, "player@example.com", "bluy"),
                    "firebase-uid",
                    "   ",
                    true));
    assertEquals(
        "Firebase token email is required for registration linkage.",
        missingEmailFailure.getReason());
  }

  @Test
  void returnsExistingUserWhenFirebaseUidAlreadyLinked() {
    UserEntity existing = playerEntity(14L, "bluy", "player@example.com");
    existing.setFirebaseUid("firebase-uid");
    when(userRepository.findByFirebaseUid("firebase-uid")).thenReturn(Optional.of(existing));

    UserResponse response =
        firebaseEnabledService.createUser(
            playerRequest(true, "player@example.com", "bluy"),
            "firebase-uid",
            "player@example.com",
            true);

    assertEquals("14", response.id());
    verify(userRepository, never()).save(any());
  }

  @Test
  void relinksExistingPlayerWhenFirebaseRegistrationMatchesEmailAndUsername() {
    UserEntity existing = playerEntity(14L, "bluy", "player@example.com");
    when(userRepository.findByFirebaseUid("firebase-uid")).thenReturn(Optional.empty());
    when(userRepository.findByPersonalEmailIgnoreCase("player@example.com"))
        .thenReturn(Optional.of(existing));
    when(userRepository.save(existing))
        .thenAnswer(
            invocation -> {
              UserEntity saved = invocation.getArgument(0);
              saved.setUpdatedAt(OffsetDateTime.parse("2026-05-13T00:00:00Z"));
              return saved;
            });

    UserResponse response =
        firebaseEnabledService.createUser(
            playerRequest(true, "player@example.com", "bluy"),
            "firebase-uid",
            "player@example.com",
            true);

    assertEquals("14", response.id());
    assertEquals("bluy", response.username());
    assertEquals("firebase-uid", existing.getFirebaseUid());
    verify(userRepository).save(existing);
  }

  @Test
  void rejectsRelinkingWhenExistingEmailBelongsToDifferentRoleOrUsername() {
    UserEntity recruiter = recruiterEntity(21L, "player@example.com");
    when(userRepository.findByFirebaseUid("firebase-uid")).thenReturn(Optional.empty());
    when(userRepository.findByPersonalEmailIgnoreCase("player@example.com"))
        .thenReturn(Optional.of(recruiter));

    ResponseStatusException roleFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                firebaseEnabledService.createUser(
                    playerRequest(true, "player@example.com", "bluy"),
                    "firebase-uid",
                    "player@example.com",
                    true));
    assertEquals("An account with this email already exists.", roleFailure.getReason());

    UserEntity player = playerEntity(14L, "other-name", "player@example.com");
    when(userRepository.findByPersonalEmailIgnoreCase("player@example.com"))
        .thenReturn(Optional.of(player));

    ResponseStatusException usernameFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                firebaseEnabledService.createUser(
                    playerRequest(true, "player@example.com", "bluy"),
                    "firebase-uid",
                    "player@example.com",
                    true));
    assertEquals("An account with this email already exists.", usernameFailure.getReason());
  }

  @Test
  void createsRecruiterWhenUniqueAndValid() {
    doReturn("hashed").when(passwordEncoder).encode("secret");
    when(userRepository.existsByEmailIgnoreCase("recruiter@example.com")).thenReturn(false);
    when(userRepository.existsByPersonalEmailIgnoreCase("recruiter@example.com")).thenReturn(false);
    when(userRepository.findByFirebaseUid("firebase-uid")).thenReturn(Optional.empty());
    when(userRepository.findByEmailIgnoreCase("recruiter@example.com")).thenReturn(Optional.empty());
    when(userRepository.save(any(UserEntity.class)))
        .thenAnswer(
            invocation -> {
              UserEntity saved = invocation.getArgument(0);
              saved.setId(22L);
              return saved;
            });

    UserResponse response =
        firebaseEnabledService.createUser(
            recruiterRequest(" Recruiter@Example.com "),
            "firebase-uid",
            "recruiter@example.com",
            true);

    assertEquals("22", response.id());
    assertEquals(UserRole.RECRUITER, response.role());
    assertEquals(UserStatus.PENDING, response.status());
    verify(userRepository).save(any(UserEntity.class));
  }

  @Test
  void allowsRegistrationWithoutFirebaseWhenFeatureIsDisabled() {
    doReturn("hashed").when(passwordEncoder).encode("secret");
    when(userRepository.existsByUsernameIgnoreCase("bluy")).thenReturn(false);
    when(userRepository.existsByEmailIgnoreCase("player@example.com")).thenReturn(false);
    when(userRepository.existsByPersonalEmailIgnoreCase("player@example.com")).thenReturn(false);
    when(userRepository.save(any(UserEntity.class)))
        .thenAnswer(
            invocation -> {
              UserEntity saved = invocation.getArgument(0);
              saved.setId(18L);
              return saved;
            });

    UserResponse response =
        firebaseDisabledService.createUser(
            playerRequest(true, "player@example.com", "bluy"), null, null, null);

    assertEquals("18", response.id());
  }

  @Test
  void surfacesValidationAndUniquePersistenceFailuresFromFactoriesAndRepository() {
    when(passwordEncoder.encode("secret")).thenThrow(new IllegalArgumentException("bad password"));
    when(userRepository.existsByUsernameIgnoreCase("bluy")).thenReturn(false);
    when(userRepository.existsByEmailIgnoreCase("player@example.com")).thenReturn(false);
    when(userRepository.existsByPersonalEmailIgnoreCase("player@example.com")).thenReturn(false);

    ResponseStatusException validationFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                firebaseDisabledService.createUser(
                    playerRequest(true, "player@example.com", "bluy"), null, null, null));
    assertEquals("bad password", validationFailure.getReason());

    doReturn("hashed").when(passwordEncoder).encode("secret");
    when(userRepository.save(any(UserEntity.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate"));

    ResponseStatusException conflictFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                firebaseDisabledService.createUser(
                    playerRequest(true, "player@example.com", "bluy"), null, null, null));
    assertEquals(
        "User already exists with one of the provided unique fields.",
        conflictFailure.getReason());
  }

  private CreateUserRequest playerRequest(
      boolean legalConsentAccepted, String personalEmail, String username) {
    return new CreateUserRequest(
        null,
        "Bluy",
        username,
        personalEmail,
        null,
        null,
        null,
        null,
        "secret",
        UserRole.PLAYER,
        legalConsentAccepted,
        OffsetDateTime.parse("2026-05-13T00:00:00Z"),
        "en",
        ConsentSource.REGISTER_PAGE,
        "terms-v1",
        "privacy-v1");
  }

  private CreateUserRequest recruiterRequest(String email) {
    return new CreateUserRequest(
        null,
        "Scout",
        null,
        null,
        email,
        "Org",
        "https://linkedin.com/in/scout",
        RecruiterDmOpenness.OPEN_VERIFIED_PLAYERS,
        "secret",
        UserRole.RECRUITER,
        true,
        OffsetDateTime.parse("2026-05-13T00:00:00Z"),
        "en",
        ConsentSource.REGISTER_PAGE,
        "terms-v1",
        "privacy-v1");
  }

  private CreateUserRequest adminRequest() {
    return new CreateUserRequest(
        null,
        "Admin",
        null,
        null,
        "admin@example.com",
        "Org",
        "https://linkedin.com/in/admin",
        null,
        "secret",
        UserRole.ADMIN,
        true,
        OffsetDateTime.parse("2026-05-13T00:00:00Z"),
        "en",
        ConsentSource.REGISTER_PAGE,
        "terms-v1",
        "privacy-v1");
  }

  private UserEntity playerEntity(Long id, String username, String email) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setFullName("Bluy");
    user.setUsername(username);
    user.setPersonalEmail(email);
    user.setRole(UserRole.PLAYER);
    user.setStatus(UserStatus.APPROVED);
    user.setLegalConsentLocale("en");
    user.setTermsVersionAccepted("terms-v1");
    user.setPrivacyVersionAccepted("privacy-v1");
    return user;
  }

  private UserEntity recruiterEntity(Long id, String email) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setFullName("Scout");
    user.setEmail(TextNormalization.trimToNull(email));
    user.setRole(UserRole.RECRUITER);
    user.setStatus(UserStatus.PENDING);
    user.setOrganizationName("Org");
    user.setLinkedinUrl("https://linkedin.com/in/scout");
    user.setLegalConsentLocale("en");
    user.setTermsVersionAccepted("terms-v1");
    user.setPrivacyVersionAccepted("privacy-v1");
    return user;
  }
}
