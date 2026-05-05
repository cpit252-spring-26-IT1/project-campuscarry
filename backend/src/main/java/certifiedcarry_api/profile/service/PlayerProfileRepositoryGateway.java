package certifiedcarry_api.profile.service;

import certifiedcarry_api.profile.PlayerProfileFields;
import certifiedcarry_api.shared.HttpErrors;
import certifiedcarry_api.shared.HttpRequestParsers;
import certifiedcarry_api.shared.PreparedStatementBinder;
import certifiedcarry_api.shared.RocketLeagueModesCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.web.server.ResponseStatusException;

final class PlayerProfileRepositoryGateway {

  private static final String PROFILE_SELECT = """
      SELECT
        id,
        user_id,
        username,
        profile_image,
        game,
        rank,
        allow_player_chats,
        is_with_team,
        team_name,
        rocket_league_modes,
        primary_rocket_league_mode,
        in_game_roles,
        in_game_role,
        rating_value,
        rating_label,
        proof_image,
        bio,
        clips_url,
        rank_verification_status,
        decline_reason,
        declined_at,
        is_verified,
        submitted_at,
        rank_verified_at,
        rank_expires_at,
        rank_expiry_reminder_sent_at,
        created_at,
        updated_at
      FROM player_profiles
      """;

  private static final String PROFILE_NOT_FOUND_PREFIX = "Player profile not found for id ";

  private static final String PROFILE_INSERT_SQL = """
      INSERT INTO player_profiles (
        user_id,
        username,
        profile_image,
        game,
        rank,
        allow_player_chats,
        is_with_team,
        team_name,
        rocket_league_modes,
        primary_rocket_league_mode,
        in_game_roles,
        in_game_role,
        rating_value,
        rating_label,
        proof_image,
        bio,
        clips_url,
        rank_verification_status,
        decline_reason,
        declined_at,
        is_verified,
        submitted_at,
        rank_verified_at,
        rank_expires_at,
        rank_expiry_reminder_sent_at,
        updated_at
      )
      VALUES (
        ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
        CAST(? AS rank_verification_status_enum), ?, ?, ?, ?, ?, ?, ?, ?
      )
      RETURNING id
      """;

  private static final String PROFILE_UPDATE_SQL = """
      UPDATE player_profiles
      SET
        user_id = ?,
        username = ?,
        profile_image = ?,
        game = ?,
        rank = ?,
        allow_player_chats = ?,
        is_with_team = ?,
        team_name = ?,
        rocket_league_modes = ?,
        primary_rocket_league_mode = ?,
        in_game_roles = ?,
        in_game_role = ?,
        rating_value = ?,
        rating_label = ?,
        proof_image = ?,
        bio = ?,
        clips_url = ?,
        rank_verification_status = CAST(? AS rank_verification_status_enum),
        decline_reason = ?,
        declined_at = ?,
        is_verified = ?,
        submitted_at = ?,
        rank_verified_at = ?,
        rank_expires_at = ?,
        rank_expiry_reminder_sent_at = ?,
        updated_at = ?
      WHERE id = ?
      """;

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  PlayerProfileRepositoryGateway(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
    this.jdbcTemplate = jdbcTemplate;
    this.objectMapper = objectMapper;
  }

  List<Map<String, Object>> getPlayerProfiles(String userId) {
    if (userId == null || userId.isBlank()) {
      return jdbcTemplate.query(PROFILE_SELECT + " ORDER BY id ASC", this::mapPlayerProfileRow);
    }

    long parsedUserId = HttpRequestParsers.parsePathId(userId, PlayerProfileFields.USER_ID);
    return jdbcTemplate.query(
        PROFILE_SELECT + " WHERE user_id = ? ORDER BY id ASC", this::mapPlayerProfileRow, parsedUserId);
  }

  boolean isProfileOwnedBy(String profileId, long expectedUserId) {
    long parsedId = HttpRequestParsers.parsePathId(profileId, "profileId");
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM player_profiles WHERE id = ? AND user_id = ?",
            Integer.class,
            parsedId,
            expectedUserId);

