package certifiedcarry_api.profile.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PlayerProfileServiceOrchestrationTest {

  @Mock
  private PlayerProfileRepositoryGateway repositoryGateway;

  @Mock
  private PlayerProfilePayloadFactory payloadFactory;

  @Mock
  private PlayerProfileAccessPolicy accessPolicy;

  @Test
  void sanitizesProfilesForNonAdminButNotForAdmin() {
    PlayerProfileService service =
        new PlayerProfileService(repositoryGateway, payloadFactory, accessPolicy);
    Map<String, Object> profile = Map.of("id", "1");
    Map<String, Object> sanitized = Map.of("id", "1", "safe", true);
    when(repositoryGateway.getPlayerProfiles("7")).thenReturn(List.of(profile));
    when(accessPolicy.sanitizeProfileForActor(profile, 7L)).thenReturn(sanitized);

    assertEquals(List.of(sanitized), service.getPlayerProfilesForActor("7", 7L, false));
    assertEquals(List.of(profile), service.getPlayerProfilesForActor("7", 7L, true));
  }

  @Test
  void createAndPatchForActorApplyAccessPolicyBeforeDelegating() {
    Map<String, Object> input = new LinkedHashMap<>(Map.of("bio", "updated"));
    Map<String, Object> created = Map.of("id", "1");
    Map<String, Object> patched = Map.of("id", "2");

    PlayerProfileService service =
        new PlayerProfileService(repositoryGateway, payloadFactory, accessPolicy) {
          @Override
          public Map<String, Object> createPlayerProfile(Map<String, Object> request) {
            assertEquals(input, request);
            return created;
          }

          @Override
          public boolean isProfileOwnedBy(String profileId, long expectedUserId) {
            return true;
          }

          @Override
          public Map<String, Object> patchPlayerProfile(String profileId, Map<String, Object> request) {
            assertEquals(input, request);
            assertEquals("9", profileId);
            return patched;
          }
        };

    assertSame(created, service.createPlayerProfileForActor(input, 7L, false));
    assertSame(patched, service.patchPlayerProfileForActor("9", input, 7L, false));

    verify(accessPolicy, times(2)).enforcePayloadUserOwnership(input, 7L);
    verify(accessPolicy, times(2)).stripAdminVerificationFields(input);
  }

  @Test
  void patchForActorRejectsNonOwners() {
    PlayerProfileService service =
        new PlayerProfileService(repositoryGateway, payloadFactory, accessPolicy) {
          @Override
          public boolean isProfileOwnedBy(String profileId, long expectedUserId) {
            return false;
          }
        };

    ResponseStatusException failure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchPlayerProfileForActor("9", new LinkedHashMap<>(), 7L, false));

    assertEquals(HttpStatus.FORBIDDEN, failure.getStatusCode());
    assertEquals("You can only update your own player profile.", failure.getReason());
  }

  @Test
  void createAndPatchMapDataIntegrityAndSqlFailures() throws SQLException {
    PlayerProfileService service =
        new PlayerProfileService(repositoryGateway, payloadFactory, accessPolicy);
    PlayerProfilePayload payload = payload();
    PlayerProfileRow existing = row();
    when(payloadFactory.buildCreatePayload(any())).thenReturn(payload);
    when(payloadFactory.buildPatchPayload(any(), any())).thenReturn(payload);
    when(repositoryGateway.findPlayerProfileRow(9L)).thenReturn(existing);

    doThrow(new DataIntegrityViolationException("boom", new RuntimeException("ERROR: duplicate key value")))
        .when(repositoryGateway)
        .insertProfile(payload);
    ResponseStatusException createConflict =
        assertThrows(
            ResponseStatusException.class,
            () -> service.createPlayerProfile(new LinkedHashMap<>()));
    assertEquals("Player profile already exists for this user.", createConflict.getReason());

    doThrow(new UncategorizedSQLException("task", "sql", new SQLException("ERROR: invalid payload")))
        .when(repositoryGateway)
        .insertProfile(payload);
    ResponseStatusException createBadRequest =
        assertThrows(
            ResponseStatusException.class,
            () -> service.createPlayerProfile(new LinkedHashMap<>()));
    assertEquals("invalid payload", createBadRequest.getReason());

    doThrow(new DataIntegrityViolationException("boom", new RuntimeException("ERROR: duplicate key value")))
        .when(repositoryGateway)
        .updateProfile(9L, payload);
    ResponseStatusException patchConflict =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchPlayerProfile("9", new LinkedHashMap<>()));
    assertEquals("Player profile patch conflicts with existing records.", patchConflict.getReason());
  }

  @Test
  void patchReturnsNotFoundWhenNoRowsUpdated() {
    PlayerProfileService service =
        new PlayerProfileService(repositoryGateway, payloadFactory, accessPolicy);
    PlayerProfilePayload payload = payload();
    when(repositoryGateway.findPlayerProfileRow(9L)).thenReturn(row());
    when(payloadFactory.buildPatchPayload(any(), any())).thenReturn(payload);
    when(repositoryGateway.updateProfile(9L, payload)).thenReturn(0);

    ResponseStatusException failure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchPlayerProfile("9", new LinkedHashMap<>()));
    assertEquals("Player profile not found for id 9", failure.getReason());
  }

  private PlayerProfilePayload payload() {
    return new PlayerProfilePayload(
        7L, "Bluy", "", "Valorant", "Ascendant", true, false, null, List.of(), null, List.of(),
        null, null, "MMR", "", "", "", "NOT_SUBMITTED", "", null, false, null, null, null, null, null);
  }

  private PlayerProfileRow row() {
    return new PlayerProfileRow(
        9L, 7L, "Bluy", "", "Valorant", "Ascendant", true, false, null, List.of(), null, List.of(),
        null, null, "MMR", "", "", "", "NOT_SUBMITTED", "", null, false, null, null, null, null, null, null);
  }
}
