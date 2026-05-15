package certifiedcarry_api.queue.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import certifiedcarry_api.queue.PendingQueueFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PendingRankServiceTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void actorGuardsEnforceOwnershipCooldownAndRoleAwareReads() {
    Map<String, Object> payload = Map.of(PendingQueueFields.ID, "88");

    PendingRankService service =
        new PendingRankService(jdbcTemplate, objectMapper) {
          @Override
          public List<Map<String, Object>> getPendingRanks(String status) {
            return List.of(Map.of(PendingQueueFields.STATUS, status == null ? "ALL" : status));
          }

          @Override
          public List<Map<String, Object>> getPendingRanksForUser(String status, long userId) {
            return List.of(Map.of(PendingQueueFields.USER_ID, String.valueOf(userId)));
          }

          @Override
          public OffsetDateTime getLatestDeclinedTimestampForUser(long userId) {
            return userId == 14L ? OffsetDateTime.now().minusHours(2) : null;
          }

          @Override
          public boolean isPendingRankOwnedBy(String pendingRankId, long expectedUserId) {
            return "owned".equals(pendingRankId);
          }

          @Override
          public Map<String, Object> createPendingRank(Map<String, Object> request) {
            return request;
          }

          @Override
          public Map<String, Object> patchPendingRank(String pendingRankId, Map<String, Object> request) {
            Map<String, Object> patched = new LinkedHashMap<>(request);
            patched.put(PendingQueueFields.ID, pendingRankId);
            return patched;
          }

          @Override
          public void deletePendingRank(String pendingRankId) {
            // no-op
          }
        };

    assertEquals(
        List.of(Map.of(PendingQueueFields.STATUS, "approved")),
        service.getPendingRanksForActor("approved", 7L, true));
    assertEquals(
        List.of(Map.of(PendingQueueFields.USER_ID, "7")),
        service.getPendingRanksForActor(null, 7L, false));

    Map<String, Object> created =
        service.createPendingRankForActor(new LinkedHashMap<>(), 7L, false);
    assertEquals("7", created.get(PendingQueueFields.USER_ID));

    ResponseStatusException ownershipFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createPendingRankForActor(
                    new LinkedHashMap<>(Map.of(PendingQueueFields.USER_ID, "9")), 7L, false));
    assertEquals(HttpStatus.FORBIDDEN, ownershipFailure.getStatusCode());
    assertEquals("You can only submit updates for your own userId.", ownershipFailure.getReason());

    ResponseStatusException cooldownFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createPendingRankForActor(
                    new LinkedHashMap<>(Map.of(PendingQueueFields.USER_ID, "14")), 14L, false));
    assertEquals(HttpStatus.TOO_MANY_REQUESTS, cooldownFailure.getStatusCode());
    assertTrue(cooldownFailure.getReason().startsWith(
        "You can resubmit rank verification after the 7-day cooldown."));

    ResponseStatusException patchFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchPendingRankForActor("other", new LinkedHashMap<>(), 7L, false));
    assertEquals("You can only edit your own rank submissions.", patchFailure.getReason());

    Map<String, Object> patched =
        service.patchPendingRankForActor("owned", new LinkedHashMap<>(), 7L, true);
    assertEquals("owned", patched.get(PendingQueueFields.ID));

    ResponseStatusException deleteFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.deletePendingRankForActor("other", 7L, false));
    assertEquals("You can only delete your own rank submissions.", deleteFailure.getReason());
    assertDoesNotThrow(() -> service.deletePendingRankForActor("owned", 7L, false));
  }

  @Test
  void queryAndOwnershipHelpersNormalizeStatusAndMapRows() throws SQLException {
    PendingRankService service = new PendingRankService(jdbcTemplate, objectMapper);

    when(jdbcTemplate.queryForObject(anyString(), any(Class.class), anyLong(), anyLong()))
        .thenReturn(1)
        .thenReturn(null);

    assertTrue(service.isPendingRankOwnedBy("8", 7L));
    assertFalse(service.isPendingRankOwnedBy("8", 7L));

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockPendingRankResultSet(17L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq("APPROVED"));

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockPendingRankResultSet(21L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq(9L), eq("DECLINED"));

    List<Map<String, Object>> approvedRows = service.getPendingRanks(" approved ");
    assertEquals(1, approvedRows.size());
    assertEquals("17", approvedRows.get(0).get(PendingQueueFields.ID));
    assertEquals("PENDING", approvedRows.get(0).get(PendingQueueFields.STATUS));
    assertEquals(List.of("duelist", "igl"), approvedRows.get(0).get(PendingQueueFields.IN_GAME_ROLES));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> rocketLeagueModes =
        (List<Map<String, Object>>) approvedRows.get(0).get(PendingQueueFields.ROCKET_LEAGUE_MODES);
    assertEquals("2v2", rocketLeagueModes.get(0).get("mode"));

    List<Map<String, Object>> declinedRows = service.getPendingRanksForUser("declined", 9L);
    assertEquals("21", declinedRows.get(0).get(PendingQueueFields.ID));

    ResponseStatusException invalidStatus =
        assertThrows(ResponseStatusException.class, () -> service.getPendingRanks("later"));
    assertEquals("status must be one of PENDING, APPROVED, or DECLINED.", invalidStatus.getReason());
  }

  @Test
  void createPatchDeleteAndCooldownBranchesMapFailuresCorrectly() throws SQLException {
    PendingRankService service = new PendingRankService(jdbcTemplate, objectMapper);
    Map<String, Object> request = validPendingRankRequest();

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new DataIntegrityViolationException("boom"));

    ResponseStatusException createConflict =
        assertThrows(ResponseStatusException.class, () -> service.createPendingRank(request));
    assertEquals("Unable to create pending rank record due to data integrity constraints.",
        createConflict.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(
            new UncategorizedSQLException(
                "task", "sql", new SQLException("ERROR: invalid pending rank payload")));

    ResponseStatusException createBadRequest =
        assertThrows(ResponseStatusException.class, () -> service.createPendingRank(request));
    assertEquals("invalid pending rank payload", createBadRequest.getReason());

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockPendingRankResultSet(44L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq(44L));

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenReturn(0);
    ResponseStatusException patchNotFound =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchPendingRank("44", Map.of(PendingQueueFields.STATUS, "APPROVED")));
    assertEquals("Pending rank record not found for id 44", patchNotFound.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new DataIntegrityViolationException("boom"));
    ResponseStatusException patchConflict =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchPendingRank("44", Map.of(PendingQueueFields.STATUS, "APPROVED")));
    assertEquals("Unable to patch pending rank record due to data integrity constraints.",
        patchConflict.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(
            new UncategorizedSQLException(
                "task", "sql", new SQLException("ERROR: invalid patch payload")));
    ResponseStatusException patchBadRequest =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchPendingRank("44", Map.of(PendingQueueFields.STATUS, "APPROVED")));
    assertEquals("invalid patch payload", patchBadRequest.getReason());

    when(jdbcTemplate.update(anyString(), anyLong())).thenReturn(0);
    ResponseStatusException deleteFailure =
        assertThrows(ResponseStatusException.class, () -> service.deletePendingRank("44"));
    assertEquals("Pending rank record not found for id 44", deleteFailure.getReason());

    PendingRankService cooldownService =
        new PendingRankService(jdbcTemplate, objectMapper) {
          @Override
          public OffsetDateTime getLatestDeclinedTimestampForUser(long userId) {
            return OffsetDateTime.now().minusDays(8);
          }
        };
    assertDoesNotThrow(() -> cooldownService.enforceDeclinedResubmissionCooldown(7L));
  }

  @Test
  void successPathsCreatePatchAndLookupPendingRanks() throws SQLException {
    PendingRankService service = new PendingRankService(jdbcTemplate, objectMapper);
    Map<String, Object> request = validPendingRankRequest();

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockPendingRankResultSet(77L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq(77L));
    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenReturn(77L)
        .thenReturn(1);
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(7L)))
        .thenReturn(List.of(OffsetDateTime.parse("2026-05-13T05:00:00Z")));
    when(jdbcTemplate.queryForObject(anyString(), any(Class.class), eq(77L), eq(7L)))
        .thenReturn(1)
        .thenReturn(null);

    Map<String, Object> created = service.createPendingRank(request);
    Map<String, Object> patched =
        service.patchPendingRank(
            "77",
            Map.of(
                PendingQueueFields.STATUS, "APPROVED",
                PendingQueueFields.PROOF_IMAGE, "",
                PendingQueueFields.EDITED_AFTER_DECLINE, false));
    OffsetDateTime latestDeclined = service.getLatestDeclinedTimestampForUser(7L);
    boolean owned = service.isPendingRankOwnedBy("77", 7L);
    boolean notOwned = service.isPendingRankOwnedBy("77", 7L);

    assertEquals("77", created.get(PendingQueueFields.ID));
    assertEquals("77", patched.get(PendingQueueFields.ID));
    assertEquals("https://cdn.example/proof.png", patched.get(PendingQueueFields.PROOF_IMAGE));
    assertNotNull(latestDeclined);
    assertTrue(owned);
    assertFalse(notOwned);
  }

  @Test
  void nullStatusReadsAndCreateFailureWithoutReturnedIdAreHandled() throws SQLException {
    PendingRankService service = new PendingRankService(jdbcTemplate, objectMapper);

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockPendingRankResultSet(31L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class));

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockPendingRankResultSet(32L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq(7L));

    List<Map<String, Object>> allRanks = service.getPendingRanks(null);
    List<Map<String, Object>> actorRanks = service.getPendingRanksForUser(null, 7L);

    assertEquals("31", allRanks.get(0).get(PendingQueueFields.ID));
    assertEquals("32", actorRanks.get(0).get(PendingQueueFields.ID));

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenAnswer(
            invocation -> {
              PreparedStatementCreator creator = invocation.getArgument(0);
              @SuppressWarnings("unchecked")
              PreparedStatementCallback<Long> callback = invocation.getArgument(1);
              Connection connection = mock(Connection.class);
              java.sql.Array textArray = mock(java.sql.Array.class);
              java.sql.PreparedStatement statement = mock(java.sql.PreparedStatement.class);
              ResultSet resultSet = mock(ResultSet.class);

              when(connection.prepareStatement(anyString())).thenReturn(statement);
              when(connection.createArrayOf(eq("text"), any())).thenReturn(textArray);
              when(statement.executeQuery()).thenReturn(resultSet);
              when(resultSet.next()).thenReturn(false);

              creator.createPreparedStatement(connection);
              return callback.doInPreparedStatement(statement);
            });

    ResponseStatusException failure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.createPendingRank(validPendingRankRequest()));
    assertEquals("Failed to create pending rank record.", failure.getReason());
  }

  private Map<String, Object> validPendingRankRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put(PendingQueueFields.USER_ID, "7");
    request.put(PendingQueueFields.USERNAME, "bluy");
    request.put(PendingQueueFields.FULL_NAME, "Blue Y");
    request.put(PendingQueueFields.GAME, "valorant");
    request.put(PendingQueueFields.CLAIMED_RANK, "Immortal");
    request.put(PendingQueueFields.IN_GAME_ROLES, List.of("duelist", "igl"));
    request.put(PendingQueueFields.IN_GAME_ROLE, "duelist");
    request.put(PendingQueueFields.RATING_VALUE, "1500");
    request.put(PendingQueueFields.RATING_LABEL, "RR");
    request.put(PendingQueueFields.ROCKET_LEAGUE_MODES, List.of(Map.of("mode", "2v2", "rank", "Champ")));
    request.put(PendingQueueFields.PRIMARY_ROCKET_LEAGUE_MODE, "2v2");
    request.put(PendingQueueFields.PROOF_IMAGE, "https://cdn.example/proof.png");
    request.put(PendingQueueFields.STATUS, "pending");
    request.put(PendingQueueFields.SUBMITTED_AT, "2026-05-13T00:00:00Z");
    request.put(PendingQueueFields.RESOLVED_AT, "2026-05-13T01:00:00Z");
    request.put(PendingQueueFields.DECLINE_REASON, "Needs clearer image");
    request.put(PendingQueueFields.EDITED_AFTER_DECLINE, true);
    request.put(PendingQueueFields.EDITED_AT, "2026-05-13T02:00:00Z");
    return request;
  }

  private ResultSet mockPendingRankResultSet(long id) throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    java.sql.Array rolesArray = mock(java.sql.Array.class);
    java.sql.Array modesArray = mock(java.sql.Array.class);
    OffsetDateTime submittedAt = OffsetDateTime.parse("2026-05-13T00:00:00Z");
    OffsetDateTime resolvedAt = OffsetDateTime.parse("2026-05-13T01:00:00Z");
    OffsetDateTime editedAt = OffsetDateTime.parse("2026-05-13T02:00:00Z");
    OffsetDateTime updatedAt = OffsetDateTime.parse("2026-05-13T03:00:00Z");

    when(rolesArray.getArray()).thenReturn(new String[] {"duelist", "igl"});
    when(modesArray.getArray())
        .thenReturn(
            new String[] {
              "{\"mode\":\"2v2\",\"rank\":\"Champ\",\"ratingValue\":1500,\"ratingLabel\":\"MMR\"}"
            });

    when(resultSet.getLong("id")).thenReturn(id);
    when(resultSet.getLong("user_id")).thenReturn(7L);
    when(resultSet.getString("username")).thenReturn("bluy");
    when(resultSet.getString("full_name")).thenReturn("Blue Y");
    when(resultSet.getString("game")).thenReturn("VALORANT");
    when(resultSet.getString("claimed_rank")).thenReturn("Immortal");
    when(resultSet.getArray("in_game_roles")).thenReturn(rolesArray);
    when(resultSet.getString("in_game_role")).thenReturn("duelist");
    when(resultSet.getBigDecimal("rating_value")).thenReturn(new BigDecimal("1500"));
    when(resultSet.getString("rating_label")).thenReturn("RR");
    when(resultSet.getArray("rocket_league_modes")).thenReturn(modesArray);
    when(resultSet.getString("primary_rocket_league_mode")).thenReturn("2v2");
    when(resultSet.getString("proof_image")).thenReturn("https://cdn.example/proof.png");
    when(resultSet.getString("status")).thenReturn("PENDING");
    when(resultSet.getObject("submitted_at", OffsetDateTime.class)).thenReturn(submittedAt);
    when(resultSet.getObject("resolved_at", OffsetDateTime.class)).thenReturn(resolvedAt);
    when(resultSet.getString("decline_reason")).thenReturn("Needs clearer image");
    when(resultSet.getBoolean("edited_after_decline")).thenReturn(true);
    when(resultSet.getObject("edited_at", OffsetDateTime.class)).thenReturn(editedAt);
    when(resultSet.getObject("updated_at", OffsetDateTime.class)).thenReturn(updatedAt);
    return resultSet;
  }
}
