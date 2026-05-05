package certifiedcarry_api.chat.service;

import certifiedcarry_api.chat.ChatFields;
import certifiedcarry_api.notification.service.NotificationOrchestratorService;
import certifiedcarry_api.shared.HttpErrors;
import certifiedcarry_api.shared.HttpRequestParsers;
import certifiedcarry_api.shared.PreparedStatementBinder;
import certifiedcarry_api.shared.SqlErrorMapper;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
@Transactional(readOnly = true)
public class ChatMessageService {

  private static final String MESSAGE_SELECT = """
      SELECT
        id,
        thread_id,
        sender_id,
        recipient_id,
        body,
        created_at,
        read_at
      FROM chat_messages
      """;

  private static final String WHERE_ID_CLAUSE = " WHERE id = ?";
  private static final String CHAT_MESSAGE_NOT_FOUND_PREFIX = "Chat message not found for id ";

  private final JdbcTemplate jdbcTemplate;
  private final NotificationOrchestratorService notificationOrchestratorService;

  public ChatMessageService(
      JdbcTemplate jdbcTemplate,
      NotificationOrchestratorService notificationOrchestratorService) {
    this.jdbcTemplate = jdbcTemplate;
    this.notificationOrchestratorService = notificationOrchestratorService;
  }

  public List<Map<String, Object>> getChatMessagesForActor(long actorUserId, boolean isAdmin) {
    return isAdmin ? getChatMessages() : getChatMessagesForUser(actorUserId);
  }

  public Map<String, Object> createChatMessageForActor(
      Map<String, Object> request, long actorUserId, boolean isAdmin) {
    Map<String, Object> mutableRequest = new LinkedHashMap<>(request);

    if (!isAdmin) {
      long senderId = parseLongRequired(mutableRequest.get(ChatFields.SENDER_ID), ChatFields.SENDER_ID);
      if (senderId != actorUserId) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "You can only send messages as yourself.");
      }

