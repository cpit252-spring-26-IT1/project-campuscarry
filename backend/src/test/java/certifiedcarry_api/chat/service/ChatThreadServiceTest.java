package certifiedcarry_api.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import certifiedcarry_api.chat.ChatFields;
import java.sql.PreparedStatement;
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
class ChatThreadServiceTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Test
  void actorGuardsEnforceOwnershipAndAllowedFields() {
    ChatThreadService service =
        new ChatThreadService(jdbcTemplate) {
          @Override
          public boolean isThreadParticipant(String threadId, long userId) {
            return "owned".equals(threadId);
          }

          @Override
          public Map<String, Object> createChatThread(Map<String, Object> request) {
            return request;
          }

          @Override
          public Map<String, Object> patchChatThread(String threadId, Map<String, Object> request) {
            Map<String, Object> result = new LinkedHashMap<>(request);
            result.put(ChatFields.ID, threadId);
            return result;
          }
        };

    Map<String, Object> createRequest = new LinkedHashMap<>();
    createRequest.put(ChatFields.PARTICIPANT_IDS, List.of("7", "12"));
    createRequest.put(ChatFields.INITIATED_BY_ROLE, "PLAYER");
    Map<String, Object> created = service.createChatThreadForActor(createRequest, 7L, false);
    assertEquals("7", created.get(ChatFields.INITIATED_BY_ID));

    ResponseStatusException foreignInitiatorFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createChatThreadForActor(
                    new LinkedHashMap<>(
                        Map.of(
                            ChatFields.PARTICIPANT_IDS, List.of("7", "12"),
                            ChatFields.INITIATED_BY_ROLE, "PLAYER",
                            ChatFields.INITIATED_BY_ID, "9")),
                    7L,
                    false));
    assertEquals("You can only initiate conversations as yourself.", foreignInitiatorFailure.getReason());

    ResponseStatusException missingParticipantFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createChatThreadForActor(
                    new LinkedHashMap<>(
                        Map.of(
                            ChatFields.PARTICIPANT_IDS, List.of("12", "13"),
                            ChatFields.INITIATED_BY_ROLE, "PLAYER")),
                    7L,
                    false));
    assertEquals(
        "You can only create conversations that include your own user id.",
        missingParticipantFailure.getReason());

    ResponseStatusException nonParticipantPatchFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchChatThreadForActor("other", new LinkedHashMap<>(), 7L, false));
    assertEquals(
        "You can only modify conversations that include your own user id.",
        nonParticipantPatchFailure.getReason());

    ResponseStatusException forbiddenFieldFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.patchChatThreadForActor(
                    "owned",
                    new LinkedHashMap<>(Map.of(ChatFields.PARTICIPANT_IDS, List.of("7", "12"))),
                    7L,
                    false));
    assertEquals(
        "Field 'participantIds' cannot be changed for this operation.",
        forbiddenFieldFailure.getReason());

    ResponseStatusException wrongSenderFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.patchChatThreadForActor(
                    "owned",
                    new LinkedHashMap<>(Map.of(ChatFields.LAST_SENDER_ID, "12")),
                    7L,
                    false));
    assertEquals("You can only set lastSenderId to your own user id.", wrongSenderFailure.getReason());
  }

  @Test
  void createThreadValidatesParticipantShapeAndRoleBeforeDatabaseWrite() {
    ChatThreadService service = new ChatThreadService(jdbcTemplate);

    ResponseStatusException notArray =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createChatThread(
                    Map.of(
                        ChatFields.PARTICIPANT_IDS, "bad",
                        ChatFields.INITIATED_BY_ID, "7",
                        ChatFields.INITIATED_BY_ROLE, "PLAYER")));
    assertEquals("participantIds must be an array with exactly two unique user ids.", notArray.getReason());

    ResponseStatusException wrongSize =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createChatThread(
                    Map.of(
                        ChatFields.PARTICIPANT_IDS, List.of("7"),
                        ChatFields.INITIATED_BY_ID, "7",
                        ChatFields.INITIATED_BY_ROLE, "PLAYER")));
    assertEquals("participantIds must contain exactly two user ids.", wrongSize.getReason());

    ResponseStatusException sameParticipants =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createChatThread(
                    Map.of(
                        ChatFields.PARTICIPANT_IDS, List.of("7", "7"),
                        ChatFields.INITIATED_BY_ID, "7",
                        ChatFields.INITIATED_BY_ROLE, "PLAYER")));
    assertEquals("participantIds must contain two different user ids.", sameParticipants.getReason());

    ResponseStatusException invalidRole =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createChatThread(
                    Map.of(
                        ChatFields.PARTICIPANT_IDS, List.of("7", "12"),
                        ChatFields.INITIATED_BY_ID, "7",
                        ChatFields.INITIATED_BY_ROLE, "COACH")));
    assertEquals(
        "initiatedByRole must be one of PLAYER, RECRUITER, or ADMIN.",
        invalidRole.getReason());
  }

  @Test
  void createThreadMapsConflictAndSqlFailures() {
    ChatThreadService service = new ChatThreadService(jdbcTemplate);
    Map<String, Object> request =
        Map.of(
            ChatFields.PARTICIPANT_IDS, List.of("12", "7"),
            ChatFields.INITIATED_BY_ID, "7",
            ChatFields.INITIATED_BY_ROLE, "PLAYER");

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new DataIntegrityViolationException("boom", new RuntimeException("ERROR: duplicate key value")));

    ResponseStatusException conflict =
        assertThrows(ResponseStatusException.class, () -> service.createChatThread(request));
    assertEquals("Conversation already exists for this participant pair.", conflict.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new UncategorizedSQLException("task", "sql", new SQLException("ERROR: invalid thread payload")));

    ResponseStatusException badRequest =
        assertThrows(ResponseStatusException.class, () -> service.createChatThread(request));
    assertEquals("invalid thread payload", badRequest.getReason());
  }

  @Test
  void patchThreadUsesExistingDefaultsAndMapsNotFoundAndConflict() {
    ChatThreadService service = new ChatThreadService(jdbcTemplate);
    mockThreadQueryData(9L);

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenReturn(0);

    ResponseStatusException notFound =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchChatThread("9", new LinkedHashMap<>()));
    assertEquals("Chat thread not found for id 9", notFound.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new DataIntegrityViolationException("boom", new RuntimeException("ERROR: duplicate key value")));

    ResponseStatusException conflict =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchChatThread("9", new LinkedHashMap<>()));
    assertEquals("Chat thread patch conflicts with existing records.", conflict.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new UncategorizedSQLException("task", "sql", new SQLException("ERROR: invalid thread patch payload")));
    ResponseStatusException badRequest =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchChatThread("9", new LinkedHashMap<>()));
    assertEquals("invalid thread patch payload", badRequest.getReason());
  }

  @Test
  void createAndPatchThreadSuccessPathsReturnMappedRows() throws SQLException {
    ChatThreadService service = new ChatThreadService(jdbcTemplate);
    Map<String, Object> createRequest =
        new LinkedHashMap<>(
            Map.of(
                ChatFields.PARTICIPANT_IDS, List.of("12", "13"),
                ChatFields.INITIATED_BY_ID, "12",
                ChatFields.INITIATED_BY_ROLE, "PLAYER",
                ChatFields.LAST_MESSAGE_PREVIEW, ""));

    mockThreadQueryData(5L);
    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenReturn(5L)
        .thenReturn(1);

    Map<String, Object> created = service.createChatThread(createRequest);
    Map<String, Object> patched =
        service.patchChatThread(
            "5",
            Map.of(
                ChatFields.LAST_SENDER_ID, "12",
                ChatFields.LAST_MESSAGE_PREVIEW, "updated",
                ChatFields.UPDATED_AT, "2026-05-13T03:00:00Z"));

    assertEquals("5", created.get(ChatFields.ID));
    assertEquals("5", patched.get(ChatFields.ID));
    assertEquals(List.of("12", "13"), patched.get(ChatFields.PARTICIPANT_IDS));
  }

  @Test
  void threadParticipantChecksHandleNullCountAndMappedRows() throws SQLException {
    ChatThreadService service = new ChatThreadService(jdbcTemplate);
    when(jdbcTemplate.queryForObject(anyString(), any(Class.class), anyLong(), anyLong(), anyLong()))
        .thenReturn(1)
        .thenReturn(null)
        .thenReturn(1);

    assertEquals(true, service.isThreadParticipant("7", 12L));
    assertEquals(false, service.isThreadParticipant("7", 12L));
    assertEquals(true, service.isThreadParticipant(7L, 12L));

    mockThreadMapQuery();
    List<Map<String, Object>> rows = service.getChatThreads();
    assertEquals(1, rows.size());
    assertEquals(List.of("12", "13"), rows.get(0).get(ChatFields.PARTICIPANT_IDS));
  }

  @Test
  void actorScopedReadsAndCreateFailureWithoutReturnedIdAreHandled() throws SQLException {
    ChatThreadService service = new ChatThreadService(jdbcTemplate);
    mockThreadMapQuery();
    mockThreadMapListQueryForUser();

    List<Map<String, Object>> adminRows = service.getChatThreadsForActor(13L, true);
    List<Map<String, Object>> userRows = service.getChatThreadsForActor(13L, false);

    assertEquals("5", adminRows.get(0).get(ChatFields.ID));
    assertEquals("5", userRows.get(0).get(ChatFields.ID));

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenAnswer(
            invocation -> {
              PreparedStatementCreator creator = invocation.getArgument(0);
              @SuppressWarnings("unchecked")
              PreparedStatementCallback<Long> callback = invocation.getArgument(1);
              Connection connection = org.mockito.Mockito.mock(Connection.class);
              PreparedStatement statement = org.mockito.Mockito.mock(PreparedStatement.class);
              ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
              when(connection.prepareStatement(anyString())).thenReturn(statement);
              when(statement.executeQuery()).thenReturn(resultSet);
              when(resultSet.next()).thenReturn(false);
              creator.createPreparedStatement(connection);
              return callback.doInPreparedStatement(statement);
            });

    ResponseStatusException failure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createChatThread(
                    Map.of(
                        ChatFields.PARTICIPANT_IDS, List.of("7", "12"),
                        ChatFields.INITIATED_BY_ID, "7",
                        ChatFields.INITIATED_BY_ROLE, "PLAYER")));
    assertEquals("Failed to create chat thread.", failure.getReason());
  }

  @SuppressWarnings("unchecked")
  private void mockThreadQueryData(long id) {
    doAnswer(
            invocation -> {
              RowMapper<Object> mapper = invocation.getArgument(1);
              ResultSet resultSet = mockThreadResultSet(id);
              return List.of(mapper.mapRow(resultSet, 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq(id));
  }

  @SuppressWarnings("unchecked")
  private void mockThreadMapQuery() {
    doAnswer(
            invocation -> {
              RowMapper<Object> mapper = invocation.getArgument(1);
              ResultSet resultSet = mockThreadResultSet(5L);
              return List.of(mapper.mapRow(resultSet, 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class));
  }

  @SuppressWarnings("unchecked")
  private void mockThreadMapListQueryForUser() {
    doAnswer(
            invocation -> {
              RowMapper<Object> mapper = invocation.getArgument(1);
              ResultSet resultSet = mockThreadResultSet(5L);
              return List.of(mapper.mapRow(resultSet, 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), anyLong(), anyLong());
  }

  private ResultSet mockThreadResultSet(long id) throws SQLException {
    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
    OffsetDateTime timestamp = OffsetDateTime.parse("2026-05-13T02:00:00Z");
    when(resultSet.getLong("id")).thenReturn(id);
    when(resultSet.getLong("participant_user_id_1")).thenReturn(12L);
    when(resultSet.getLong("participant_user_id_2")).thenReturn(13L);
    when(resultSet.getLong("initiated_by_id")).thenReturn(12L);
    when(resultSet.getString("initiated_by_role")).thenReturn("PLAYER");
    when(resultSet.getObject("last_sender_id", Long.class)).thenReturn(12L);
    when(resultSet.getObject("last_message_at", OffsetDateTime.class)).thenReturn(timestamp);
    when(resultSet.getString("last_message_preview")).thenReturn("preview");
    when(resultSet.getObject("created_at", OffsetDateTime.class)).thenReturn(timestamp.minusHours(1));
    when(resultSet.getObject("updated_at", OffsetDateTime.class)).thenReturn(timestamp);
    return resultSet;
  }
}
