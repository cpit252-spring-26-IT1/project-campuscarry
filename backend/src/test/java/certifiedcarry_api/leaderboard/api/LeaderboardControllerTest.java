package certifiedcarry_api.leaderboard.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.leaderboard.LeaderboardFields;
import certifiedcarry_api.leaderboard.service.LeaderboardService;
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
class LeaderboardControllerTest {

  @Mock private LeaderboardService leaderboardService;

  @Test
  void leaderboardControllerDelegatesAllActorScopedOperations() {
    LeaderboardController controller = new LeaderboardController(leaderboardService);
    MockHttpServletRequest request = actorRequest();
    UsernamePasswordAuthenticationToken authentication = playerAuth();
    Map<String, Object> row =
        Map.of(
            LeaderboardFields.ID, "11",
            LeaderboardFields.USER_ID, "9",
            LeaderboardFields.USERNAME, "player",
            LeaderboardFields.GAME, "Valorant",
            LeaderboardFields.RANK, "Immortal");

    LeaderboardCreateRequest createRequest =
        new LeaderboardCreateRequest("9", null, null, null, null, null, null, null);
    when(leaderboardService.getLeaderboardEntriesForActor(9L, false)).thenReturn(List.of(row));
    when(leaderboardService.createLeaderboardEntryForActor(createRequest.toServiceRequest(), 9L, false))
        .thenReturn(row);
    when(leaderboardService.patchLeaderboardEntryForActor("11", Map.of(LeaderboardFields.RANK, "Radiant"), 9L, false))
        .thenReturn(row);

    List<LeaderboardResponse> entries = controller.getLeaderboardEntries(authentication, request);
    var created =
        controller.createLeaderboardEntry(
            createRequest,
            authentication,
            request);
    LeaderboardPatchRequest patchRequest = new LeaderboardPatchRequest();
    patchRequest.setRank("Radiant");
    var updated = controller.patchLeaderboardEntry("11", patchRequest, authentication, request);
    var deleted = controller.deleteLeaderboardEntry("11", authentication, request);

    assertEquals(1, entries.size());
    assertEquals("11", entries.getFirst().id());
    assertEquals(HttpStatus.CREATED, created.getStatusCode());
    assertEquals("11", created.getBody().id());
    assertEquals("11", updated.getBody().id());
    assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
    verify(leaderboardService).deleteLeaderboardEntryForActor("11", 9L, false);
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