      long threadId = parseLongRequired(mutableRequest.get(ChatFields.THREAD_ID), ChatFields.THREAD_ID);
      if (!isThreadParticipant(threadId, actorUserId)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "You can only send messages in conversations that include your own user id.");
      }
    }

    return createChatMessage(mutableRequest);
  }

  public Map<String, Object> patchChatMessageForActor(
      String messageId, Map<String, Object> request, long actorUserId, boolean isAdmin) {
    Map<String, Object> mutableRequest = new LinkedHashMap<>(request);

    if (!isAdmin) {
      enforceReadOnlyPatchShape(mutableRequest, Set.of(ChatFields.READ_AT));

      if (!isMessageRecipient(messageId, actorUserId)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "You can only mark messages addressed to your own user id.");
      }
    }

    return patchChatMessage(messageId, mutableRequest);
  }

  public List<Map<String, Object>> getChatMessages() {
    return jdbcTemplate.query(
        MESSAGE_SELECT + " ORDER BY created_at ASC, id ASC", this::mapChatMessageRow);
  }

  public List<Map<String, Object>> getChatMessagesForUser(long userId) {
    String sql = """
        SELECT
          m.id,
          m.thread_id,
          m.sender_id,
          m.recipient_id,
          m.body,
          m.created_at,
          m.read_at
        FROM chat_messages m
        JOIN chat_threads t ON t.id = m.thread_id
        WHERE t.participant_user_id_1 = ? OR t.participant_user_id_2 = ?
        ORDER BY m.created_at ASC, m.id ASC
        """;

    return jdbcTemplate.query(sql, this::mapChatMessageRow, userId, userId);
  }

  public boolean isMessageRecipient(String messageId, long userId) {
    long parsedId = HttpRequestParsers.parsePathId(messageId, "messageId");
    Integer count =
        jdbcTemplate.queryForObject(
            """
                SELECT COUNT(*)
                FROM chat_messages m
                JOIN chat_threads t ON t.id = m.thread_id
                WHERE m.id = ?
                  AND m.recipient_id = ?
                  AND (t.participant_user_id_1 = ? OR t.participant_user_id_2 = ?)
                """,
            Integer.class,
            parsedId,
            userId,
            userId,
            userId);

    return count != null && count > 0;
  }

  @Transactional
  public Map<String, Object> createChatMessage(Map<String, Object> request) {
    long threadId =
        HttpRequestParsers.requireLong(request.get(ChatFields.THREAD_ID), ChatFields.THREAD_ID);
    long senderId =
        HttpRequestParsers.requireLong(request.get(ChatFields.SENDER_ID), ChatFields.SENDER_ID);
    long recipientId =
        HttpRequestParsers.requireLong(request.get(ChatFields.RECIPIENT_ID), ChatFields.RECIPIENT_ID);
    String body = HttpRequestParsers.requireNonBlank(request.get(ChatFields.BODY), ChatFields.BODY);
    OffsetDateTime createdAt =
        HttpRequestParsers.optionalOffsetDateTime(request.get(ChatFields.CREATED_AT), ChatFields.CREATED_AT);
    OffsetDateTime readAt =
        HttpRequestParsers.optionalOffsetDateTime(request.get(ChatFields.READ_AT), ChatFields.READ_AT);

    String sql = """
        INSERT INTO chat_messages (
          thread_id,
          sender_id,
          recipient_id,
          body,
          created_at,
          read_at
        )
        VALUES (?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    try {
      Long createdId =
          jdbcTemplate.execute(
              (PreparedStatementCreator)
                  connection -> {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setLong(1, threadId);
                    statement.setLong(2, senderId);
                    statement.setLong(3, recipientId);
                    statement.setString(4, body);
                    statement.setObject(
                        5, createdAt != null ? createdAt : OffsetDateTime.now(ZoneOffset.UTC));
                    PreparedStatementBinder.setNullableOffsetDateTime(statement, 6, readAt);
                    return statement;
                  },
              (PreparedStatementCallback<Long>)
                  statement -> {
                    try (ResultSet resultSet = statement.executeQuery()) {
                      if (!resultSet.next()) {
                        throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create chat message.");
                      }

                      return resultSet.getLong(1);
                    }
                  });

      notificationOrchestratorService.registerChatMessageCreatedAfterCommit(recipientId);

      return findChatMessageById(createdId);
    } catch (DataIntegrityViolationException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessage(
              exception.getMostSpecificCause(), "Invalid chat message payload."));
    } catch (UncategorizedSQLException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessage(
              exception.getSQLException(), "Invalid chat message payload."));
    }
  }

  @Transactional
  public Map<String, Object> patchChatMessage(String messageId, Map<String, Object> request) {
    long parsedId = HttpRequestParsers.parsePathId(messageId, "messageId");
    ChatMessageRow existing = findChatMessageRow(parsedId);

    long threadId =
        request.containsKey(ChatFields.THREAD_ID)
            ? HttpRequestParsers.requireLong(request.get(ChatFields.THREAD_ID), ChatFields.THREAD_ID)
            : existing.threadId();
    long senderId =
        request.containsKey(ChatFields.SENDER_ID)
            ? HttpRequestParsers.requireLong(request.get(ChatFields.SENDER_ID), ChatFields.SENDER_ID)
            : existing.senderId();
    long recipientId =
        request.containsKey(ChatFields.RECIPIENT_ID)
            ? HttpRequestParsers.requireLong(request.get(ChatFields.RECIPIENT_ID), ChatFields.RECIPIENT_ID)
            : existing.recipientId();
    String body =
        request.containsKey(ChatFields.BODY)
            ? HttpRequestParsers.requireNonBlank(request.get(ChatFields.BODY), ChatFields.BODY)
            : existing.body();
    OffsetDateTime createdAt =
        request.containsKey(ChatFields.CREATED_AT)
            ? HttpRequestParsers.optionalOffsetDateTime(
                request.get(ChatFields.CREATED_AT), ChatFields.CREATED_AT)
            : existing.createdAt();
    OffsetDateTime readAt =
        request.containsKey(ChatFields.READ_AT)
            ? HttpRequestParsers.optionalOffsetDateTime(request.get(ChatFields.READ_AT), ChatFields.READ_AT)
            : existing.readAt();

    String sql = """
        UPDATE chat_messages
        SET
          thread_id = ?,
          sender_id = ?,
          recipient_id = ?,
          body = ?,
          created_at = ?,
          read_at = ?
        WHERE id = ?
        """;

    try {
      int updatedRows =
          jdbcTemplate.execute(
              (PreparedStatementCreator)
                  connection -> {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setLong(1, threadId);
                    statement.setLong(2, senderId);
                    statement.setLong(3, recipientId);
                    statement.setString(4, body);
                    statement.setObject(
                        5, createdAt != null ? createdAt : OffsetDateTime.now(ZoneOffset.UTC));
                    PreparedStatementBinder.setNullableOffsetDateTime(statement, 6, readAt);
                    statement.setLong(7, parsedId);
                    return statement;
                  },
              (PreparedStatementCallback<Integer>) PreparedStatement::executeUpdate);

      if (updatedRows == 0) {
        throw HttpErrors.notFound(CHAT_MESSAGE_NOT_FOUND_PREFIX + messageId);
      }

      return findChatMessageById(parsedId);
    } catch (DataIntegrityViolationException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessage(
              exception.getMostSpecificCause(), "Invalid chat message patch payload."));
    } catch (UncategorizedSQLException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessage(
              exception.getSQLException(), "Invalid chat message patch payload."));
    }
  }

  private boolean isThreadParticipant(long threadId, long userId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_threads WHERE id = ? AND (participant_user_id_1 = ? OR participant_user_id_2 = ?)",
            Integer.class,
            threadId,
            userId,
            userId);

    return count != null && count > 0;
  }

  private ChatMessageRow findChatMessageRow(long id) {
    List<ChatMessageRow> rows =
        jdbcTemplate.query(MESSAGE_SELECT + WHERE_ID_CLAUSE, this::mapChatMessageDataRow, id);
    if (rows.isEmpty()) {
      throw HttpErrors.notFound(CHAT_MESSAGE_NOT_FOUND_PREFIX + id);
    }

    return rows.get(0);
  }

  private Map<String, Object> findChatMessageById(long id) {
    List<Map<String, Object>> rows =
        jdbcTemplate.query(MESSAGE_SELECT + WHERE_ID_CLAUSE, this::mapChatMessageRow, id);
    if (rows.isEmpty()) {
      throw HttpErrors.notFound(CHAT_MESSAGE_NOT_FOUND_PREFIX + id);
    }

    return rows.get(0);
  }

  private ChatMessageRow mapChatMessageDataRow(ResultSet resultSet, int rowNumber) throws SQLException {
    return new ChatMessageRow(
        resultSet.getLong("id"),
        resultSet.getLong("thread_id"),
        resultSet.getLong("sender_id"),
        resultSet.getLong("recipient_id"),
        resultSet.getString("body"),
        resultSet.getObject("created_at", OffsetDateTime.class),
        resultSet.getObject("read_at", OffsetDateTime.class));
  }

  private Map<String, Object> mapChatMessageRow(ResultSet resultSet, int rowNumber) throws SQLException {
    ChatMessageRow data = mapChatMessageDataRow(resultSet, rowNumber);

    Map<String, Object> row = new LinkedHashMap<>();
    row.put(ChatFields.ID, String.valueOf(data.id()));
    row.put(ChatFields.THREAD_ID, String.valueOf(data.threadId()));
    row.put(ChatFields.SENDER_ID, String.valueOf(data.senderId()));
    row.put(ChatFields.RECIPIENT_ID, String.valueOf(data.recipientId()));
    row.put(ChatFields.BODY, data.body());
    row.put(ChatFields.CREATED_AT, data.createdAt());
    row.put(ChatFields.READ_AT, data.readAt());
    return row;
  }

  private void enforceReadOnlyPatchShape(Map<String, Object> request, Set<String> allowedFields) {
    for (String field : request.keySet()) {
      if (!allowedFields.contains(field)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Only readAt can be updated for this operation.");
      }
    }
  }

  private long parseLongValue(Object value, String fieldName) {
    if (value instanceof Number numericValue) {
      return numericValue.longValue();
    }

    try {
      return Long.parseLong(String.valueOf(value).trim());
    } catch (NumberFormatException exception) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, fieldName + " must be a numeric string.");
    }
  }

  private long parseLongRequired(Object value, String fieldName) {
    if (value == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required.");
    }

    return parseLongValue(value, fieldName);
  }

  private record ChatMessageRow(
      long id,
      long threadId,
      long senderId,
      long recipientId,
      String body,
      OffsetDateTime createdAt,
      OffsetDateTime readAt) {}
}
