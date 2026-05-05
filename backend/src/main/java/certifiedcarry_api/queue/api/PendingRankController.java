package certifiedcarry_api.queue.api;

import certifiedcarry_api.queue.service.PendingQueueService;
import certifiedcarry_api.shared.ActorRequestContext;
import certifiedcarry_api.shared.ActorRequestResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pending_ranks")
public class PendingRankController {

  private final PendingQueueService pendingQueueService;

  public PendingRankController(PendingQueueService pendingQueueService) {
    this.pendingQueueService = pendingQueueService;
  }

  @GetMapping
  public List<PendingRankResponse> getPendingRanks(
      @RequestParam(required = false) String status,
      Authentication authentication,
      HttpServletRequest request) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActorAllowingUnlinkedAdmin(authentication, request);
    return PendingRankResponse.fromServiceRows(
        pendingQueueService.getPendingRanksForActor(status, actor.userId(), actor.isAdmin()));
  }

  @PostMapping
  public ResponseEntity<PendingRankResponse> createPendingRank(
      @RequestBody PendingRankCreateRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActor(authentication, servletRequest);
    PendingRankResponse created =
        PendingRankResponse.fromServiceRow(
            pendingQueueService.createPendingRankForActor(
                request.toServiceRequest(), actor.userId(), actor.isAdmin()));
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PatchMapping("/{pendingRankId}")
  public ResponseEntity<PendingRankResponse> patchPendingRank(
      @PathVariable String pendingRankId,
      @RequestBody PendingRankPatchRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActor(authentication, servletRequest);
    PendingRankResponse updated =
        PendingRankResponse.fromServiceRow(
            pendingQueueService.patchPendingRankForActor(
                pendingRankId,
                request.toServiceRequest(),
                actor.userId(),
                actor.isAdmin()));
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{pendingRankId}")
  public ResponseEntity<Void> deletePendingRank(
      @PathVariable String pendingRankId,
      Authentication authentication,
      HttpServletRequest request) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActorAllowingUnlinkedAdmin(authentication, request);
    pendingQueueService.deletePendingRankForActor(pendingRankId, actor.userId(), actor.isAdmin());
    return ResponseEntity.noContent().build();
  }
}
