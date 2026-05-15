package certifiedcarry_api.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import certifiedcarry_api.profile.PlayerProfileFields;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PlayerProfileAccessPolicyTest {

  private final PlayerProfileAccessPolicy policy = new PlayerProfileAccessPolicy();

  @Test
  void returnsOriginalProfileForOwnerAndSanitizedCopyForOthers() {
    Map<String, Object> profile = new LinkedHashMap<>();
    profile.put(PlayerProfileFields.USER_ID, "42");
    profile.put(PlayerProfileFields.PROOF_IMAGE, "proof.png");
    profile.put(PlayerProfileFields.DECLINE_REASON, "reason");
    profile.put(PlayerProfileFields.DECLINED_AT, "2026-05-13T00:00:00Z");

    assertSame(profile, policy.sanitizeProfileForActor(profile, 42L));

    Map<String, Object> sanitized = policy.sanitizeProfileForActor(profile, 7L);
    assertNotSame(profile, sanitized);
    assertEquals("", sanitized.get(PlayerProfileFields.PROOF_IMAGE));
    assertEquals("", sanitized.get(PlayerProfileFields.DECLINE_REASON));
    assertNull(sanitized.get(PlayerProfileFields.DECLINED_AT));
  }

  @Test
  void enforcesPayloadOwnershipAndBackfillsMissingUserId() {
    Map<String, Object> request = new LinkedHashMap<>();
    policy.enforcePayloadUserOwnership(request, 77L);
    assertEquals("77", request.get(PlayerProfileFields.USER_ID));

    Map<String, Object> matching = new LinkedHashMap<>();
    matching.put(PlayerProfileFields.USER_ID, "77");
    policy.enforcePayloadUserOwnership(matching, 77L);
    assertEquals("77", matching.get(PlayerProfileFields.USER_ID));

    Map<String, Object> mismatched = new LinkedHashMap<>();
    mismatched.put(PlayerProfileFields.USER_ID, "12");
    ResponseStatusException failure =
        assertThrows(
            ResponseStatusException.class,
            () -> policy.enforcePayloadUserOwnership(mismatched, 77L));
    assertEquals("You can only submit updates for your own userId.", failure.getReason());
  }

  @Test
  void stripsAdminOnlyVerificationFields() {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put(PlayerProfileFields.RANK_VERIFICATION_STATUS, "APPROVED");
    request.put(PlayerProfileFields.IS_VERIFIED, true);
    request.put(PlayerProfileFields.DECLINE_REASON, "reason");
    request.put(PlayerProfileFields.DECLINED_AT, "now");
    request.put(PlayerProfileFields.RANK_VERIFIED_AT, "now");
    request.put(PlayerProfileFields.RANK_EXPIRES_AT, "later");
    request.put(PlayerProfileFields.RANK_EXPIRY_REMINDER_SENT_AT, "reminded");

    policy.stripAdminVerificationFields(request);

    assertEquals(0, request.size());
  }
}
