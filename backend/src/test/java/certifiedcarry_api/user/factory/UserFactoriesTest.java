package certifiedcarry_api.user.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.model.ConsentSource;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserFactoriesTest {

  private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

  @Test
  void playerFactoryBuildsApprovedPlayerDefaults() {
    when(passwordEncoder.encode("secret")).thenReturn("hashed");
    PlayerUserCreationFactory factory = new PlayerUserCreationFactory();

    var user = factory.create(playerRequest(), passwordEncoder);

    assertEquals(UserRole.PLAYER, factory.supportedRole());
    assertEquals("Bluy", user.getFullName());
    assertEquals("bluy", user.getUsername());
    assertEquals("player@example.com", user.getPersonalEmail());
    assertEquals("hashed", user.getPasswordHash());
    assertEquals(UserStatus.APPROVED, user.getStatus());
    assertEquals(RecruiterDmOpenness.CLOSED, user.getRecruiterDmOpenness());
  }

  @Test
  void recruiterFactoryBuildsPendingRecruiterDefaults() {
    when(passwordEncoder.encode("secret")).thenReturn("hashed");
    RecruiterUserCreationFactory factory = new RecruiterUserCreationFactory();

    var user = factory.create(recruiterRequest(null), passwordEncoder);

    assertEquals(UserRole.RECRUITER, factory.supportedRole());
    assertEquals("Org", user.getOrganizationName());
    assertEquals("https://linkedin.com/in/recruiter", user.getLinkedinUrl());
    assertEquals("recruiter@example.com", user.getEmail());
    assertEquals(UserStatus.PENDING, user.getStatus());
    assertEquals(RecruiterDmOpenness.CLOSED, user.getRecruiterDmOpenness());
    assertEquals("", user.getDeclineReason());
  }

  @Test
  void recruiterFactoryUsesProvidedDmSetting() {
    when(passwordEncoder.encode("secret")).thenReturn("hashed");
    RecruiterUserCreationFactory factory = new RecruiterUserCreationFactory();

    var user =
        factory.create(recruiterRequest(RecruiterDmOpenness.OPEN_ALL_PLAYERS), passwordEncoder);

    assertEquals(RecruiterDmOpenness.OPEN_ALL_PLAYERS, user.getRecruiterDmOpenness());
  }

  @Test
  void fieldUtilsRequireTextAndNormalizeEmail() {
    assertEquals("hello", UserFactoryFieldUtils.requireText("  hello ", "required"));
    assertEquals("email@example.com", UserFactoryFieldUtils.normalizeEmail("Email@Example.com".toLowerCase()));

    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> UserFactoryFieldUtils.requireText("   ", "required"));
    assertEquals("required", failure.getMessage());
  }

  private CreateUserRequest playerRequest() {
    return new CreateUserRequest(
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
        "v1",
        "v1");
  }

  private CreateUserRequest recruiterRequest(RecruiterDmOpenness openness) {
    return new CreateUserRequest(
        null,
        "Scout",
        null,
        null,
        "recruiter@example.com",
        "Org",
        "https://linkedin.com/in/recruiter",
        openness,
        "secret",
        UserRole.RECRUITER,
        true,
        OffsetDateTime.parse("2026-05-13T00:00:00Z"),
        "en",
        ConsentSource.REGISTER_PAGE,
        "v1",
        "v1");
  }
}
