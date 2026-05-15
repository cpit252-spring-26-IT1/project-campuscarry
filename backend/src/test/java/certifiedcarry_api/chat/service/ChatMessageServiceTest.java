package certifiedcarry_api.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.chat.ChatFields;
import certifiedcarry_api.notification.service.NotificationOrchestratorService;
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
class ChatMessageServiceTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private NotificationOrchestratorService notificationOrchestratorService;

  @Test
  void actorGuardsEnforceSenderParticipantAndReadOnlyPatchShape() {
    ChatMessageService service =
        new ChatMessageService(jdbcTemplate, notificationOrchestratorService) {
          @Override
          public Map<String, Object> createChatMessage(Map<String, Object> request) {
            return request;
          }

          @Override
          public Map<String, Object> patchChatMessage(String messageId, Map<String, Object> request) {
            Map<String, Object> result = new LinkedHashMap<>(request);
            result.put(ChatFields.ID, messageId);
            return result;
          }

          @Override
          public boolean isMessageRecipient(String messageId, long userId) {
            return "mine".equals(messageId);
          }
        };

    when(jdbcTemplate.queryForObject(anyString(), any(Class.class), anyLong(), anyLong(), anyLong()))
        .thenReturn(1)
        .thenReturn(0);

    Map<String, Object> createRequest =
        new LinkedHashMap<>(
            Map.of(
                ChatFields.THREAD_ID, "5",
                ChatFields.SENDER_ID, "7",
                ChatFields.RECIPIENT_ID, "12",
                ChatFields.BODY, "hello"));
    assertEquals("7", service.createChatMessageForActor(createRequest, 7L, false).get(ChatFields.SENDER_ID));

    ResponseStatusException senderFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createChatMessageForActor(
                    new LinkedHashMap<>(
                        Map.of(
                            ChatFields.THREAD_ID, "5",
                            ChatFields.SENDER_ID, "12",
                            ChatFields.RECIPIENT_ID, "7",
                            ChatFields.BODY, "hello")),
                    7L,
                    false));
    assertEquals("You can only send messages as yourself.", senderFailure.getReason());

    ResponseStatusException participantFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createChatMessageForActor(
                    new LinkedHashMap<>(
                        Map.of(
                            ChatFields.THREAD_ID, "5",
                            ChatFields.SENDER_ID, "7",
                            ChatFields.RECIPIENT_ID, "12",
                            ChatFields.BODY, "hello")),
                    7L,
                    false));
    assertEquals(
        "You can only send messages in conversations that include your own user id.",
        participantFailure.getReason());

    ResponseStatusException patchShapeFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.patchChatMessageForActor(
                    "mine",
                    new LinkedHashMap<>(Map.of(ChatFields.BODY, "nope")),
                    7L,
                    false));
    assertEquals("Only readAt can be updated for this operation.", patchShapeFailure.getReason());

    ResponseStatusException recipientFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.patchChatMessageForActor(
                    "other",
                    new LinkedHashMap<>(Map.of(ChatFields.READ_AT, "2026-05-13T00:00:00Z")),
                    7L,
                    false));
    assertEquals(
        "You can only mark messages addressed to your own user id.",
        recipientFailure.getReason());
  }

  @Test
  void createMessageValidatesAndMapsSqlFailures() {
    ChatMessageService service = new ChatMessageService(jdbcTemplate, notificationOrchestratorService);
    Map<String, Object> request =
        Map.of(
            ChatFields.THREAD_ID, "5",
            ChatFields.SENDER_ID, "7",
            ChatFields.RECIPIENT_ID, "12",
            ChatFields.BODY, "hello");

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new DataIntegrityViolationException("boom", new RuntimeException("ERROR: invalid message")));

    ResponseStatusException createFailure =
        assertThrows(ResponseStatusException.class, () -> service.createChatMessage(request));
    assertEquals("invalid message", createFailure.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new UncategorizedSQLException("task", "sql", new SQLException("ERROR: invalid sql")));

    ResponseStatusException sqlFailure =
        assertThrows(ResponseStatusException.class, () -> service.createChatMessage(request));
    assertEquals("invalid sql", sqlFailure.getReason());
  }

  @Test
  void createMessageNotifiesAndPatchHandlesNotFoundAndBadRequest() throws SQLException {
    ChatMessageService service = new ChatMessageService(jdbcTemplate, notificationOrchestratorService);
    mockMessageRowQuery(4L);
    mockMessageMapQuery(77L);

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenReturn(77L)
        .thenReturn(0);

    Map<String, Object> created =
        service.createChatMessage(
            Map.of(
                ChatFields.THREAD_ID, "5",
                ChatFields.SENDER_ID, "7",
                ChatFields.RECIPIENT_ID, "12",
                ChatFields.BODY, "hello"));
    assertEquals("77", created.get(ChatFields.ID));
    verify(notificationOrchestratorService).registerChatMessageCreatedAfterCommit(12L);

    ResponseStatusException notFound =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchChatMessage("4", new LinkedHashMap<>()));
    assertEquals("Chat message not found for id 4", notFound.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new DataIntegrityViolationException("boom", new RuntimeException("ERROR: invalid patch")));
    ResponseStatusException patchFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchChatMessage("4", new LinkedHashMap<>()));
    assertEquals("invalid patch", patchFailure.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new UncategorizedSQLException("task", "sql", new SQLException("ERROR: invalid patch sql")));
    ResponseStatusException patchSqlFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.patchChatMessage("4", new LinkedHashMap<>()));
    assertEquals("invalid patch sql", patchSqlFailure.getReason());
  }

  @Test
  void patchMessageSuccessPathReturnsMappedRow() throws SQLException {
    ChatMessageService service = new ChatMessageService(jdbcTemplate, notificationOrchestratorService);
    mockMessageRowQuery(8L);

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenReturn(1);

    Map<String, Object> patched =
        service.patchChatMessage(
            "8",
            Map.of(
                ChatFields.BODY, "updated",
                ChatFields.READ_AT, "2026-05-13T03:00:00Z"));

    assertEquals("8", patched.get(ChatFields.ID));
    assertEquals("hello", patched.get(ChatFields.BODY));
  }

  @Test
  void messageRecipientChecksAndMappingWork() throws SQLException {
    ChatMessageService service = new ChatMessageService(jdbcTemplate, notificationOrchestratorService);
    when(jdbcTemplate.queryForObject(anyString(), any(Class.class), anyLong(), anyLong(), anyLong(), anyLong()))
        .thenReturn(1)
        .thenReturn(null);
    assertEquals(true, service.isMessageRecipient("8", 7L));
    assertEquals(false, service.isMessageRecipient("8", 7L));

    mockMessageMapListQuery();
    List<Map<String, Object>> rows = service.getChatMessagesForUser(7L);
    assertEquals(1, rows.size());
    assertEquals("8", rows.get(0).get(ChatFields.ID));
  }

  @Test
  void actorScopedReadsAndCreateFailureWithoutReturnedIdAreHandled() throws SQLException {
    ChatMessageService service = new ChatMessageService(jdbcTemplate, notificationOrchestratorService);
    mockMessageMapQuery();
    mockMessageMapListQuery();

    List<Map<String, Object>> adminRows = service.getChatMessagesForActor(7L, true);
    List<Map<String, Object>> userRows = service.getChatMessagesForActor(7L, false);

    assertEquals("8", adminRows.get(0).get(ChatFields.ID));
    assertEquals("8", userRows.get(0).get(ChatFields.ID));

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenAnswer(
            invocation -> {
              PreparedStatementCreator creator = invocation.getArgument(0);
              @SuppressWarnings("unchecked")
              PreparedStatementCallback<Long> callback = invocation.getArgument(1);
              Connection connection = org.mockito.Mockito.mock(Connection.class);
              java.sql.PreparedStatement statement = org.mockito.Mockito.mock(java.sql.PreparedStatement.class);
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
                service.createChatMessage(
                    Map.of(
                        ChatFields.THREAD_ID, "5",
                        ChatFields.SENDER_ID, "7",
                        ChatFields.RECIPIENT_ID, "12",
                        ChatFields.BODY, "hello")));
    assertEquals("Failed to create chat message.", failure.getReason());
  }

  @SuppressWarnings("unchecked")
  private void mockMessageRowQuery(long id) {
    doAnswer(
            invocation -> {
              RowMapper<Object> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockMessageResultSet(id), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq(id));
  }

  @SuppressWarnings("unchecked")
  private void mockMessageMapQuery(long id) {
    doAnswer(
            invocation -> {
              RowMapper<Object> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockMessageResultSet(id), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq(id));
  }

  @SuppressWarnings("unchecked")
  private void mockMessageMapListQuery() {
    doAnswer(
            invocation -> {
              RowMapper<Object> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockMessageResultSet(8L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), anyLong(), anyLong());
  }

  @SuppressWarnings("unchecked")
  private void mockMessageMapQuery() {
    doAnswer(
            invocation -> {
              RowMapper<Object> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockMessageResultSet(8L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class));
  }

  private ResultSet mockMessageResultSet(long id) throws SQLException {
    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
    OffsetDateTime timestamp = OffsetDateTime.parse("2026-05-13T02:00:00Z");
    when(resultSet.getLong("id")).thenReturn(id);
    when(resultSet.getLong("thread_id")).thenReturn(5L);
    when(resultSet.getLong("sender_id")).thenReturn(7L);
    when(resultSet.getLong("recipient_id")).thenReturn(12L);
    when(resultSet.getString("body")).thenReturn("hello");
    when(resultSet.getObject("created_at", OffsetDateTime.class)).thenReturn(timestamp);
    when(resultSet.getObject("read_at", OffsetDateTime.class)).thenReturn(null);
    return resultSet;
  }
}
