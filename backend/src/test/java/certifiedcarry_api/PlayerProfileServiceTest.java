package certifiedcarry_api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import certifiedcarry_api.profile.service.PlayerProfileService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class PlayerProfileServiceTest {

  @Test
  void nonAdminCannotPatchAnotherUsersProfile() {
    PlayerProfileService service =
        new PlayerProfileService(new JdbcTemplate(), new ObjectMapper(), 30) {
          @Override
          public boolean isProfileOwnedBy(String profileId, long expectedUserId) {
            return false;
          }

          @Override
          public Map<String, Object> patchPlayerProfile(String profileId, Map<String, Object> request) {
            throw new AssertionError("patchPlayerProfile should not be called for unauthorized updates.");
          }
        };

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchPlayerProfileForActor("999", Map.of("bio", "updated"), 13L, false));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    assertEquals("You can only update your own player profile.", exception.getReason());
  }
}
