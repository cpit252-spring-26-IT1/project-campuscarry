package certifiedcarry_api.queue.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import certifiedcarry_api.queue.PendingQueueFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
}
