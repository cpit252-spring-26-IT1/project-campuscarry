package certifiedcarry_api.queue.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.queue.PendingQueueFields;
import certifiedcarry_api.queue.service.PendingQueueService;
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
class PendingRankControllerTest {

  @Mock private PendingQueueService pendingQueueService;

  @Test
  void pendingRankControllerSupportsAdminReadsAndPlayerMutations() {
    PendingRankController controller = new PendingRankController(pendingQueueService);
    MockHttpServletRequest playerRequest = playerRequest();
    MockHttpServletRequest adminRequest = new MockHttpServletRequest();
    UsernamePasswordAuthenticationToken playerAuth = playerAuth();
    UsernamePasswordAuthenticationToken adminAuth = adminAuth();
    Map<String, Object> row =
        Map.of(
            PendingQueueFields.ID, "18",
            PendingQueueFields.USER_ID, "9",
            PendingQueueFields.USERNAME, "player",
            PendingQueueFields.FULL_NAME, "Player Name",
            PendingQueueFields.GAME, "Valorant",
            PendingQueueFields.CLAIMED_RANK, "Immortal",
            PendingQueueFields.EDITED_AFTER_DECLINE, false);

    PendingRankCreateRequest createRequest =
        new PendingRankCreateRequest(
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
            null);
    when(pendingQueueService.getPendingRanksForActor("PENDING", 0L, true)).thenReturn(List.of(row));
    when(pendingQueueService.createPendingRankForActor(createRequest.toServiceRequest(), 9L, false))
        .thenReturn(row);
    when(pendingQueueService.patchPendingRankForActor("18", Map.of(PendingQueueFields.STATUS, "PENDING"), 9L, false))
        .thenReturn(row);

    List<PendingRankResponse> pending = controller.getPendingRanks("PENDING", adminAuth, adminRequest);
    var created =
        controller.createPendingRank(
            createRequest,
            playerAuth,
            playerRequest);
    PendingRankPatchRequest patchRequest = new PendingRankPatchRequest();
    patchRequest.setStatus("PENDING");
    var updated = controller.patchPendingRank("18", patchRequest, playerAuth, playerRequest);
    var deleted = controller.deletePendingRank("18", adminAuth, adminRequest);

    assertEquals(1, pending.size());
    assertEquals("18", pending.getFirst().id());
    assertEquals(HttpStatus.CREATED, created.getStatusCode());
    assertEquals("18", created.getBody().id());
    assertEquals("18", updated.getBody().id());
    assertEquals(HttpStatus.NO_CONTENT, deleted.getStatusCode());
    verify(pendingQueueService).deletePendingRankForActor("18", 0L, true);
  }

  private MockHttpServletRequest playerRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("backendUserId", "9");
    request.setAttribute("backendUserRole", "PLAYER");
    return request;
  }

  private UsernamePasswordAuthenticationToken playerAuth() {
    return new UsernamePasswordAuthenticationToken(
        "player", "n/a", List.of(new SimpleGrantedAuthority("ROLE_PLAYER")));
  }

  private UsernamePasswordAuthenticationToken adminAuth() {
    return new UsernamePasswordAuthenticationToken(
        "admin", "n/a", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }
}
