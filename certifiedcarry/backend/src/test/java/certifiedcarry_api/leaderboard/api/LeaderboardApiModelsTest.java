package certifiedcarry_api.leaderboard.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import certifiedcarry_api.leaderboard.LeaderboardFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
}
