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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/pending_recruiters")
public class PendingRecruiterController {

  private final PendingQueueService pendingQueueService;

  public PendingRecruiterController(PendingQueueService pendingQueueService) {
    this.pendingQueueService = pendingQueueService;
  }

  @GetMapping
  public List<PendingRecruiterResponse> getPendingRecruiters(
      @RequestParam(required = false) String userId,
      Authentication authentication, HttpServletRequest request) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActorAllowingUnlinkedAdmin(authentication, request);
    return PendingRecruiterResponse.fromServiceRows(
        pendingQueueService.getPendingRecruitersForActor(
            userId, actor.userId(), actor.backendUserRole(), actor.isAdmin()));
  }

  @PostMapping
  public ResponseEntity<PendingRecruiterResponse> createPendingRecruiter(
      @RequestBody PendingRecruiterCreateRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActor(authentication, servletRequest);
    PendingRecruiterResponse created =
        PendingRecruiterResponse.fromServiceRow(
            pendingQueueService.createPendingRecruiterForActor(
                request.toServiceRequest(),
                actor.userId(),
                actor.backendUserRole(),
                actor.isAdmin()));
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @DeleteMapping("/{pendingRecruiterId}")
  public ResponseEntity<Void> deletePendingRecruiter(
      @PathVariable String pendingRecruiterId,
      Authentication authentication,
      HttpServletRequest request) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActorAllowingUnlinkedAdmin(authentication, request);
    pendingQueueService.deletePendingRecruiterForActor(
        pendingRecruiterId, actor.userId(), actor.backendUserRole(), actor.isAdmin());
    return ResponseEntity.noContent().build();
  }
}
