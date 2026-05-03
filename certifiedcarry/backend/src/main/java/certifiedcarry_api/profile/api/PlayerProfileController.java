package certifiedcarry_api.profile.api;

import certifiedcarry_api.profile.service.PlayerProfileService;
import certifiedcarry_api.shared.ActorRequestContext;
import certifiedcarry_api.shared.ActorRequestResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/player_profiles")
public class PlayerProfileController {

  private final PlayerProfileService playerProfileService;

  public PlayerProfileController(PlayerProfileService playerProfileService) {
    this.playerProfileService = playerProfileService;
  }

  @GetMapping
  public List<PlayerProfileResponse> getPlayerProfiles(
      @RequestParam(required = false) String userId,
      Authentication authentication,
      HttpServletRequest request) {
    ActorRequestContext actor = ActorRequestResolver.requireActor(authentication, request);
    return PlayerProfileResponse.fromServiceRows(
        playerProfileService.getPlayerProfilesForActor(userId, actor.userId(), actor.isAdmin()));
  }

  @PostMapping
  public ResponseEntity<PlayerProfileResponse> createPlayerProfile(
      @RequestBody PlayerProfileCreateRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActor(authentication, servletRequest);
    PlayerProfileResponse created =
        PlayerProfileResponse.fromServiceRow(
            playerProfileService.createPlayerProfileForActor(
                request.toServiceRequest(), actor.userId(), actor.isAdmin()));
    return ResponseEntity.status(HttpStatus.CREATED).body(created);
  }

  @PatchMapping("/{profileId}")
  public ResponseEntity<PlayerProfileResponse> patchPlayerProfile(
      @PathVariable String profileId,
      @RequestBody PlayerProfilePatchRequest request,
      Authentication authentication,
      HttpServletRequest servletRequest) {
    ActorRequestContext actor =
        ActorRequestResolver.requireActor(authentication, servletRequest);
    PlayerProfileResponse updated =
        PlayerProfileResponse.fromServiceRow(
            playerProfileService.patchPlayerProfileForActor(
                profileId, request.toServiceRequest(), actor.userId(), actor.isAdmin()));
    return ResponseEntity.ok(updated);
  }
}
