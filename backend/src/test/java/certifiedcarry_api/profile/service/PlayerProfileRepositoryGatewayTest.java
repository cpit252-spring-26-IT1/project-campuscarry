package certifiedcarry_api.profile.service;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.profile.PlayerProfileFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PlayerProfileRepositoryGatewayTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Test
  void queryHelpersOwnershipAndNotFoundPathsWork() throws SQLException {
    PlayerProfileRepositoryGateway gateway = new PlayerProfileRepositoryGateway(jdbcTemplate, objectMapper);

    when(jdbcTemplate.queryForObject(anyString(), any(Class.class), anyLong(), anyLong()))
        .thenReturn(1)
        .thenReturn(null);

    assertTrue(gateway.isProfileOwnedBy("7", 12L));
    assertFalse(gateway.isProfileOwnedBy("7", 12L));

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockProfileResultSet(5L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class));

    List<Map<String, Object>> rows = gateway.getPlayerProfiles(null);
    assertEquals(1, rows.size());
    assertEquals("5", rows.get(0).get(PlayerProfileFields.ID));
    assertEquals("VALORANT", rows.get(0).get(PlayerProfileFields.GAME));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> modes =
        (List<Map<String, Object>>) rows.get(0).get(PlayerProfileFields.ROCKET_LEAGUE_MODES);
    assertEquals("2v2", modes.get(0).get("mode"));

    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(99L))).thenReturn(List.of());
    ResponseStatusException notFoundRow =
        assertThrows(ResponseStatusException.class, () -> gateway.findPlayerProfileRow(99L));
    assertEquals("Player profile not found for id 99", notFoundRow.getReason());

    ResponseStatusException notFoundMap =
        assertThrows(ResponseStatusException.class, () -> gateway.findPlayerProfileById(99L));
    assertEquals("Player profile not found for id 99", notFoundMap.getReason());
  }

  @Test
  void insertAndUpdateBindPayloadAndReturnResults() throws Exception {
    PlayerProfileRepositoryGateway gateway = new PlayerProfileRepositoryGateway(jdbcTemplate, objectMapper);
    PlayerProfilePayload payload = payload();

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenAnswer(
            invocation -> {
              PreparedStatementCreator creator = invocation.getArgument(0);
              @SuppressWarnings("unchecked")
              PreparedStatementCallback<Long> callback = invocation.getArgument(1);
              Connection connection = mock(Connection.class);
              PreparedStatement statement = mock(PreparedStatement.class);
              java.sql.Array textArray = mock(java.sql.Array.class);
              ResultSet resultSet = mock(ResultSet.class);

              when(connection.prepareStatement(anyString())).thenReturn(statement);
              when(connection.createArrayOf(eq("text"), any(Object[].class))).thenReturn(textArray);
              when(statement.executeQuery()).thenReturn(resultSet);
              when(resultSet.next()).thenReturn(true);
              when(resultSet.getLong(1)).thenReturn(44L);

              creator.createPreparedStatement(connection);
              Long createdId = callback.doInPreparedStatement(statement);

              verify(statement).setLong(1, 7L);
              verify(statement).setString(2, "bluy");
              verify(statement).setString(18, PlayerProfileFields.STATUS_APPROVED);
              verify(statement).setBoolean(21, true);
              return createdId;
            })
        .thenAnswer(
            invocation -> {
              PreparedStatementCreator creator = invocation.getArgument(0);
              @SuppressWarnings("unchecked")
              PreparedStatementCallback<Integer> callback = invocation.getArgument(1);
              Connection connection = mock(Connection.class);
              PreparedStatement statement = mock(PreparedStatement.class);
              java.sql.Array textArray = mock(java.sql.Array.class);

              when(connection.prepareStatement(anyString())).thenReturn(statement);
              when(connection.createArrayOf(eq("text"), any(Object[].class))).thenReturn(textArray);
              when(statement.executeUpdate()).thenReturn(1);

              creator.createPreparedStatement(connection);
              Integer updated = callback.doInPreparedStatement(statement);

              verify(statement).setLong(27, 88L);
              return updated;
            });

    assertEquals(44L, gateway.insertProfile(payload));
    assertEquals(1, gateway.updateProfile(88L, payload));
  }

  private PlayerProfilePayload payload() {
    return new PlayerProfilePayload(
        7L,
        "bluy",
        "https://cdn.example/avatar.png",
        "VALORANT",
        "Immortal",
        true,
        true,
        "Cloud 9",
        List.of("{\"mode\":\"2v2\",\"rank\":\"Champion\"}"),
        "2v2",
        List.of("duelist", "igl"),
        "duelist",
        new BigDecimal("1500"),
        "RR",
        "https://cdn.example/proof.png",
        "bio",
        "https://clips.example",
        PlayerProfileFields.STATUS_APPROVED,
        "",
        null,
        true,
        OffsetDateTime.parse("2026-05-13T00:00:00Z"),
        OffsetDateTime.parse("2026-05-13T01:00:00Z"),
        OffsetDateTime.parse("2026-06-13T01:00:00Z"),
        null,
        OffsetDateTime.parse("2026-05-13T02:00:00Z"));
  }

  private ResultSet mockProfileResultSet(long id) throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    java.sql.Array rocketLeagueModes = mock(java.sql.Array.class);
    java.sql.Array inGameRoles = mock(java.sql.Array.class);
    OffsetDateTime submittedAt = OffsetDateTime.parse("2026-05-13T00:00:00Z");
    OffsetDateTime verifiedAt = OffsetDateTime.parse("2026-05-13T01:00:00Z");
    OffsetDateTime expiresAt = OffsetDateTime.parse("2026-06-13T01:00:00Z");
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-10T00:00:00Z");
    OffsetDateTime updatedAt = OffsetDateTime.parse("2026-05-13T02:00:00Z");

    when(rocketLeagueModes.getArray())
        .thenReturn(new String[] {"{\"mode\":\"2v2\",\"rank\":\"Champion\",\"ratingLabel\":\"MMR\"}"});
    when(inGameRoles.getArray()).thenReturn(new String[] {"duelist", "igl"});

    when(resultSet.getLong("id")).thenReturn(id);
    when(resultSet.getLong("user_id")).thenReturn(7L);
    when(resultSet.getString("username")).thenReturn("bluy");
    when(resultSet.getString("profile_image")).thenReturn("https://cdn.example/avatar.png");
    when(resultSet.getString("game")).thenReturn("VALORANT");
    when(resultSet.getString("rank")).thenReturn("Immortal");
    when(resultSet.getBoolean("allow_player_chats")).thenReturn(true);
    when(resultSet.getBoolean("is_with_team")).thenReturn(true);
    when(resultSet.getString("team_name")).thenReturn("Cloud 9");
    when(resultSet.getArray("rocket_league_modes")).thenReturn(rocketLeagueModes);
    when(resultSet.getString("primary_rocket_league_mode")).thenReturn("2v2");
    when(resultSet.getArray("in_game_roles")).thenReturn(inGameRoles);
    when(resultSet.getString("in_game_role")).thenReturn("duelist");
    when(resultSet.getBigDecimal("rating_value")).thenReturn(new BigDecimal("1500"));
    when(resultSet.getString("rating_label")).thenReturn("RR");
    when(resultSet.getString("proof_image")).thenReturn("https://cdn.example/proof.png");
    when(resultSet.getString("bio")).thenReturn("bio");
    when(resultSet.getString("clips_url")).thenReturn("https://clips.example");
    when(resultSet.getString("rank_verification_status")).thenReturn("APPROVED");
    when(resultSet.getString("decline_reason")).thenReturn("");
    when(resultSet.getObject("declined_at", OffsetDateTime.class)).thenReturn(null);
    when(resultSet.getBoolean("is_verified")).thenReturn(true);
    when(resultSet.getObject("submitted_at", OffsetDateTime.class)).thenReturn(submittedAt);
    when(resultSet.getObject("rank_verified_at", OffsetDateTime.class)).thenReturn(verifiedAt);
    when(resultSet.getObject("rank_expires_at", OffsetDateTime.class)).thenReturn(expiresAt);
    when(resultSet.getObject("rank_expiry_reminder_sent_at", OffsetDateTime.class)).thenReturn(null);
    when(resultSet.getObject("created_at", OffsetDateTime.class)).thenReturn(createdAt);
    when(resultSet.getObject("updated_at", OffsetDateTime.class)).thenReturn(updatedAt);
    return resultSet;
  }
}
