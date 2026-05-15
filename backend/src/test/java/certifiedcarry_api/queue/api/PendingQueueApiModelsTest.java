package certifiedcarry_api.queue.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import certifiedcarry_api.queue.PendingQueueFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PendingQueueApiModelsTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  @Test
  void pendingRankPatchRequestPreservesExplicitNullFields() throws Exception {
    PendingRankPatchRequest request =
        objectMapper.readValue("{\"declineReason\":null}", PendingRankPatchRequest.class);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertTrue(serviceRequest.containsKey(PendingQueueFields.DECLINE_REASON));
    assertNull(serviceRequest.get(PendingQueueFields.DECLINE_REASON));
    assertEquals(1, serviceRequest.size());
  }

  @Test
  void pendingRankPatchRequestOmitsMissingFields() throws Exception {
    PendingRankPatchRequest request =
        objectMapper.readValue("{}", PendingRankPatchRequest.class);

    assertTrue(request.toServiceRequest().isEmpty());
  }

  @Test
  void pendingRecruiterResponseKeepsExistingJsonFieldTypes() {
    PendingRecruiterResponse response =
        PendingRecruiterResponse.fromServiceRow(
            Map.of(
                PendingQueueFields.ID, "12",
                PendingQueueFields.USER_ID, "21",
                PendingQueueFields.FULL_NAME, "Recruiter",
                PendingQueueFields.EMAIL, "recruiter@example.local",
                PendingQueueFields.LINKEDIN_URL, "https://linkedin.com/in/recruiter",
                PendingQueueFields.ORGANIZATION_NAME, "Org"));

    assertEquals("12", response.id());
    assertEquals("21", response.userId());
    assertEquals("Recruiter", response.fullName());
  }

  @Test
  void pendingRankResponseMapsListFields() {
    PendingRankResponse response =
        PendingRankResponse.fromServiceRow(
            Map.of(
                PendingQueueFields.ID, "1",
                PendingQueueFields.USER_ID, "13",
                PendingQueueFields.USERNAME, "tag",
                PendingQueueFields.FULL_NAME, "Player",
                PendingQueueFields.GAME, "Valorant",
                PendingQueueFields.CLAIMED_RANK, "Immortal",
                PendingQueueFields.IN_GAME_ROLES, List.of("Duelist"),
                PendingQueueFields.ROCKET_LEAGUE_MODES, List.of("2v2"),
                PendingQueueFields.EDITED_AFTER_DECLINE, true));

    assertEquals(List.of("Duelist"), response.inGameRoles());
    assertEquals(List.of("2v2"), response.rocketLeagueModes());
    assertTrue(response.editedAfterDecline());
  }

  @Test
  void pendingRankCreateRequestMapsRocketLeagueAndRatingFields() {
    OffsetDateTime submittedAt = OffsetDateTime.parse("2026-05-13T00:00:00Z");
    PendingRankCreateRequest request =
        new PendingRankCreateRequest(
            "13",
            "player",
            "Player Name",
            "Rocket League",
            "Champion",
            List.of("Striker"),
            "Striker",
            BigDecimal.valueOf(1550),
            "MMR",
            List.of(Map.of("mode", "2v2")),
            "2v2",
            "proof.png",
            "PENDING",
            submittedAt,
            null,
            null,
            false,
            submittedAt);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertEquals("Player Name", serviceRequest.get(PendingQueueFields.FULL_NAME));
    assertEquals(List.of(Map.of("mode", "2v2")), serviceRequest.get(PendingQueueFields.ROCKET_LEAGUE_MODES));
    assertEquals(BigDecimal.valueOf(1550), serviceRequest.get(PendingQueueFields.RATING_VALUE));
    assertEquals(submittedAt, serviceRequest.get(PendingQueueFields.SUBMITTED_AT));
  }

  @Test
  void pendingRankPatchRequestTracksExplicitListsAndStatusFields() throws Exception {
    PendingRankPatchRequest request =
        objectMapper.readValue(
            """
            {
              "inGameRoles": ["Controller"],
              "status": "DECLINED",
              "editedAfterDecline": true
            }
            """,
            PendingRankPatchRequest.class);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertEquals(List.of("Controller"), serviceRequest.get(PendingQueueFields.IN_GAME_ROLES));
    assertEquals("DECLINED", serviceRequest.get(PendingQueueFields.STATUS));
    assertEquals(true, serviceRequest.get(PendingQueueFields.EDITED_AFTER_DECLINE));
  }

  @Test
  void pendingRankPatchRequestTracksNumericAndTimestampFields() throws Exception {
    PendingRankPatchRequest request =
        objectMapper.readValue(
            """
            {
              "ratingValue": 1550,
              "ratingLabel": "MMR",
              "submittedAt": "2026-05-13T00:00:00Z",
              "resolvedAt": "2026-05-13T01:00:00Z",
              "editedAt": "2026-05-13T02:00:00Z"
            }
            """,
            PendingRankPatchRequest.class);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertEquals(BigDecimal.valueOf(1550), serviceRequest.get(PendingQueueFields.RATING_VALUE));
    assertEquals("MMR", serviceRequest.get(PendingQueueFields.RATING_LABEL));
    assertEquals(OffsetDateTime.parse("2026-05-13T00:00:00Z"), serviceRequest.get(PendingQueueFields.SUBMITTED_AT));
    assertEquals(OffsetDateTime.parse("2026-05-13T01:00:00Z"), serviceRequest.get(PendingQueueFields.RESOLVED_AT));
    assertEquals(OffsetDateTime.parse("2026-05-13T02:00:00Z"), serviceRequest.get(PendingQueueFields.EDITED_AT));
  }

  @Test
  void pendingRankPatchDirectSettersPopulateAllSupportedFields() {
    OffsetDateTime now = OffsetDateTime.parse("2026-05-13T00:00:00Z");
    PendingRankPatchRequest request = new PendingRankPatchRequest();
    request.setUserId("13");
    request.setUsername("player");
    request.setFullName("Player Name");
    request.setGame("Rocket League");
    request.setClaimedRank("Champion");
    request.setInGameRoles(List.of("Support"));
    request.setInGameRole("Support");
    request.setRatingValue(BigDecimal.valueOf(1500));
    request.setRatingLabel("MMR");
    request.setRocketLeagueModes(List.of(Map.of("mode", "2v2")));
    request.setPrimaryRocketLeagueMode("2v2");
    request.setProofImage("proof.png");
    request.setStatus("PENDING");
    request.setSubmittedAt(now);
    request.setResolvedAt(now.plusHours(1));
    request.setDeclineReason("Need clearer proof");
    request.setEditedAfterDecline(true);
    request.setEditedAt(now.plusHours(2));

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertEquals(18, serviceRequest.size());
    assertEquals("13", serviceRequest.get(PendingQueueFields.USER_ID));
    assertEquals("proof.png", serviceRequest.get(PendingQueueFields.PROOF_IMAGE));
    assertEquals(true, serviceRequest.get(PendingQueueFields.EDITED_AFTER_DECLINE));
  }
}
