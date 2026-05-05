package certifiedcarry_api.leaderboard.api;

import certifiedcarry_api.leaderboard.service.LeaderboardService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/leaderboard")
public class LeaderboardController {

  private final LeaderboardService leaderboardService;

  public LeaderboardController(LeaderboardService leaderboardService) {
    this.leaderboardService = leaderboardService;
  }

  @GetMapping
  public List<LeaderboardResponse> getLeaderboardEntries(
      Authentication authentication, HttpServletRequest request) {
    ActorRequestContext actor = ActorRequestResolver.requireActor(authentication, request);
    return LeaderboardResponse.fromServiceRows(
        leaderboardService.getLeaderboardEntriesForActor(actor.userId(), actor.isAdmin()));
  }

  @PostMapping
  public ResponseEntity<LeaderboardResponse> createLeaderboardEntry(
      @RequestBody LeaderboardCreateRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActor(authentication, servletRequest);
    LeaderboardResponse created =
        LeaderboardResponse.fromServiceRow(
            leaderboardService.createLeaderboardEntryForActor(
                request.toServiceRequest(), actor.userId(), actor.isAdmin()));
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PatchMapping("/{leaderboardEntryId}")
  public ResponseEntity<LeaderboardResponse> patchLeaderboardEntry(
      @PathVariable String leaderboardEntryId,
      @RequestBody LeaderboardPatchRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActor(authentication, servletRequest);
    LeaderboardResponse updated =
        LeaderboardResponse.fromServiceRow(
            leaderboardService.patchLeaderboardEntryForActor(
                leaderboardEntryId,
                request.toServiceRequest(),
                actor.userId(),
                actor.isAdmin()));
    return ResponseEntity.ok(updated);
  }

  @DeleteMapping("/{leaderboardEntryId}")
  public ResponseEntity<Void> deleteLeaderboardEntry(
      @PathVariable String leaderboardEntryId,
      Authentication authentication,
      HttpServletRequest request) {
    ActorRequestContext actor = ActorRequestResolver.requireActor(authentication, request);
    leaderboardService.deleteLeaderboardEntryForActor(
        leaderboardEntryId, actor.userId(), actor.isAdmin());
    return ResponseEntity.noContent().build();
  }
}
