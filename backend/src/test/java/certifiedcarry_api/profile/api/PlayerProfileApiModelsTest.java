package certifiedcarry_api.profile.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import certifiedcarry_api.profile.PlayerProfileFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PlayerProfileApiModelsTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  @Test
  void playerProfilePatchRequestPreservesExplicitNullFields() throws Exception {
    PlayerProfilePatchRequest request =
        objectMapper.readValue("{\"declineReason\":null}", PlayerProfilePatchRequest.class);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertTrue(serviceRequest.containsKey(PlayerProfileFields.DECLINE_REASON));
    assertNull(serviceRequest.get(PlayerProfileFields.DECLINE_REASON));
    assertEquals(1, serviceRequest.size());
  }

  @Test
  void playerProfilePatchRequestOmitsMissingFields() throws Exception {
    PlayerProfilePatchRequest request =
        objectMapper.readValue("{}", PlayerProfilePatchRequest.class);

    assertTrue(request.toServiceRequest().isEmpty());
  }

  @Test
  void playerProfileResponseKeepsExistingJsonFieldTypes() {
    PlayerProfileResponse response =
        PlayerProfileResponse.fromServiceRow(
            Map.of(
                PlayerProfileFields.ID, "1",
                PlayerProfileFields.USER_ID, "13",
                PlayerProfileFields.USERNAME, "player",
                PlayerProfileFields.ROCKET_LEAGUE_MODES, List.of("2v2"),
                PlayerProfileFields.IN_GAME_ROLES, List.of("Support"),
                PlayerProfileFields.ALLOW_PLAYER_CHATS, true,
                PlayerProfileFields.IS_WITH_TEAM, false,
                PlayerProfileFields.IS_VERIFIED, false));

    assertEquals("1", response.id());
    assertEquals("13", response.userId());
    assertEquals("player", response.username());
    assertEquals(List.of("2v2"), response.rocketLeagueModes());
    assertEquals(List.of("Support"), response.inGameRoles());
    assertTrue(response.allowPlayerChats());
  }

  @Test
  void playerProfileCreateRequestMapsCoreFieldsToServiceRequest() {
    OffsetDateTime submittedAt = OffsetDateTime.parse("2026-05-13T00:00:00Z");
    PlayerProfileCreateRequest request =
        new PlayerProfileCreateRequest(
            "13",
            "player",
            "profile.png",
            "Rocket League",
            "Champion",
            true,
            false,
            null,
            List.of(Map.of("mode", "2v2")),
            "2v2",
            List.of("Support"),
            "Support",
            BigDecimal.valueOf(1450),
            "MMR",
            "proof.png",
            "bio",
            "https://clips.example/player",
            "PENDING",
            null,
            null,
            false,
            submittedAt,
            null,
            null,
            null,
            submittedAt);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertEquals("13", serviceRequest.get(PlayerProfileFields.USER_ID));
    assertEquals("Rocket League", serviceRequest.get(PlayerProfileFields.GAME));
    assertEquals("Champion", serviceRequest.get(PlayerProfileFields.RANK));
    assertEquals(List.of(Map.of("mode", "2v2")), serviceRequest.get(PlayerProfileFields.ROCKET_LEAGUE_MODES));
    assertEquals(BigDecimal.valueOf(1450), serviceRequest.get(PlayerProfileFields.RATING_VALUE));
    assertEquals(submittedAt, serviceRequest.get(PlayerProfileFields.SUBMITTED_AT));
  }

  @Test
  void playerProfilePatchRequestTracksExplicitFalseAndListValues() throws Exception {
    PlayerProfilePatchRequest request =
        objectMapper.readValue(
            """
            {
              "allowPlayerChats": false,
              "isWithTeam": true,
              "teamName": "Certified Carry",
              "inGameRoles": ["Controller", "Sentinel"]
            }
            """,
            PlayerProfilePatchRequest.class);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertEquals(false, serviceRequest.get(PlayerProfileFields.ALLOW_PLAYER_CHATS));
    assertEquals(true, serviceRequest.get(PlayerProfileFields.IS_WITH_TEAM));
    assertEquals("Certified Carry", serviceRequest.get(PlayerProfileFields.TEAM_NAME));
    assertEquals(List.of("Controller", "Sentinel"), serviceRequest.get(PlayerProfileFields.IN_GAME_ROLES));
  }

  @Test
  void playerProfilePatchDirectSettersPopulateAllSupportedFields() {
    OffsetDateTime now = OffsetDateTime.parse("2026-05-13T00:00:00Z");
    PlayerProfilePatchRequest request = new PlayerProfilePatchRequest();
    request.setUserId("13");
    request.setUsername("player");
    request.setProfileImage("profile.png");
    request.setGame("Rocket League");
    request.setRank("Champion");
    request.setAllowPlayerChats(false);
    request.setIsWithTeam(true);
    request.setTeamName("Certified Carry");
    request.setRocketLeagueModes(List.of(Map.of("mode", "2v2")));
    request.setPrimaryRocketLeagueMode("2v2");
    request.setInGameRoles(List.of("Controller"));
    request.setInGameRole("Controller");
    request.setRatingValue(BigDecimal.valueOf(1500));
    request.setRatingLabel("MMR");
    request.setProofImage("proof.png");
    request.setBio("bio");
    request.setClipsUrl("https://clips.example/player");
    request.setRankVerificationStatus("PENDING");
    request.setDeclineReason("Need clearer image");
    request.setDeclinedAt(now);
    request.setIsVerified(false);
    request.setSubmittedAt(now);
    request.setRankVerifiedAt(now.plusHours(1));
    request.setRankExpiresAt(now.plusDays(30));
    request.setRankExpiryReminderSentAt(now.plusDays(20));
    request.setUpdatedAt(now.plusHours(2));

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertEquals(26, serviceRequest.size());
    assertEquals("13", serviceRequest.get(PlayerProfileFields.USER_ID));
    assertEquals("proof.png", serviceRequest.get(PlayerProfileFields.PROOF_IMAGE));
    assertEquals(now.plusDays(30), serviceRequest.get(PlayerProfileFields.RANK_EXPIRES_AT));
  }
}
