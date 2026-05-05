package certifiedcarry_api.leaderboard.service;

import certifiedcarry_api.shared.GameNameAlias;
import certifiedcarry_api.shared.HttpErrors;
import certifiedcarry_api.shared.HttpRequestParsers;
import certifiedcarry_api.shared.PreparedStatementBinder;
import certifiedcarry_api.shared.SqlErrorMapper;
import certifiedcarry_api.leaderboard.LeaderboardFields;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class LeaderboardService {

  private static final String LEADERBOARD_ENTRY_ID_PATH = "leaderboardEntryId";
  private static final String ADMIN_CREATE_MESSAGE = "Only admins can create leaderboard entries.";
  private static final String ADMIN_UPDATE_MESSAGE = "Only admins can update leaderboard entries.";
  private static final String DELETE_OWN_MESSAGE = "You can only remove your own leaderboard entries.";
  private static final String CREATE_FAILED_MESSAGE = "Failed to create leaderboard entry.";
  private static final String ENTRY_EXISTS_MESSAGE =
      "Leaderboard entry already exists for this user and game.";
  private static final String INVALID_PAYLOAD_MESSAGE = "Invalid leaderboard payload.";
  private static final String PATCH_CONFLICT_MESSAGE =
      "Leaderboard entry patch conflicts with existing records.";
  private static final String INVALID_PATCH_MESSAGE = "Invalid leaderboard patch payload.";
  private static final String ENTRY_NOT_FOUND_PREFIX = "Leaderboard entry not found for id ";
  private static final String LEADERBOARD_SELECT_SQL = """
      SELECT
        id,
        user_id,
        username,
        game,
        rank,
        role,
        rating_value,
        rating_label,
        updated_at
      FROM leaderboard_entries
      """;
  private static final String LEADERBOARD_SELECT_BY_ID_SQL =
      LEADERBOARD_SELECT_SQL + " WHERE id = ?";

  private final JdbcTemplate jdbcTemplate;

  public LeaderboardService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> getLeaderboardEntries() {
    return jdbcTemplate.query(
        LEADERBOARD_SELECT_SQL + " ORDER BY updated_at DESC, id DESC", this::mapLeaderboardEntryRow);
  }

  public List<Map<String, Object>> getLeaderboardEntriesForActor(long actorUserId, boolean isAdmin) {
    return getLeaderboardEntries();
  }

  @Transactional
  public Map<String, Object> createLeaderboardEntryForActor(
      Map<String, Object> request, long actorUserId, boolean isAdmin) {
    if (!isAdmin) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, ADMIN_CREATE_MESSAGE);
    }

    return createLeaderboardEntry(request);
  }

  @Transactional
  public Map<String, Object> patchLeaderboardEntryForActor(
      String leaderboardEntryId, Map<String, Object> request, long actorUserId, boolean isAdmin) {
    if (!isAdmin) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, ADMIN_UPDATE_MESSAGE);
    }

    return patchLeaderboardEntry(leaderboardEntryId, request);
  }

  @Transactional
  public void deleteLeaderboardEntryForActor(
      String leaderboardEntryId, long actorUserId, boolean isAdmin) {
    if (!isAdmin && !isLeaderboardEntryOwnedBy(leaderboardEntryId, actorUserId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, DELETE_OWN_MESSAGE);
    }

    deleteLeaderboardEntry(leaderboardEntryId);
  }

  public boolean isLeaderboardEntryOwnedBy(String leaderboardEntryId, long expectedUserId) {
    long parsedId = HttpRequestParsers.parsePathId(leaderboardEntryId, LEADERBOARD_ENTRY_ID_PATH);
    Integer count = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM leaderboard_entries WHERE id = ? AND user_id = ?",
        Integer.class,
        parsedId,
        expectedUserId);

    return count != null && count > 0;
  }

  @Transactional
  public Map<String, Object> createLeaderboardEntry(Map<String, Object> request) {
    long userId = HttpRequestParsers.requireLong(request.get(LeaderboardFields.USER_ID),
        LeaderboardFields.USER_ID);
    String username = HttpRequestParsers.requireNonBlank(request.get(LeaderboardFields.USERNAME),
        LeaderboardFields.USERNAME);
    String game = GameNameAlias.normalize(HttpRequestParsers.requireNonBlank(
        request.get(LeaderboardFields.GAME), LeaderboardFields.GAME));
    String rank = HttpRequestParsers.requireNonBlank(request.get(LeaderboardFields.RANK),
        LeaderboardFields.RANK);
    String role = HttpRequestParsers.optionalString(request.get(LeaderboardFields.ROLE));
    BigDecimal ratingValue = HttpRequestParsers.optionalBigDecimal(
        request.get(LeaderboardFields.RATING_VALUE), LeaderboardFields.RATING_VALUE);
    String ratingLabel = HttpRequestParsers.defaultIfBlank(
        HttpRequestParsers.optionalString(request.get(LeaderboardFields.RATING_LABEL)),
        LeaderboardFields.DEFAULT_RATING_LABEL);
    OffsetDateTime updatedAt = HttpRequestParsers.optionalOffsetDateTime(
        request.get(LeaderboardFields.UPDATED_AT), LeaderboardFields.UPDATED_AT);

    String sql = """
        INSERT INTO leaderboard_entries (
          user_id,
          username,
          game,
          rank,
          role,
          rating_value,
          rating_label,
          updated_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    try {
      Long createdId = jdbcTemplate.execute(
          (PreparedStatementCreator) connection -> {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            statement.setString(2, username);
            statement.setString(3, game);
            statement.setString(4, rank);
            PreparedStatementBinder.setNullableString(statement, 5, role);
            PreparedStatementBinder.setNullableBigDecimal(statement, 6, ratingValue);
            statement.setString(7, ratingLabel);
            statement.setObject(
                8,
                updatedAt != null ? updatedAt : OffsetDateTime.now(ZoneOffset.UTC));
            return statement;
          },
          (PreparedStatementCallback<Long>) statement -> {
            try (ResultSet resultSet = statement.executeQuery()) {
              if (!resultSet.next()) {
                throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    CREATE_FAILED_MESSAGE);
              }

              return resultSet.getLong(1);
            }
          });

      return findLeaderboardEntryById(createdId);
    } catch (DataIntegrityViolationException exception) {
      throw SqlErrorMapper.mapDataIntegrityViolation(
          exception,
          ENTRY_EXISTS_MESSAGE,
          INVALID_PAYLOAD_MESSAGE,
          HttpErrors::badRequest,
          HttpErrors::conflict);
    } catch (UncategorizedSQLException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessage(
              exception.getSQLException(), INVALID_PAYLOAD_MESSAGE));
    }
  }

  @Transactional
  public Map<String, Object> patchLeaderboardEntry(
      String leaderboardEntryId, Map<String, Object> request) {
    long parsedId = HttpRequestParsers.parsePathId(leaderboardEntryId, LEADERBOARD_ENTRY_ID_PATH);
    LeaderboardRow existing = findLeaderboardEntryRow(parsedId);

    long userId = request.containsKey(LeaderboardFields.USER_ID)
        ? HttpRequestParsers.requireLong(request.get(LeaderboardFields.USER_ID),
            LeaderboardFields.USER_ID)
        : existing.userId();
    String username = request.containsKey(LeaderboardFields.USERNAME)
        ? HttpRequestParsers.requireNonBlank(request.get(LeaderboardFields.USERNAME),
            LeaderboardFields.USERNAME)
        : existing.username();
    String game = request.containsKey(LeaderboardFields.GAME)
        ? GameNameAlias.normalize(HttpRequestParsers.requireNonBlank(
            request.get(LeaderboardFields.GAME), LeaderboardFields.GAME))
        : existing.game();
    String rank = request.containsKey(LeaderboardFields.RANK)
        ? HttpRequestParsers.requireNonBlank(request.get(LeaderboardFields.RANK),
            LeaderboardFields.RANK)
        : existing.rank();
    String role = request.containsKey(LeaderboardFields.ROLE)
        ? HttpRequestParsers.optionalString(request.get(LeaderboardFields.ROLE))
        : existing.role();
    BigDecimal ratingValue = request.containsKey(LeaderboardFields.RATING_VALUE)
        ? HttpRequestParsers.optionalBigDecimal(request.get(LeaderboardFields.RATING_VALUE),
            LeaderboardFields.RATING_VALUE)
        : existing.ratingValue();
    String ratingLabel = request.containsKey(LeaderboardFields.RATING_LABEL)
        ? HttpRequestParsers.defaultIfBlank(
            HttpRequestParsers.optionalString(request.get(LeaderboardFields.RATING_LABEL)),
            LeaderboardFields.DEFAULT_RATING_LABEL)
        : existing.ratingLabel();
    OffsetDateTime updatedAt = request.containsKey(LeaderboardFields.UPDATED_AT)
        ? HttpRequestParsers.optionalOffsetDateTime(request.get(LeaderboardFields.UPDATED_AT),
            LeaderboardFields.UPDATED_AT)
        : OffsetDateTime.now(ZoneOffset.UTC);

    String sql = """
        UPDATE leaderboard_entries
        SET
          user_id = ?,
          username = ?,
          game = ?,
          rank = ?,
          role = ?,
          rating_value = ?,
          rating_label = ?,
          updated_at = ?
        WHERE id = ?
        """;

    try {
      int updatedRows = jdbcTemplate.execute(
          (PreparedStatementCreator) connection -> {
            PreparedStatement statement = connection.prepareStatement(sql);
            statement.setLong(1, userId);
            statement.setString(2, username);
            statement.setString(3, game);
            statement.setString(4, rank);
            PreparedStatementBinder.setNullableString(statement, 5, role);
            PreparedStatementBinder.setNullableBigDecimal(statement, 6, ratingValue);
            statement.setString(7, ratingLabel);
            statement.setObject(
                8,
                updatedAt != null ? updatedAt : OffsetDateTime.now(ZoneOffset.UTC));
            statement.setLong(9, parsedId);
            return statement;
          },
          (PreparedStatementCallback<Integer>) PreparedStatement::executeUpdate);

      if (updatedRows == 0) {
        throw HttpErrors.notFound(ENTRY_NOT_FOUND_PREFIX + leaderboardEntryId);
      }

      return findLeaderboardEntryById(parsedId);
    } catch (DataIntegrityViolationException exception) {
      throw SqlErrorMapper.mapDataIntegrityViolation(
          exception,
          PATCH_CONFLICT_MESSAGE,
          INVALID_PATCH_MESSAGE,
          HttpErrors::badRequest,
          HttpErrors::conflict);
    } catch (UncategorizedSQLException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessage(
              exception.getSQLException(), INVALID_PATCH_MESSAGE));
    }
  }

  @Transactional
  public void deleteLeaderboardEntry(String leaderboardEntryId) {
    long parsedId = HttpRequestParsers.parsePathId(leaderboardEntryId, LEADERBOARD_ENTRY_ID_PATH);
    int deletedRows = jdbcTemplate.update("DELETE FROM leaderboard_entries WHERE id = ?", parsedId);

    if (deletedRows == 0) {
      throw HttpErrors.notFound(ENTRY_NOT_FOUND_PREFIX + leaderboardEntryId);
    }
  }

  private LeaderboardRow findLeaderboardEntryRow(long id) {
    List<LeaderboardRow> rows =
        jdbcTemplate.query(LEADERBOARD_SELECT_BY_ID_SQL, this::mapLeaderboardDataRow, id);
    if (rows.isEmpty()) {
      throw HttpErrors.notFound(ENTRY_NOT_FOUND_PREFIX + id);
    }

    return rows.get(0);
  }

  private Map<String, Object> findLeaderboardEntryById(long id) {
    List<Map<String, Object>> rows =
        jdbcTemplate.query(LEADERBOARD_SELECT_BY_ID_SQL, this::mapLeaderboardEntryRow, id);
    if (rows.isEmpty()) {
      throw HttpErrors.notFound(ENTRY_NOT_FOUND_PREFIX + id);
    }

    return rows.get(0);
  }

  private LeaderboardRow mapLeaderboardDataRow(ResultSet resultSet, int rowNumber)
      throws SQLException {
    return new LeaderboardRow(
        resultSet.getLong("id"),
        resultSet.getLong("user_id"),
        resultSet.getString("username"),
        resultSet.getString("game"),
        resultSet.getString("rank"),
        resultSet.getString("role"),
        resultSet.getBigDecimal("rating_value"),
        resultSet.getString("rating_label"),
        resultSet.getObject("updated_at", OffsetDateTime.class));
  }

  private Map<String, Object> mapLeaderboardEntryRow(ResultSet resultSet, int rowNumber)
      throws SQLException {
    LeaderboardRow data = mapLeaderboardDataRow(resultSet, rowNumber);

    Map<String, Object> row = new LinkedHashMap<>();
    row.put(LeaderboardFields.ID, String.valueOf(data.id()));
    row.put(LeaderboardFields.USER_ID, String.valueOf(data.userId()));
    row.put(LeaderboardFields.USERNAME, data.username());
    row.put(LeaderboardFields.GAME, data.game());
    row.put(LeaderboardFields.RANK, data.rank());
    row.put(LeaderboardFields.ROLE, data.role());
    row.put(LeaderboardFields.RATING_VALUE, data.ratingValue());
    row.put(LeaderboardFields.RATING_LABEL, data.ratingLabel());
    row.put(LeaderboardFields.UPDATED_AT, data.updatedAt());
    return row;
  }

  private record LeaderboardRow(
      long id,
      long userId,
      String username,
      String game,
      String rank,
      String role,
      BigDecimal ratingValue,
      String ratingLabel,
      OffsetDateTime updatedAt) {
  }
}
