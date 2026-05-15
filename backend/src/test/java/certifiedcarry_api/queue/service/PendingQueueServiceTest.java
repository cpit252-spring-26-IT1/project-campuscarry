package certifiedcarry_api.queue.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PendingQueueServiceTest {

  @Mock private PendingRecruiterService pendingRecruiterService;
  @Mock private PendingRankService pendingRankService;

  @Test
  void recruiterOperationsDelegateToRecruiterService() {
    PendingQueueService service = new PendingQueueService(pendingRecruiterService, pendingRankService);
    Map<String, Object> request = Map.of("organizationName", "Org");
    Map<String, Object> created = Map.of("id", "7");
    List<Map<String, Object>> rows = List.of(created);

    when(pendingRecruiterService.getPendingRecruiters("23")).thenReturn(rows);
    when(pendingRecruiterService.getPendingRecruitersForActor("23", 9L, "RECRUITER", false))
        .thenReturn(rows);
    when(pendingRecruiterService.createPendingRecruiterForActor(request, 9L, "RECRUITER", false))
        .thenReturn(created);
    when(pendingRecruiterService.createPendingRecruiter(request)).thenReturn(created);

    assertSame(rows, service.getPendingRecruiters("23"));
    assertSame(rows, service.getPendingRecruitersForActor("23", 9L, "RECRUITER", false));
    assertSame(created, service.createPendingRecruiterForActor(request, 9L, "RECRUITER", false));
    assertSame(created, service.createPendingRecruiter(request));
    service.deletePendingRecruiterForActor("12", 9L, "RECRUITER", false);
    service.deletePendingRecruiter("12");

    verify(pendingRecruiterService).deletePendingRecruiterForActor("12", 9L, "RECRUITER", false);
    verify(pendingRecruiterService).deletePendingRecruiter("12");
  }

  @Test
  void rankOperationsDelegateToRankService() {
    PendingQueueService service = new PendingQueueService(pendingRecruiterService, pendingRankService);
    Map<String, Object> request = Map.of("game", "Valorant");
    Map<String, Object> patched = Map.of("id", "18");
    List<Map<String, Object>> rows = List.of(patched);
    OffsetDateTime declinedAt = OffsetDateTime.now();

    when(pendingRankService.getPendingRanks("PENDING")).thenReturn(rows);
    when(pendingRankService.getPendingRanksForActor("PENDING", 44L, true)).thenReturn(rows);
    when(pendingRankService.getPendingRanksForUser("DECLINED", 44L)).thenReturn(rows);
    when(pendingRankService.getLatestDeclinedTimestampForUser(44L)).thenReturn(declinedAt);
    when(pendingRankService.isPendingRankOwnedBy("18", 44L)).thenReturn(true);
    when(pendingRankService.createPendingRankForActor(request, 44L, true)).thenReturn(patched);
    when(pendingRankService.patchPendingRankForActor("18", request, 44L, true)).thenReturn(patched);
    when(pendingRankService.createPendingRank(request)).thenReturn(patched);
    when(pendingRankService.patchPendingRank("18", request)).thenReturn(patched);

    assertSame(rows, service.getPendingRanks("PENDING"));
    assertSame(rows, service.getPendingRanksForActor("PENDING", 44L, true));
    assertSame(rows, service.getPendingRanksForUser("DECLINED", 44L));
    assertEquals(declinedAt, service.getLatestDeclinedTimestampForUser(44L));
    service.enforceDeclinedResubmissionCooldown(44L);
    assertEquals(true, service.isPendingRankOwnedBy("18", 44L));
    assertSame(patched, service.createPendingRankForActor(request, 44L, true));
    assertSame(patched, service.patchPendingRankForActor("18", request, 44L, true));
    service.deletePendingRankForActor("18", 44L, true);
    assertSame(patched, service.createPendingRank(request));
    assertSame(patched, service.patchPendingRank("18", request));
    service.deletePendingRank("18");

    verify(pendingRankService).deletePendingRankForActor("18", 44L, true);
    verify(pendingRankService).deletePendingRank("18");
  }
}