    return count != null && count > 0;
  }

  PlayerProfileRow findPlayerProfileRow(long id) {
    List<PlayerProfileRow> rows =
        jdbcTemplate.query(PROFILE_SELECT + " WHERE id = ?", this::mapPlayerProfileDataRow, id);
    if (rows.isEmpty()) {
      throw HttpErrors.notFound(PROFILE_NOT_FOUND_PREFIX + id);
    }

    return rows.get(0);
  }

  Map<String, Object> findPlayerProfileById(long id) {
    List<Map<String, Object>> rows =
        jdbcTemplate.query(PROFILE_SELECT + " WHERE id = ?", this::mapPlayerProfileRow, id);
    if (rows.isEmpty()) {
      throw HttpErrors.notFound(PROFILE_NOT_FOUND_PREFIX + id);
    }

    return rows.get(0);
  }

  Long insertProfile(PlayerProfilePayload payload) {
    return jdbcTemplate.execute(
        (PreparedStatementCreator)
            connection -> {
              PreparedStatement statement = connection.prepareStatement(PROFILE_INSERT_SQL);
              bindProfileStatement(statement, connection, payload);
              return statement;
            },
        (PreparedStatementCallback<Long>)
            statement -> {
              try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                  throw new ResponseStatusException(
                      HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create player profile.");
                }

                return resultSet.getLong(1);
              }
            });
  }

  int updateProfile(long profileId, PlayerProfilePayload payload) {
    return jdbcTemplate.execute(
        (PreparedStatementCreator)
            connection -> {
              PreparedStatement statement = connection.prepareStatement(PROFILE_UPDATE_SQL);
              bindProfileStatement(statement, connection, payload);
              statement.setLong(27, profileId);
              return statement;
            },
        (PreparedStatementCallback<Integer>) PreparedStatement::executeUpdate);
  }

  private void bindProfileStatement(
      PreparedStatement statement,
      java.sql.Connection connection,
      PlayerProfilePayload payload)
      throws SQLException {
    statement.setLong(1, payload.userId());
    statement.setString(2, payload.username());
    statement.setString(3, payload.profileImage());
    PreparedStatementBinder.setNullableString(statement, 4, payload.game());
    PreparedStatementBinder.setNullableString(statement, 5, payload.rank());
    statement.setBoolean(6, payload.allowPlayerChats());
    statement.setBoolean(7, payload.isWithTeam());
    PreparedStatementBinder.setNullableString(statement, 8, payload.teamName());
    statement.setArray(
        9, connection.createArrayOf("text", payload.rocketLeagueModes().toArray(String[]::new)));
    PreparedStatementBinder.setNullableString(statement, 10, payload.primaryRocketLeagueMode());
    statement.setArray(
        11, connection.createArrayOf("text", payload.inGameRoles().toArray(String[]::new)));
    PreparedStatementBinder.setNullableString(statement, 12, payload.inGameRole());
    PreparedStatementBinder.setNullableBigDecimal(statement, 13, payload.ratingValue());
    statement.setString(14, payload.ratingLabel());
    statement.setString(15, payload.proofImage());
    statement.setString(16, payload.bio());
    statement.setString(17, payload.clipsUrl());
    statement.setString(18, payload.rankVerificationStatus());
    statement.setString(19, payload.declineReason());
    PreparedStatementBinder.setNullableOffsetDateTime(statement, 20, payload.declinedAt());
    statement.setBoolean(21, payload.isVerified());
    PreparedStatementBinder.setNullableOffsetDateTime(statement, 22, payload.submittedAt());
    PreparedStatementBinder.setNullableOffsetDateTime(statement, 23, payload.rankVerifiedAt());
    PreparedStatementBinder.setNullableOffsetDateTime(statement, 24, payload.rankExpiresAt());
    PreparedStatementBinder.setNullableOffsetDateTime(
        statement, 25, payload.rankExpiryReminderSentAt());
    statement.setObject(
        26, payload.updatedAt() != null ? payload.updatedAt() : OffsetDateTime.now(ZoneOffset.UTC));
  }

  private PlayerProfileRow mapPlayerProfileDataRow(ResultSet resultSet, int rowNumber)
      throws SQLException {
    return new PlayerProfileRow(
        resultSet.getLong("id"),
        resultSet.getLong("user_id"),
        resultSet.getString("username"),
        resultSet.getString("profile_image"),
        resultSet.getString("game"),
        resultSet.getString("rank"),
        resultSet.getBoolean("allow_player_chats"),
        resultSet.getBoolean("is_with_team"),
        resultSet.getString("team_name"),
        RocketLeagueModesCodec.readTextArray(resultSet, "rocket_league_modes"),
        resultSet.getString("primary_rocket_league_mode"),
        RocketLeagueModesCodec.readTextArray(resultSet, "in_game_roles"),
        resultSet.getString("in_game_role"),
        resultSet.getBigDecimal("rating_value"),
        resultSet.getString("rating_label"),
        resultSet.getString("proof_image"),
        resultSet.getString("bio"),
        resultSet.getString("clips_url"),
        resultSet.getString("rank_verification_status"),
        resultSet.getString("decline_reason"),
        resultSet.getObject("declined_at", OffsetDateTime.class),
        resultSet.getBoolean("is_verified"),
        resultSet.getObject("submitted_at", OffsetDateTime.class),
        resultSet.getObject("rank_verified_at", OffsetDateTime.class),
        resultSet.getObject("rank_expires_at", OffsetDateTime.class),
        resultSet.getObject("rank_expiry_reminder_sent_at", OffsetDateTime.class),
        resultSet.getObject("created_at", OffsetDateTime.class),
        resultSet.getObject("updated_at", OffsetDateTime.class));
  }

  private Map<String, Object> mapPlayerProfileRow(ResultSet resultSet, int rowNumber)
      throws SQLException {
    PlayerProfileRow data = mapPlayerProfileDataRow(resultSet, rowNumber);

    Map<String, Object> row = new LinkedHashMap<>();
    row.put(PlayerProfileFields.ID, String.valueOf(data.id()));
    row.put(PlayerProfileFields.USER_ID, String.valueOf(data.userId()));
    row.put(PlayerProfileFields.USERNAME, data.username());
    row.put(PlayerProfileFields.PROFILE_IMAGE, data.profileImage());
    row.put(PlayerProfileFields.GAME, data.game());
    row.put(PlayerProfileFields.RANK, data.rank());
    row.put(PlayerProfileFields.ALLOW_PLAYER_CHATS, data.allowPlayerChats());
    row.put(PlayerProfileFields.IS_WITH_TEAM, data.isWithTeam());
    row.put(PlayerProfileFields.TEAM_NAME, data.teamName());
    row.put(
        PlayerProfileFields.ROCKET_LEAGUE_MODES,
        RocketLeagueModesCodec.decodeRocketLeagueModes(data.rocketLeagueModes(), objectMapper));
    row.put(PlayerProfileFields.PRIMARY_ROCKET_LEAGUE_MODE, data.primaryRocketLeagueMode());
    row.put(PlayerProfileFields.IN_GAME_ROLES, data.inGameRoles());
    row.put(PlayerProfileFields.IN_GAME_ROLE, data.inGameRole());
    row.put(PlayerProfileFields.RATING_VALUE, data.ratingValue());
    row.put(PlayerProfileFields.RATING_LABEL, data.ratingLabel());
    row.put(PlayerProfileFields.PROOF_IMAGE, data.proofImage());
    row.put(PlayerProfileFields.BIO, data.bio());
    row.put(PlayerProfileFields.CLIPS_URL, data.clipsUrl());
    row.put(PlayerProfileFields.RANK_VERIFICATION_STATUS, data.rankVerificationStatus());
    row.put(PlayerProfileFields.DECLINE_REASON, data.declineReason());
    row.put(PlayerProfileFields.DECLINED_AT, data.declinedAt());
    row.put(PlayerProfileFields.IS_VERIFIED, data.isVerified());
    row.put(PlayerProfileFields.SUBMITTED_AT, data.submittedAt());
    row.put(PlayerProfileFields.RANK_VERIFIED_AT, data.rankVerifiedAt());
    row.put(PlayerProfileFields.RANK_EXPIRES_AT, data.rankExpiresAt());
    row.put(
        PlayerProfileFields.RANK_EXPIRY_REMINDER_SENT_AT, data.rankExpiryReminderSentAt());
    row.put(PlayerProfileFields.CREATED_AT, data.createdAt());
    row.put(PlayerProfileFields.UPDATED_AT, data.updatedAt());
    return row;
  }
}
