package certifiedcarry_api.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

class UserResponseMapperTest {

  @Test
  void sanitizeForNonAdminHidesSensitiveFieldsForOtherUsers() {
    UserResponse original =
        new UserResponse(
            "14",
            "Other User",
            "other_tag",
            "other@example.local",
            "org@example.local",
            "Org",
            UserRole.RECRUITER,
            UserStatus.APPROVED,
            "reason",
            OffsetDateTime.parse("2026-04-04T00:00:00Z"),
            RecruiterDmOpenness.OPEN_ALL_PLAYERS,
            OffsetDateTime.parse("2026-04-05T00:00:00Z"),
            "en",
            "terms-v1",
            "privacy-v1");

    UserResponse sanitized = UserResponseMapper.sanitizeForNonAdmin(original, 13L);

    assertEquals("14", sanitized.id());
    assertEquals("Other User", sanitized.fullName());
    assertEquals("other_tag", sanitized.username());
    assertNull(sanitized.personalEmail());
    assertNull(sanitized.email());
    assertNull(sanitized.declineReason());
    assertNull(sanitized.declinedAt());
    assertEquals(RecruiterDmOpenness.OPEN_ALL_PLAYERS, sanitized.recruiterDmOpenness());
    assertNull(sanitized.legalConsentLocale());
    assertNull(sanitized.termsVersionAccepted());
    assertNull(sanitized.privacyVersionAccepted());
  }
}
