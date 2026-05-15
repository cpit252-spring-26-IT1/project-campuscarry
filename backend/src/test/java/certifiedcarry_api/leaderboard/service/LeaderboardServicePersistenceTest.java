package certifiedcarry_api.leaderboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import certifiedcarry_api.leaderboard.LeaderboardFields;
import java.math.BigDecimal;
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
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class LeaderboardServicePersistenceTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Test
  void queryAndOwnershipHelpersMapLeaderboardRows() throws SQLException {
    LeaderboardService service = new LeaderboardService(jdbcTemplate);

    when(jdbcTemplate.queryForObject(anyString(), any(Class.class), anyLong(), anyLong()))
        .thenReturn(1)
        .thenReturn(null);

    assertTrue(service.isLeaderboardEntryOwnedBy("7", 12L));
    assertFalse(service.isLeaderboardEntryOwnedBy("7", 12L));

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockLeaderboardResultSet(5L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class));

    List<Map<String, Object>> rows = service.getLeaderboardEntries();
    assertEquals(1, rows.size());
    assertEquals("5", rows.get(0).get(LeaderboardFields.ID));
    assertEquals("VALORANT", rows.get(0).get(LeaderboardFields.GAME));
    assertEquals(new BigDecimal("1500"), rows.get(0).get(LeaderboardFields.RATING_VALUE));
  }

  @Test
  void createPatchAndDeleteMapConflictsAndNotFoundStates() throws SQLException {
    LeaderboardService service = new LeaderboardService(jdbcTemplate);
    Map<String, Object> request = validLeaderboardRequest();

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new DataIntegrityViolationException("boom", new RuntimeException("ERROR: duplicate key value")));

    ResponseStatusException createConflict =
        assertThrows(ResponseStatusException.class, () -> service.createLeaderboardEntry(request));
    assertEquals("Leaderboard entry already exists for this user and game.", createConflict.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(
            new UncategorizedSQLException(
                "task", "sql", new SQLException("ERROR: invalid leaderboard payload")));

    ResponseStatusException createBadRequest =
        assertThrows(ResponseStatusException.class, () -> service.createLeaderboardEntry(request));
    assertEquals("invalid leaderboard payload", createBadRequest.getReason());

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockLeaderboardResultSet(12L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq(12L));

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenReturn(0);
    ResponseStatusException patchNotFound =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchLeaderboardEntry("12", Map.of(LeaderboardFields.RANK, "Radiant")));
    assertEquals("Leaderboard entry not found for id 12", patchNotFound.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new DataIntegrityViolationException("boom", new RuntimeException("ERROR: duplicate key value")));
    ResponseStatusException patchConflict =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchLeaderboardEntry("12", Map.of(LeaderboardFields.RANK, "Radiant")));
    assertEquals("Leaderboard entry patch conflicts with existing records.", patchConflict.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(
            new UncategorizedSQLException(
                "task", "sql", new SQLException("ERROR: invalid leaderboard patch payload")));
    ResponseStatusException patchBadRequest =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchLeaderboardEntry("12", Map.of(LeaderboardFields.RANK, "Radiant")));
    assertEquals("invalid leaderboard patch payload", patchBadRequest.getReason());

    when(jdbcTemplate.update(anyString(), eq(12L))).thenReturn(0);
    ResponseStatusException deleteFailure =
        assertThrows(ResponseStatusException.class, () -> service.deleteLeaderboardEntry("12"));
    assertEquals("Leaderboard entry not found for id 12", deleteFailure.getReason());
  }

  @Test
  void createAndPatchLeaderboardSuccessPathsReturnMappedRows() throws SQLException {
    LeaderboardService service = new LeaderboardService(jdbcTemplate);
    Map<String, Object> request = validLeaderboardRequest();

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockLeaderboardResultSet(14L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq(14L));

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenReturn(14L)
        .thenReturn(1);

    Map<String, Object> created = service.createLeaderboardEntry(request);
    Map<String, Object> patched =
        service.patchLeaderboardEntry(
            "14",
            Map.of(
                LeaderboardFields.RANK, "Radiant",
                LeaderboardFields.RATING_LABEL, "",
                LeaderboardFields.UPDATED_AT, "2026-05-13T01:00:00Z"));

    assertEquals("14", created.get(LeaderboardFields.ID));
    assertEquals("14", patched.get(LeaderboardFields.ID));
    assertEquals("VALORANT", patched.get(LeaderboardFields.GAME));
  }

  private Map<String, Object> validLeaderboardRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put(LeaderboardFields.USER_ID, "7");
    request.put(LeaderboardFields.USERNAME, "bluy");
    request.put(LeaderboardFields.GAME, "valorant");
    request.put(LeaderboardFields.RANK, "Immortal");
    request.put(LeaderboardFields.ROLE, "duelist");
    request.put(LeaderboardFields.RATING_VALUE, "1500");
    request.put(LeaderboardFields.RATING_LABEL, "RR");
    request.put(LeaderboardFields.UPDATED_AT, "2026-05-13T00:00:00Z");
    return request;
  }

  private ResultSet mockLeaderboardResultSet(long id) throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    OffsetDateTime updatedAt = OffsetDateTime.parse("2026-05-13T00:00:00Z");
    when(resultSet.getLong("id")).thenReturn(id);
    when(resultSet.getLong("user_id")).thenReturn(7L);
    when(resultSet.getString("username")).thenReturn("bluy");
    when(resultSet.getString("game")).thenReturn("VALORANT");
    when(resultSet.getString("rank")).thenReturn("Immortal");
    when(resultSet.getString("role")).thenReturn("duelist");
    when(resultSet.getBigDecimal("rating_value")).thenReturn(new BigDecimal("1500"));
    when(resultSet.getString("rating_label")).thenReturn("RR");
    when(resultSet.getObject("updated_at", OffsetDateTime.class)).thenReturn(updatedAt);
    return resultSet;
  }
}
