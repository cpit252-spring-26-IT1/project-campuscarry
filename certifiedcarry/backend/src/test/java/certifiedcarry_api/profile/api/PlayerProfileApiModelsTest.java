package certifiedcarry_api.profile.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import certifiedcarry_api.profile.PlayerProfileFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
}
