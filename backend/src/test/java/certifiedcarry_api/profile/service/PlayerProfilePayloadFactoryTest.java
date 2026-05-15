package certifiedcarry_api.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import certifiedcarry_api.profile.PlayerProfileFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PlayerProfilePayloadFactoryTest {

  private final PlayerProfilePayloadFactory factory =
      new PlayerProfilePayloadFactory(new ObjectMapper(), 30);

  @Test
  void buildCreatePayloadAppliesDefaultsAndComputesVerificationDates() {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put(PlayerProfileFields.USER_ID, "7");
    request.put(PlayerProfileFields.USERNAME, "Bluy");
    request.put(PlayerProfileFields.GAME, "league of legends");
    request.put(PlayerProfileFields.ALLOW_PLAYER_CHATS, false);
    request.put(PlayerProfileFields.IS_WITH_TEAM, true);
    request.put(PlayerProfileFields.TEAM_NAME, "Cloud 9");
    request.put(
        PlayerProfileFields.ROCKET_LEAGUE_MODES,
        List.of(Map.of("mode", "2v2", "rank", "Champion")));
    request.put(PlayerProfileFields.IN_GAME_ROLES, List.of(" Duelist ", "", "Support"));
    request.put(PlayerProfileFields.RATING_VALUE, "1400.5");
    request.put(PlayerProfileFields.RANK_VERIFICATION_STATUS, "approved");
    request.put(PlayerProfileFields.IS_VERIFIED, true);

    PlayerProfilePayload payload =
        factory.buildCreatePayload(request);

    assertEquals(7L, payload.userId());
    assertEquals("Bluy", payload.username());
    assertEquals("LoL", payload.game());
    assertEquals("", payload.profileImage());
    assertEquals(false, payload.allowPlayerChats());
    assertEquals(true, payload.isWithTeam());
    assertEquals("Cloud 9", payload.teamName());
    assertEquals(List.of("Duelist", "Support"), payload.inGameRoles());
    assertEquals(new BigDecimal("1400.5"), payload.ratingValue());
    assertEquals("APPROVED", payload.rankVerificationStatus());
    assertNotNull(payload.rankVerifiedAt());
    assertNotNull(payload.rankExpiresAt());
    assertEquals(payload.rankVerifiedAt().plusDays(30), payload.rankExpiresAt());
    assertNull(payload.rankExpiryReminderSentAt());
  }

  @Test
  void buildCreatePayloadRejectsMissingTeamNameWhenTeamFlagIsTrue() {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put(PlayerProfileFields.USER_ID, "7");
    request.put(PlayerProfileFields.USERNAME, "Bluy");
    request.put(PlayerProfileFields.IS_WITH_TEAM, true);

    ResponseStatusException failure =
        assertThrows(ResponseStatusException.class, () -> factory.buildCreatePayload(request));
    assertEquals("teamName is required when isWithTeam is true.", failure.getReason());
  }

  @Test
  void buildPatchPayloadTransitionsVerificationAndClearsReminder() {
    OffsetDateTime oldReminder = OffsetDateTime.parse("2026-05-10T00:00:00Z");
    PlayerProfileRow existing =
        new PlayerProfileRow(
            1L,
            7L,
            "Bluy",
            "",
            "Valorant",
            "Ascendant",
            true,
            true,
            "Cloud 9",
            List.of(),
            null,
            List.of("Duelist"),
            "Duelist",
            new BigDecimal("100"),
            "RR",
            "",
            "",
            "",
            PlayerProfileFields.STATUS_PENDING,
            "",
            null,
            false,
            null,
            null,
            null,
            oldReminder,
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-05-01T00:00:00Z"));

    Map<String, Object> request = new LinkedHashMap<>();
    request.put(PlayerProfileFields.RANK_VERIFICATION_STATUS, "approved");
    request.put(PlayerProfileFields.IS_VERIFIED, true);
    request.put(PlayerProfileFields.IS_WITH_TEAM, false);

    PlayerProfilePayload payload = factory.buildPatchPayload(request, existing);

    assertEquals("APPROVED", payload.rankVerificationStatus());
    assertEquals(true, payload.isVerified());
    assertNull(payload.teamName());
    assertNotNull(payload.rankVerifiedAt());
    assertEquals(payload.rankVerifiedAt().plusDays(30), payload.rankExpiresAt());
    assertNull(payload.rankExpiryReminderSentAt());
  }

  @Test
  void buildPatchPayloadPreservesVerificationWindowWhenAlreadyVerifiedAndUntouched() {
    OffsetDateTime verifiedAt = OffsetDateTime.parse("2026-05-01T00:00:00Z");
    OffsetDateTime expiresAt = OffsetDateTime.parse("2026-05-31T00:00:00Z");
    OffsetDateTime reminderSentAt = OffsetDateTime.parse("2026-05-20T00:00:00Z");
    PlayerProfileRow existing =
        new PlayerProfileRow(
            1L,
            7L,
            "Bluy",
            "",
            "Valorant",
            "Ascendant",
            true,
            false,
            null,
            List.of(),
            null,
            List.of("Duelist"),
            "Duelist",
            null,
            "RR",
            "",
            "",
            "",
            PlayerProfileFields.STATUS_APPROVED,
            "",
            null,
            true,
            null,
            verifiedAt,
            expiresAt,
            reminderSentAt,
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-05-01T00:00:00Z"));

    PlayerProfilePayload payload =
        factory.buildPatchPayload(Map.of(PlayerProfileFields.BIO, "updated"), existing);

    assertEquals(verifiedAt, payload.rankVerifiedAt());
    assertEquals(expiresAt, payload.rankExpiresAt());
    assertEquals(reminderSentAt, payload.rankExpiryReminderSentAt());
    assertEquals("updated", payload.bio());
  }

  @Test
  void buildPatchPayloadAppliesExplicitOverridesAndClearsVerificationWhenDeclined() {
    PlayerProfileRow existing =
        new PlayerProfileRow(
            1L,
            7L,
            "Bluy",
            "avatar.png",
            "Valorant",
            "Ascendant",
            true,
            false,
            null,
            List.of(),
            null,
            List.of("Duelist"),
            "Duelist",
            new BigDecimal("100"),
            "RR",
            "proof.png",
            "bio",
            "clips",
            PlayerProfileFields.STATUS_APPROVED,
            "",
            null,
            true,
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-05-02T00:00:00Z"),
            OffsetDateTime.parse("2026-06-01T00:00:00Z"),
            OffsetDateTime.parse("2026-05-15T00:00:00Z"),
            OffsetDateTime.parse("2026-05-01T00:00:00Z"),
            OffsetDateTime.parse("2026-05-01T00:00:00Z"));

    Map<String, Object> request = new LinkedHashMap<>();
    request.put(PlayerProfileFields.USER_ID, "9");
    request.put(PlayerProfileFields.USERNAME, "Updated");
    request.put(PlayerProfileFields.PROFILE_IMAGE, " ");
    request.put(PlayerProfileFields.GAME, "rocket league");
    request.put(PlayerProfileFields.RANK, null);
    request.put(PlayerProfileFields.ALLOW_PLAYER_CHATS, false);
    request.put(PlayerProfileFields.IS_WITH_TEAM, true);
    request.put(PlayerProfileFields.TEAM_NAME, "Team One");
    request.put(
        PlayerProfileFields.ROCKET_LEAGUE_MODES,
        List.of(Map.of("mode", "3v3", "rank", "Grand Champion")));
    request.put(PlayerProfileFields.PRIMARY_ROCKET_LEAGUE_MODE, "3v3");
    request.put(PlayerProfileFields.IN_GAME_ROLES, List.of("Support"));
    request.put(PlayerProfileFields.IN_GAME_ROLE, "Support");
    request.put(PlayerProfileFields.RATING_VALUE, "999.5");
    request.put(PlayerProfileFields.RATING_LABEL, " ");
    request.put(PlayerProfileFields.PROOF_IMAGE, " ");
    request.put(PlayerProfileFields.BIO, " ");
    request.put(PlayerProfileFields.CLIPS_URL, " ");
    request.put(PlayerProfileFields.RANK_VERIFICATION_STATUS, "declined");
    request.put(PlayerProfileFields.DECLINE_REASON, "rank proof rejected");
    request.put(PlayerProfileFields.DECLINED_AT, "2026-05-20T00:00:00Z");
    request.put(PlayerProfileFields.IS_VERIFIED, false);
    request.put(PlayerProfileFields.SUBMITTED_AT, "2026-05-18T00:00:00Z");
    request.put(PlayerProfileFields.RANK_VERIFIED_AT, "2026-05-02T00:00:00Z");
    request.put(PlayerProfileFields.RANK_EXPIRES_AT, "2026-06-02T00:00:00Z");
    request.put(
        PlayerProfileFields.RANK_EXPIRY_REMINDER_SENT_AT, "2026-05-25T00:00:00Z");
    request.put(PlayerProfileFields.UPDATED_AT, "2026-05-30T00:00:00Z");

    PlayerProfilePayload payload = factory.buildPatchPayload(request, existing);

    assertEquals(9L, payload.userId());
    assertEquals("Updated", payload.username());
    assertEquals("", payload.profileImage());
    assertEquals("rocket league", payload.game());
    assertNull(payload.rank());
    assertEquals(false, payload.allowPlayerChats());
    assertEquals(true, payload.isWithTeam());
    assertEquals("Team One", payload.teamName());
    assertEquals("DECLINED", payload.rankVerificationStatus());
    assertEquals("rank proof rejected", payload.declineReason());
    assertEquals(new BigDecimal("999.5"), payload.ratingValue());
    assertEquals(PlayerProfileFields.DEFAULT_RATING_LABEL, payload.ratingLabel());
    assertEquals("", payload.proofImage());
    assertEquals("", payload.bio());
    assertEquals("", payload.clipsUrl());
    assertEquals(OffsetDateTime.parse("2026-05-30T00:00:00Z"), payload.updatedAt());
    assertNull(payload.rankVerifiedAt());
    assertNull(payload.rankExpiresAt());
    assertNull(payload.rankExpiryReminderSentAt());
  }
}
