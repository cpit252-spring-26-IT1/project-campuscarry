package certifiedcarry_api.profile.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.profile.PlayerProfileFields;
import certifiedcarry_api.profile.service.PlayerProfileService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@ExtendWith(MockitoExtension.class)
class PlayerProfileControllerTest {

  @Mock private PlayerProfileService playerProfileService;

  @Test
  void getCreateAndPatchProfileUseActorScopedServiceCalls() {
    PlayerProfileController controller = new PlayerProfileController(playerProfileService);
    MockHttpServletRequest request = actorRequest();
    UsernamePasswordAuthenticationToken authentication = playerAuth();
    Map<String, Object> row =
        Map.of(
            PlayerProfileFields.ID, "1",
            PlayerProfileFields.USER_ID, "9",
            PlayerProfileFields.USERNAME, "player",
            PlayerProfileFields.ALLOW_PLAYER_CHATS, true,
            PlayerProfileFields.IS_WITH_TEAM, false,
            PlayerProfileFields.IS_VERIFIED, false);

    PlayerProfileCreateRequest createRequest =
        new PlayerProfileCreateRequest(
            "9",
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);
    when(playerProfileService.getPlayerProfilesForActor("9", 9L, false)).thenReturn(List.of(row));
    when(playerProfileService.createPlayerProfileForActor(createRequest.toServiceRequest(), 9L, false))
        .thenReturn(row);
    when(playerProfileService.patchPlayerProfileForActor("1", Map.of(PlayerProfileFields.USERNAME, "updated"), 9L, false))
        .thenReturn(row);

    List<PlayerProfileResponse> profiles = controller.getPlayerProfiles("9", authentication, request);
    var created =
        controller.createPlayerProfile(
            createRequest,
            authentication,
            request);
    PlayerProfilePatchRequest patchRequest = new PlayerProfilePatchRequest();
    patchRequest.setUsername("updated");
    var updated = controller.patchPlayerProfile("1", patchRequest, authentication, request);

    assertEquals(1, profiles.size());
    assertEquals("1", profiles.getFirst().id());
    assertEquals(HttpStatus.CREATED, created.getStatusCode());
    assertEquals("1", created.getBody().id());
    assertEquals("1", updated.getBody().id());
    verify(playerProfileService).getPlayerProfilesForActor("9", 9L, false);
    verify(playerProfileService).createPlayerProfileForActor(createRequest.toServiceRequest(), 9L, false);
    verify(playerProfileService)
        .patchPlayerProfileForActor("1", Map.of(PlayerProfileFields.USERNAME, "updated"), 9L, false);
  }

  private MockHttpServletRequest actorRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("backendUserId", "9");
    request.setAttribute("backendUserRole", "PLAYER");
    return request;
  }

  private UsernamePasswordAuthenticationToken playerAuth() {
    return new UsernamePasswordAuthenticationToken(
        "player", "n/a", List.of(new SimpleGrantedAuthority("ROLE_PLAYER")));
  }
}
