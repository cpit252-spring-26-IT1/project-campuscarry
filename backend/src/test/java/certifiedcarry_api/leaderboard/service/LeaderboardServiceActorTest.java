package certifiedcarry_api.leaderboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class LeaderboardServiceActorTest {

  @Test
  void actorGuardsProtectCreatePatchAndDelete() {
    Map<String, Object> payload = Map.of("id", "1");

    LeaderboardService service =
        new LeaderboardService(new JdbcTemplate()) {
          @Override
          public Map<String, Object> createLeaderboardEntry(Map<String, Object> request) {
            return payload;
          }

          @Override
          public Map<String, Object> patchLeaderboardEntry(String leaderboardEntryId, Map<String, Object> request) {
            return payload;
          }

          @Override
          public boolean isLeaderboardEntryOwnedBy(String leaderboardEntryId, long expectedUserId) {
            return "owned".equals(leaderboardEntryId);
          }

          @Override
          public void deleteLeaderboardEntry(String leaderboardEntryId) {
            // no-op
          }

          @Override
          public List<Map<String, Object>> getLeaderboardEntries() {
            return List.of(payload);
          }
        };

    ResponseStatusException createFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.createLeaderboardEntryForActor(payload, 7L, false));
    assertEquals(HttpStatus.FORBIDDEN, createFailure.getStatusCode());
    assertEquals("Only admins can create leaderboard entries.", createFailure.getReason());

    ResponseStatusException patchFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchLeaderboardEntryForActor("1", payload, 7L, false));
    assertEquals("Only admins can update leaderboard entries.", patchFailure.getReason());

    ResponseStatusException deleteFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.deleteLeaderboardEntryForActor("not-owned", 7L, false));
    assertEquals("You can only remove your own leaderboard entries.", deleteFailure.getReason());

    assertSame(payload, service.createLeaderboardEntryForActor(payload, 7L, true));
    assertSame(payload, service.patchLeaderboardEntryForActor("1", payload, 7L, true));
    service.deleteLeaderboardEntryForActor("owned", 7L, false);
    service.deleteLeaderboardEntryForActor("anything", 7L, true);
    assertEquals(List.of(payload), service.getLeaderboardEntriesForActor(7L, false));
  }
}
