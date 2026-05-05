package certifiedcarry_api.queue.service;

import certifiedcarry_api.queue.PendingQueueFields;
import certifiedcarry_api.shared.GameNameAlias;
import certifiedcarry_api.shared.HttpErrors;
import certifiedcarry_api.shared.HttpRequestParsers;
import certifiedcarry_api.shared.PreparedStatementBinder;
import certifiedcarry_api.shared.RequestArrayNormalizer;
import certifiedcarry_api.shared.RequestStatusNormalizer;
import certifiedcarry_api.shared.RocketLeagueModesCodec;
import certifiedcarry_api.shared.SqlErrorMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class PendingRankService {

  private static final Duration DECLINED_RESUBMISSION_COOLDOWN = Duration.ofDays(7);
  private static final String RANK_EDIT_OWN_MESSAGE =
      "You can only edit your own rank submissions.";
  private static final String RANK_DELETE_OWN_MESSAGE =
      "You can only delete your own rank submissions.";
  private static final String PENDING_RANK_NOT_FOUND_PREFIX =
      "Pending rank record not found for id ";
  private static final String PENDING_RANK_CREATE_CONFLICT_MESSAGE =
      "Unable to create pending rank record due to data integrity constraints.";
  private static final String PENDING_RANK_PATCH_CONFLICT_MESSAGE =
      "Unable to patch pending rank record due to data integrity constraints.";
  private static final String FAILED_CREATE_PENDING_RANK_MESSAGE =
      "Failed to create pending rank record.";
  private static final String INVALID_PENDING_RANK_PAYLOAD_MESSAGE =
      "Invalid pending rank payload.";
  private static final String INVALID_PENDING_RANK_PATCH_PAYLOAD_MESSAGE =
      "Invalid pending rank patch payload.";
  private static final String PENDING_RANK_INVALID_STATUS_MESSAGE =
      "status must be one of PENDING, APPROVED, or DECLINED.";
  private static final String COOLDOWN_MESSAGE_PREFIX =
      "You can resubmit rank verification after the 7-day cooldown. Try again in about ";
  private static final String COOLDOWN_MESSAGE_SUFFIX = " hour(s).";

  private static final Set<String> PENDING_RANK_STATUSES =
      Set.of(
          PendingQueueFields.STATUS_PENDING,
          PendingQueueFields.STATUS_APPROVED,
          PendingQueueFields.STATUS_DECLINED);

  private static final String PENDING_RANK_SELECT_SQL = """
    SELECT
    id,
    user_id,
    username,
    full_name,
    game,
    claimed_rank,
    in_game_roles,
    in_game_role,
    rating_value,
    rating_label,
    rocket_league_modes,
    primary_rocket_league_mode,
    proof_image,
    status,
    submitted_at,
    resolved_at,
    decline_reason,
    edited_after_decline,
    edited_at,
    updated_at
    FROM pending_ranks
    """;

  private static final String PENDING_RANK_SELECT_BY_ID_SQL =
      PENDING_RANK_SELECT_SQL + " WHERE id = ?";

  private static final String PENDING_RANK_SELECT_BY_USER_ID_SQL =
      PENDING_RANK_SELECT_SQL + " WHERE user_id = ?";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  public PendingRankService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  public List<Map<String, Object>> getPendingRanks(String status) {
    String normalizedStatus = normalizePendingRankStatus(status, false);

    if (normalizedStatus == null) {
      return jdbcTemplate.query(
          PENDING_RANK_SELECT_SQL + " ORDER BY submitted_at DESC, id DESC", this::mapPendingRankRow);
    }

    return jdbcTemplate.query(
        PENDING_RANK_SELECT_SQL
            + " WHERE status = CAST(? AS pending_rank_status_enum) ORDER BY submitted_at DESC, id DESC",
        this::mapPendingRankRow,
        normalizedStatus);
  }

  public List<Map<String, Object>> getPendingRanksForActor(
      String status, long actorUserId, boolean isAdmin) {
    return isAdmin ? getPendingRanks(status) : getPendingRanksForUser(status, actorUserId);
  }

  public List<Map<String, Object>> getPendingRanksForUser(String status, long userId) {
    String normalizedStatus = normalizePendingRankStatus(status, false);

    if (normalizedStatus == null) {
      return jdbcTemplate.query(
          PENDING_RANK_SELECT_BY_USER_ID_SQL + " ORDER BY submitted_at DESC, id DESC",
          this::mapPendingRankRow,
          userId);
    }

    return jdbcTemplate.query(
        PENDING_RANK_SELECT_BY_USER_ID_SQL
            + " AND status = CAST(? AS pending_rank_status_enum) ORDER BY submitted_at DESC, id DESC",
        this::mapPendingRankRow,
        userId,
        normalizedStatus);
  }

  public OffsetDateTime getLatestDeclinedTimestampForUser(long userId) {
    String sql = """
        SELECT COALESCE(edited_at, resolved_at, updated_at, submitted_at) AS latest_declined_at
        FROM pending_ranks
        WHERE user_id = ?
          AND status = 'DECLINED'
        ORDER BY COALESCE(edited_at, resolved_at, updated_at, submitted_at) DESC
        LIMIT 1
        """;

    List<OffsetDateTime> rows =
        jdbcTemplate.query(
            sql,
            (resultSet, rowNumber) ->
                resultSet.getObject("latest_declined_at", OffsetDateTime.class),
            userId);

    return rows.isEmpty() ? null : rows.get(0);
  }

  public void enforceDeclinedResubmissionCooldown(long actorUserId) {
    OffsetDateTime latestDeclinedAt = getLatestDeclinedTimestampForUser(actorUserId);
    if (latestDeclinedAt == null) {
      return;
    }

    OffsetDateTime availableAt = latestDeclinedAt.plus(DECLINED_RESUBMISSION_COOLDOWN);
    OffsetDateTime now = OffsetDateTime.now();
    if (!now.isBefore(availableAt)) {
      return;
    }

    Duration remaining = Duration.between(now, availableAt);
    long remainingHours = Math.max(1, (remaining.toMinutes() + 59) / 60);
    throw new ResponseStatusException(
        HttpStatus.TOO_MANY_REQUESTS,
        COOLDOWN_MESSAGE_PREFIX + remainingHours + COOLDOWN_MESSAGE_SUFFIX);
  }

  public boolean isPendingRankOwnedBy(String pendingRankId, long expectedUserId) {
    long parsedId = HttpRequestParsers.parsePathId(pendingRankId, "pendingRankId");
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM pending_ranks WHERE id = ? AND user_id = ?",
            Integer.class,
            parsedId,
            expectedUserId);

    return count != null && count > 0;
  }

  @Transactional
  public Map<String, Object> createPendingRankForActor(
      Map<String, Object> request, long actorUserId, boolean isAdmin) {
    Map<String, Object> mutableRequest = new LinkedHashMap<>(request);
    if (!isAdmin) {
      PendingQueueActorSupport.enforcePayloadUserOwnership(mutableRequest, actorUserId);
      enforceDeclinedResubmissionCooldown(actorUserId);
    }

    return createPendingRank(mutableRequest);
  }

  @Transactional
  public Map<String, Object> patchPendingRankForActor(
      String pendingRankId, Map<String, Object> request, long actorUserId, boolean isAdmin) {
    if (!isAdmin && !isPendingRankOwnedBy(pendingRankId, actorUserId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, RANK_EDIT_OWN_MESSAGE);
    }

    Map<String, Object> mutableRequest = new LinkedHashMap<>(request);
    if (!isAdmin) {
      PendingQueueActorSupport.enforcePayloadUserOwnership(mutableRequest, actorUserId);
      enforceDeclinedResubmissionCooldown(actorUserId);
    }

    return patchPendingRank(pendingRankId, mutableRequest);
  }

  @Transactional
  public void deletePendingRankForActor(String pendingRankId, long actorUserId, boolean isAdmin) {
    if (!isAdmin && !isPendingRankOwnedBy(pendingRankId, actorUserId)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, RANK_DELETE_OWN_MESSAGE);
    }

    deletePendingRank(pendingRankId);
  }

  @Transactional
  public Map<String, Object> createPendingRank(Map<String, Object> request) {
    Long userId =
        HttpRequestParsers.requireLong(request.get(PendingQueueFields.USER_ID), PendingQueueFields.USER_ID);
    String username =
        HttpRequestParsers.requireNonBlank(request.get(PendingQueueFields.USERNAME), PendingQueueFields.USERNAME);
    String fullName =
        HttpRequestParsers.requireNonBlank(request.get(PendingQueueFields.FULL_NAME), PendingQueueFields.FULL_NAME);
    String game =
        GameNameAlias.normalize(
            HttpRequestParsers.requireNonBlank(
                request.get(PendingQueueFields.GAME), PendingQueueFields.GAME));
    String claimedRank =
        HttpRequestParsers.requireNonBlank(
            request.get(PendingQueueFields.CLAIMED_RANK), PendingQueueFields.CLAIMED_RANK);
    List<String> inGameRoles =
        RequestArrayNormalizer.normalizeStringArray(
            request.get(PendingQueueFields.IN_GAME_ROLES), PendingQueueFields.IN_GAME_ROLES);
    String inGameRole =
        HttpRequestParsers.optionalString(request.get(PendingQueueFields.IN_GAME_ROLE));
    BigDecimal ratingValue =
        HttpRequestParsers.optionalBigDecimal(
            request.get(PendingQueueFields.RATING_VALUE), PendingQueueFields.RATING_VALUE);
    String ratingLabel =
        HttpRequestParsers.defaultIfBlank(
            HttpRequestParsers.optionalString(request.get(PendingQueueFields.RATING_LABEL)),
            PendingQueueFields.DEFAULT_RATING_LABEL);
    List<String> rocketLeagueModes =
        RocketLeagueModesCodec.encodeRocketLeagueModes(
            request.get(PendingQueueFields.ROCKET_LEAGUE_MODES), objectMapper);
    String primaryRocketLeagueMode =
        HttpRequestParsers.optionalString(request.get(PendingQueueFields.PRIMARY_ROCKET_LEAGUE_MODE));
    String proofImage =
        HttpRequestParsers.defaultIfBlank(
            HttpRequestParsers.optionalString(request.get(PendingQueueFields.PROOF_IMAGE)),
            PendingQueueFields.DEFAULT_EMPTY_TEXT);
    String status =
        normalizePendingRankStatus(
            HttpRequestParsers.optionalString(request.get(PendingQueueFields.STATUS)), true);
    OffsetDateTime submittedAt =
        HttpRequestParsers.optionalOffsetDateTime(
            request.get(PendingQueueFields.SUBMITTED_AT), PendingQueueFields.SUBMITTED_AT);
    OffsetDateTime resolvedAt =
        HttpRequestParsers.optionalOffsetDateTime(
            request.get(PendingQueueFields.RESOLVED_AT), PendingQueueFields.RESOLVED_AT);
    String declineReason =
        HttpRequestParsers.optionalString(request.get(PendingQueueFields.DECLINE_REASON));
    boolean editedAfterDecline =
        HttpRequestParsers.optionalBoolean(
            request.get(PendingQueueFields.EDITED_AFTER_DECLINE),
            PendingQueueFields.EDITED_AFTER_DECLINE,
            false);
    OffsetDateTime editedAt =
        HttpRequestParsers.optionalOffsetDateTime(
            request.get(PendingQueueFields.EDITED_AT), PendingQueueFields.EDITED_AT);

    String sql = """
        INSERT INTO pending_ranks (
          user_id,
          username,
          full_name,
          game,
          claimed_rank,
          in_game_roles,
          in_game_role,
          rating_value,
          rating_label,
          rocket_league_modes,
          primary_rocket_league_mode,
          proof_image,
          status,
          submitted_at,
          resolved_at,
          decline_reason,
          edited_after_decline,
          edited_at
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS pending_rank_status_enum), ?, ?, ?, ?, ?)
        RETURNING id
        """;

    try {
      Long createdId =
          jdbcTemplate.execute(
              (PreparedStatementCreator)
                  connection -> {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setLong(1, userId);
                    statement.setString(2, username);
                    statement.setString(3, fullName);
                    statement.setString(4, game);
                    statement.setString(5, claimedRank);
                    statement.setArray(
                        6, connection.createArrayOf("text", inGameRoles.toArray(String[]::new)));
                    PreparedStatementBinder.setNullableString(statement, 7, inGameRole);
                    PreparedStatementBinder.setNullableBigDecimal(statement, 8, ratingValue);
                    statement.setString(9, ratingLabel);
                    statement.setArray(
                        10, connection.createArrayOf("text", rocketLeagueModes.toArray(String[]::new)));
                    PreparedStatementBinder.setNullableString(statement, 11, primaryRocketLeagueMode);
                    statement.setString(12, proofImage);
                    statement.setString(13, status);
                    statement.setObject(
                        14, submittedAt != null ? submittedAt : OffsetDateTime.now(ZoneOffset.UTC));
                    PreparedStatementBinder.setNullableOffsetDateTime(statement, 15, resolvedAt);
                    PreparedStatementBinder.setNullableString(statement, 16, declineReason);
                    statement.setBoolean(17, editedAfterDecline);
                    PreparedStatementBinder.setNullableOffsetDateTime(statement, 18, editedAt);
                    return statement;
                  },
              (PreparedStatementCallback<Long>)
                  statement -> {
                    try (ResultSet resultSet = statement.executeQuery()) {
                      if (!resultSet.next()) {
                        throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, FAILED_CREATE_PENDING_RANK_MESSAGE);
                      }

                      return resultSet.getLong(1);
                    }
                  });

      return findPendingRankById(createdId);
    } catch (DataIntegrityViolationException exception) {
      throw HttpErrors.conflict(PENDING_RANK_CREATE_CONFLICT_MESSAGE);
    } catch (UncategorizedSQLException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessagePreservingDetail(
              exception.getSQLException(), INVALID_PENDING_RANK_PAYLOAD_MESSAGE));
    }
  }

  @Transactional
  public Map<String, Object> patchPendingRank(String pendingRankId, Map<String, Object> request) {
    long parsedId = HttpRequestParsers.parsePathId(pendingRankId, "pendingRankId");
    PendingRankRow existing = findPendingRankRow(parsedId);

    Long userId =
        request.containsKey(PendingQueueFields.USER_ID)
            ? HttpRequestParsers.requireLong(request.get(PendingQueueFields.USER_ID), PendingQueueFields.USER_ID)
            : existing.userId();
    String username =
        request.containsKey(PendingQueueFields.USERNAME)
            ? HttpRequestParsers.requireNonBlank(
                request.get(PendingQueueFields.USERNAME), PendingQueueFields.USERNAME)
            : existing.username();
    String fullName =
        request.containsKey(PendingQueueFields.FULL_NAME)
            ? HttpRequestParsers.requireNonBlank(
                request.get(PendingQueueFields.FULL_NAME), PendingQueueFields.FULL_NAME)
            : existing.fullName();
    String game =
        request.containsKey(PendingQueueFields.GAME)
            ? GameNameAlias.normalize(
                HttpRequestParsers.requireNonBlank(
                    request.get(PendingQueueFields.GAME), PendingQueueFields.GAME))
            : existing.game();
    String claimedRank =
        request.containsKey(PendingQueueFields.CLAIMED_RANK)
            ? HttpRequestParsers.requireNonBlank(
                request.get(PendingQueueFields.CLAIMED_RANK), PendingQueueFields.CLAIMED_RANK)
            : existing.claimedRank();
    List<String> inGameRoles =
        request.containsKey(PendingQueueFields.IN_GAME_ROLES)
            ? RequestArrayNormalizer.normalizeStringArray(
                request.get(PendingQueueFields.IN_GAME_ROLES), PendingQueueFields.IN_GAME_ROLES)
            : existing.inGameRoles();
    String inGameRole =
        request.containsKey(PendingQueueFields.IN_GAME_ROLE)
            ? HttpRequestParsers.optionalString(request.get(PendingQueueFields.IN_GAME_ROLE))
            : existing.inGameRole();
    BigDecimal ratingValue =
        request.containsKey(PendingQueueFields.RATING_VALUE)
            ? HttpRequestParsers.optionalBigDecimal(
                request.get(PendingQueueFields.RATING_VALUE), PendingQueueFields.RATING_VALUE)
            : existing.ratingValue();
    String ratingLabel =
        request.containsKey(PendingQueueFields.RATING_LABEL)
            ? HttpRequestParsers.requireNonBlank(
                request.get(PendingQueueFields.RATING_LABEL), PendingQueueFields.RATING_LABEL)
            : existing.ratingLabel();
    List<String> rocketLeagueModes =
        request.containsKey(PendingQueueFields.ROCKET_LEAGUE_MODES)
            ? RocketLeagueModesCodec.encodeRocketLeagueModes(
                request.get(PendingQueueFields.ROCKET_LEAGUE_MODES), objectMapper)
            : existing.rocketLeagueModes();
    String primaryRocketLeagueMode =
        request.containsKey(PendingQueueFields.PRIMARY_ROCKET_LEAGUE_MODE)
            ? HttpRequestParsers.optionalString(request.get(PendingQueueFields.PRIMARY_ROCKET_LEAGUE_MODE))
            : existing.primaryRocketLeagueMode();
    String proofImage =
        request.containsKey(PendingQueueFields.PROOF_IMAGE)
            ? HttpRequestParsers.defaultIfBlank(
                HttpRequestParsers.optionalString(request.get(PendingQueueFields.PROOF_IMAGE)),
                PendingQueueFields.DEFAULT_EMPTY_TEXT)
            : existing.proofImage();
    String status =
        request.containsKey(PendingQueueFields.STATUS)
            ? normalizePendingRankStatus(
                HttpRequestParsers.optionalString(request.get(PendingQueueFields.STATUS)), true)
            : existing.status();
    OffsetDateTime submittedAt =
        request.containsKey(PendingQueueFields.SUBMITTED_AT)
            ? HttpRequestParsers.optionalOffsetDateTime(
                request.get(PendingQueueFields.SUBMITTED_AT), PendingQueueFields.SUBMITTED_AT)
            : existing.submittedAt();
    OffsetDateTime resolvedAt =
        request.containsKey(PendingQueueFields.RESOLVED_AT)
            ? HttpRequestParsers.optionalOffsetDateTime(
                request.get(PendingQueueFields.RESOLVED_AT), PendingQueueFields.RESOLVED_AT)
            : existing.resolvedAt();
    String declineReason =
        request.containsKey(PendingQueueFields.DECLINE_REASON)
            ? HttpRequestParsers.optionalString(request.get(PendingQueueFields.DECLINE_REASON))
            : existing.declineReason();
    boolean editedAfterDecline =
        request.containsKey(PendingQueueFields.EDITED_AFTER_DECLINE)
            ? HttpRequestParsers.optionalBoolean(
                request.get(PendingQueueFields.EDITED_AFTER_DECLINE),
                PendingQueueFields.EDITED_AFTER_DECLINE,
                false)
            : existing.editedAfterDecline();
    OffsetDateTime editedAt =
        request.containsKey(PendingQueueFields.EDITED_AT)
            ? HttpRequestParsers.optionalOffsetDateTime(
                request.get(PendingQueueFields.EDITED_AT), PendingQueueFields.EDITED_AT)
            : existing.editedAt();

    String sql = """
        UPDATE pending_ranks
        SET
          user_id = ?,
          username = ?,
          full_name = ?,
          game = ?,
          claimed_rank = ?,
          in_game_roles = ?,
          in_game_role = ?,
          rating_value = ?,
          rating_label = ?,
          rocket_league_modes = ?,
          primary_rocket_league_mode = ?,
          proof_image = ?,
          status = CAST(? AS pending_rank_status_enum),
          submitted_at = ?,
          resolved_at = ?,
          decline_reason = ?,
          edited_after_decline = ?,
          edited_at = ?,
          updated_at = now()
        WHERE id = ?
        """;

    try {
      int updatedRows =
          jdbcTemplate.execute(
              (PreparedStatementCreator)
                  connection -> {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setLong(1, userId);
                    statement.setString(2, username);
                    statement.setString(3, fullName);
                    statement.setString(4, game);
                    statement.setString(5, claimedRank);
                    statement.setArray(
                        6, connection.createArrayOf("text", inGameRoles.toArray(String[]::new)));
                    PreparedStatementBinder.setNullableString(statement, 7, inGameRole);
                    PreparedStatementBinder.setNullableBigDecimal(statement, 8, ratingValue);
                    statement.setString(9, ratingLabel);
                    statement.setArray(
                        10, connection.createArrayOf("text", rocketLeagueModes.toArray(String[]::new)));
                    PreparedStatementBinder.setNullableString(statement, 11, primaryRocketLeagueMode);
                    statement.setString(12, proofImage);
                    statement.setString(13, status);
                    PreparedStatementBinder.setNullableOffsetDateTime(statement, 14, submittedAt);
                    PreparedStatementBinder.setNullableOffsetDateTime(statement, 15, resolvedAt);
                    PreparedStatementBinder.setNullableString(statement, 16, declineReason);
                    statement.setBoolean(17, editedAfterDecline);
                    PreparedStatementBinder.setNullableOffsetDateTime(statement, 18, editedAt);
                    statement.setLong(19, parsedId);
                    return statement;
                  },
              (PreparedStatementCallback<Integer>) PreparedStatement::executeUpdate);

      if (updatedRows == 0) {
        throw HttpErrors.notFound(PENDING_RANK_NOT_FOUND_PREFIX + pendingRankId);
      }

      return findPendingRankById(parsedId);
    } catch (DataIntegrityViolationException exception) {
      throw HttpErrors.conflict(PENDING_RANK_PATCH_CONFLICT_MESSAGE);
    } catch (UncategorizedSQLException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessagePreservingDetail(
              exception.getSQLException(), INVALID_PENDING_RANK_PATCH_PAYLOAD_MESSAGE));
    }
  }

  @Transactional
  public void deletePendingRank(String pendingRankId) {
    long parsedId = HttpRequestParsers.parsePathId(pendingRankId, "pendingRankId");
    int deletedRows = jdbcTemplate.update("DELETE FROM pending_ranks WHERE id = ?", parsedId);

    if (deletedRows == 0) {
      throw HttpErrors.notFound(PENDING_RANK_NOT_FOUND_PREFIX + pendingRankId);
    }
  }

  private PendingRankRow findPendingRankRow(long id) {
    List<PendingRankRow> rows =
        jdbcTemplate.query(PENDING_RANK_SELECT_BY_ID_SQL, this::mapPendingRankDataRow, id);
    if (rows.isEmpty()) {
      throw HttpErrors.notFound(PENDING_RANK_NOT_FOUND_PREFIX + id);
    }

    return rows.get(0);
  }

  private Map<String, Object> findPendingRankById(long id) {
    List<Map<String, Object>> rows =
        jdbcTemplate.query(PENDING_RANK_SELECT_BY_ID_SQL, this::mapPendingRankRow, id);
    if (rows.isEmpty()) {
      throw HttpErrors.notFound(PENDING_RANK_NOT_FOUND_PREFIX + id);
    }

    return rows.get(0);
  }

  private PendingRankRow mapPendingRankDataRow(ResultSet resultSet, int rowNumber) throws SQLException {
    return new PendingRankRow(
        resultSet.getLong("id"),
        resultSet.getLong("user_id"),
        resultSet.getString("username"),
        resultSet.getString("full_name"),
        resultSet.getString("game"),
        resultSet.getString("claimed_rank"),
        RocketLeagueModesCodec.readTextArray(resultSet, "in_game_roles"),
        resultSet.getString("in_game_role"),
        resultSet.getBigDecimal("rating_value"),
        resultSet.getString("rating_label"),
        RocketLeagueModesCodec.readTextArray(resultSet, "rocket_league_modes"),
        resultSet.getString("primary_rocket_league_mode"),
        resultSet.getString("proof_image"),
        resultSet.getString("status"),
        resultSet.getObject("submitted_at", OffsetDateTime.class),
        resultSet.getObject("resolved_at", OffsetDateTime.class),
        resultSet.getString("decline_reason"),
        resultSet.getBoolean("edited_after_decline"),
        resultSet.getObject("edited_at", OffsetDateTime.class),
        resultSet.getObject("updated_at", OffsetDateTime.class));
  }

  private Map<String, Object> mapPendingRankRow(ResultSet resultSet, int rowNumber) throws SQLException {
    PendingRankRow data = mapPendingRankDataRow(resultSet, rowNumber);

    Map<String, Object> row = new LinkedHashMap<>();
    row.put(PendingQueueFields.ID, String.valueOf(data.id()));
    row.put(PendingQueueFields.USER_ID, String.valueOf(data.userId()));
    row.put(PendingQueueFields.USERNAME, data.username());
    row.put(PendingQueueFields.FULL_NAME, data.fullName());
    row.put(PendingQueueFields.GAME, data.game());
    row.put(PendingQueueFields.CLAIMED_RANK, data.claimedRank());
    row.put(PendingQueueFields.IN_GAME_ROLES, data.inGameRoles());
    row.put(PendingQueueFields.IN_GAME_ROLE, data.inGameRole());
    row.put(PendingQueueFields.RATING_VALUE, data.ratingValue());
    row.put(PendingQueueFields.RATING_LABEL, data.ratingLabel());
    row.put(
        PendingQueueFields.ROCKET_LEAGUE_MODES,
        RocketLeagueModesCodec.decodeRocketLeagueModes(data.rocketLeagueModes(), objectMapper));
    row.put(PendingQueueFields.PRIMARY_ROCKET_LEAGUE_MODE, data.primaryRocketLeagueMode());
    row.put(PendingQueueFields.PROOF_IMAGE, data.proofImage());
    row.put(PendingQueueFields.STATUS, data.status());
    row.put(PendingQueueFields.SUBMITTED_AT, data.submittedAt());
    row.put(PendingQueueFields.RESOLVED_AT, data.resolvedAt());
    row.put(PendingQueueFields.DECLINE_REASON, data.declineReason());
    row.put(PendingQueueFields.EDITED_AFTER_DECLINE, data.editedAfterDecline());
    row.put(PendingQueueFields.EDITED_AT, data.editedAt());
    row.put(PendingQueueFields.UPDATED_AT, data.updatedAt());
    return row;
  }

  private String normalizePendingRankStatus(String value, boolean defaultToPending) {
    return RequestStatusNormalizer.normalize(
        value,
        defaultToPending,
        PendingQueueFields.STATUS_PENDING,
        null,
        PENDING_RANK_STATUSES,
        PENDING_RANK_INVALID_STATUS_MESSAGE,
        HttpErrors::badRequest);
  }

  private record PendingRankRow(
      long id,
      long userId,
      String username,
      String fullName,
      String game,
      String claimedRank,
      List<String> inGameRoles,
      String inGameRole,
      BigDecimal ratingValue,
      String ratingLabel,
      List<String> rocketLeagueModes,
      String primaryRocketLeagueMode,
      String proofImage,
      String status,
      OffsetDateTime submittedAt,
      OffsetDateTime resolvedAt,
      String declineReason,
      boolean editedAfterDecline,
      OffsetDateTime editedAt,
      OffsetDateTime updatedAt) {}
}
