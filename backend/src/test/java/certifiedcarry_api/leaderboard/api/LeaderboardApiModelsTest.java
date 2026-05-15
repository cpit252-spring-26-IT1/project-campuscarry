package certifiedcarry_api.leaderboard.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import certifiedcarry_api.leaderboard.LeaderboardFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeaderboardApiModelsTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  @Test
  void leaderboardPatchRequestPreservesExplicitNullFields() throws Exception {
    LeaderboardPatchRequest request =
        objectMapper.readValue("{\"role\":null}", LeaderboardPatchRequest.class);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertTrue(serviceRequest.containsKey(LeaderboardFields.ROLE));
    assertNull(serviceRequest.get(LeaderboardFields.ROLE));
    assertEquals(1, serviceRequest.size());
  }

  @Test
  void leaderboardPatchRequestOmitsMissingFields() throws Exception {
    LeaderboardPatchRequest request =
        objectMapper.readValue("{}", LeaderboardPatchRequest.class);

    assertTrue(request.toServiceRequest().isEmpty());
  }

  @Test
  void leaderboardResponseKeepsExistingJsonFieldTypes() {
    LeaderboardResponse response =
        LeaderboardResponse.fromServiceRow(
            Map.of(
                LeaderboardFields.ID, "10",
                LeaderboardFields.USER_ID, "13",
                LeaderboardFields.USERNAME, "self_tag",
                LeaderboardFields.GAME, "Valorant",
                LeaderboardFields.RANK, "Immortal",
                LeaderboardFields.RATING_LABEL, "MMR"));

    assertEquals("10", response.id());
    assertEquals("13", response.userId());
    assertEquals("self_tag", response.username());
    assertEquals("Valorant", response.game());
    assertEquals("Immortal", response.rank());
    assertEquals("MMR", response.ratingLabel());
  }

  @Test
  void leaderboardCreateRequestMapsCoreFields() {
    OffsetDateTime updatedAt = OffsetDateTime.parse("2026-05-13T00:00:00Z");
    LeaderboardCreateRequest request =
        new LeaderboardCreateRequest(
            "13", "self_tag", "Valorant", "Immortal", "Duelist", BigDecimal.valueOf(700), "RR", updatedAt);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertEquals("13", serviceRequest.get(LeaderboardFields.USER_ID));
    assertEquals("Valorant", serviceRequest.get(LeaderboardFields.GAME));
    assertEquals(BigDecimal.valueOf(700), serviceRequest.get(LeaderboardFields.RATING_VALUE));
    assertEquals(updatedAt, serviceRequest.get(LeaderboardFields.UPDATED_AT));
  }

  @Test
  void leaderboardPatchRequestTracksNumericAndTimestampFields() throws Exception {
    LeaderboardPatchRequest request =
        objectMapper.readValue(
            """
            {
              "ratingValue": 777,
              "ratingLabel": "RR",
              "updatedAt": "2026-05-13T00:00:00Z"
            }
            """,
            LeaderboardPatchRequest.class);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertEquals(BigDecimal.valueOf(777), serviceRequest.get(LeaderboardFields.RATING_VALUE));
    assertEquals("RR", serviceRequest.get(LeaderboardFields.RATING_LABEL));
    assertEquals(OffsetDateTime.parse("2026-05-13T00:00:00Z"), serviceRequest.get(LeaderboardFields.UPDATED_AT));
  }

  @Test
  void leaderboardPatchDirectSettersPopulateEverySupportedField() {
    OffsetDateTime now = OffsetDateTime.parse("2026-05-13T00:00:00Z");
    LeaderboardPatchRequest request = new LeaderboardPatchRequest();
    request.setUserId("13");
    request.setUsername("self_tag");
    request.setGame("Valorant");
    request.setRank("Radiant");
    request.setRole("Duelist");
    request.setRatingValue(BigDecimal.valueOf(999));
    request.setRatingLabel("RR");
    request.setUpdatedAt(now);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertEquals(8, serviceRequest.size());
    assertEquals("13", serviceRequest.get(LeaderboardFields.USER_ID));
    assertEquals("Radiant", serviceRequest.get(LeaderboardFields.RANK));
    assertEquals(BigDecimal.valueOf(999), serviceRequest.get(LeaderboardFields.RATING_VALUE));
  }
}
