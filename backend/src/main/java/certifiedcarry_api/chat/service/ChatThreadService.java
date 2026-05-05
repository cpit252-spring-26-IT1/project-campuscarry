package certifiedcarry_api.chat.service;

import certifiedcarry_api.chat.ChatFields;
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
import java.util.Locale;
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
public class ChatThreadService {

  private static final String THREAD_SELECT = """
      SELECT
        id,
        participant_user_id_1,
        participant_user_id_2,
        initiated_by_id,
        initiated_by_role,
        last_sender_id,
        last_message_at,
        last_message_preview,
        created_at,
        updated_at
      FROM chat_threads
      """;

  private static final String WHERE_ID_CLAUSE = " WHERE id = ?";
  private static final String CHAT_THREAD_NOT_FOUND_PREFIX = "Chat thread not found for id ";

  private final JdbcTemplate jdbcTemplate;

  public ChatThreadService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> getChatThreadsForActor(long actorUserId, boolean isAdmin) {
    return isAdmin ? getChatThreads() : getChatThreadsForUser(actorUserId);
  }

  public Map<String, Object> createChatThreadForActor(
      Map<String, Object> request, long actorUserId, boolean isAdmin) {
    Map<String, Object> mutableRequest = new LinkedHashMap<>(request);

    if (!isAdmin) {
      enforceInitiatorOwnership(mutableRequest, actorUserId);
      if (!payloadContainsParticipant(mutableRequest.get(ChatFields.PARTICIPANT_IDS), actorUserId)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "You can only create conversations that include your own user id.");
      }
    }

    return createChatThread(mutableRequest);
  }

  public Map<String, Object> patchChatThreadForActor(
      String threadId, Map<String, Object> request, long actorUserId, boolean isAdmin) {
    Map<String, Object> mutableRequest = new LinkedHashMap<>(request);

    if (!isAdmin) {
      if (!isThreadParticipant(threadId, actorUserId)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "You can only modify conversations that include your own user id.");
      }

      forbidFieldMutation(
          mutableRequest,
          Set.of(
              ChatFields.PARTICIPANT_IDS,
              ChatFields.INITIATED_BY_ID,
              ChatFields.INITIATED_BY_ROLE,
              ChatFields.CREATED_AT));

      if (mutableRequest.containsKey(ChatFields.LAST_SENDER_ID)
          && mutableRequest.get(ChatFields.LAST_SENDER_ID) != null) {
        long payloadLastSenderId =
            parseLongValue(mutableRequest.get(ChatFields.LAST_SENDER_ID), ChatFields.LAST_SENDER_ID);
        if (payloadLastSenderId != actorUserId) {
          throw new ResponseStatusException(
              HttpStatus.FORBIDDEN,
              "You can only set lastSenderId to your own user id.");
        }
      }
    }

    return patchChatThread(threadId, mutableRequest);
  }

  public List<Map<String, Object>> getChatThreads() {
    return jdbcTemplate.query(
        THREAD_SELECT + " ORDER BY updated_at DESC, id DESC", this::mapChatThreadRow);
  }

  public List<Map<String, Object>> getChatThreadsForUser(long userId) {
    return jdbcTemplate.query(
        THREAD_SELECT + " WHERE participant_user_id_1 = ? OR participant_user_id_2 = ?"
            + " ORDER BY updated_at DESC, id DESC",
        this::mapChatThreadRow,
        userId,
        userId);
  }

  @Transactional
  public Map<String, Object> createChatThread(Map<String, Object> request) {
    ParticipantPair participantPair = requireParticipantPair(request.get(ChatFields.PARTICIPANT_IDS));
    long initiatedById =
        HttpRequestParsers.requireLong(request.get(ChatFields.INITIATED_BY_ID), ChatFields.INITIATED_BY_ID);
    String initiatedByRole =
        requireRole(request.get(ChatFields.INITIATED_BY_ROLE), ChatFields.INITIATED_BY_ROLE);
    Long lastSenderId =
        HttpRequestParsers.optionalLong(request.get(ChatFields.LAST_SENDER_ID), ChatFields.LAST_SENDER_ID);
    OffsetDateTime lastMessageAt =
        HttpRequestParsers.optionalOffsetDateTime(
            request.get(ChatFields.LAST_MESSAGE_AT), ChatFields.LAST_MESSAGE_AT);
    String lastMessagePreview =
        HttpRequestParsers.defaultIfBlank(
            HttpRequestParsers.optionalString(request.get(ChatFields.LAST_MESSAGE_PREVIEW)), "");
    OffsetDateTime createdAt =
        HttpRequestParsers.optionalOffsetDateTime(request.get(ChatFields.CREATED_AT), ChatFields.CREATED_AT);
    OffsetDateTime updatedAt =
        HttpRequestParsers.optionalOffsetDateTime(request.get(ChatFields.UPDATED_AT), ChatFields.UPDATED_AT);

    String sql = """
        INSERT INTO chat_threads (
          participant_user_id_1,
          participant_user_id_2,
          initiated_by_id,
          initiated_by_role,
          last_sender_id,
          last_message_at,
          last_message_preview,
          created_at,
          updated_at
        )
        VALUES (?, ?, ?, CAST(? AS user_role_enum), ?, ?, ?, ?, ?)
        RETURNING id
        """;

    try {
      Long createdId =
          jdbcTemplate.execute(
              (PreparedStatementCreator)
                  connection -> {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setLong(1, participantPair.first());
                    statement.setLong(2, participantPair.second());
                    statement.setLong(3, initiatedById);
                    statement.setString(4, initiatedByRole);
                    PreparedStatementBinder.setNullableLong(statement, 5, lastSenderId);
                    PreparedStatementBinder.setNullableOffsetDateTime(statement, 6, lastMessageAt);
                    statement.setString(7, lastMessagePreview);
                    statement.setObject(
                        8, createdAt != null ? createdAt : OffsetDateTime.now(ZoneOffset.UTC));
                    statement.setObject(
                        9, updatedAt != null ? updatedAt : OffsetDateTime.now(ZoneOffset.UTC));
                    return statement;
                  },
              (PreparedStatementCallback<Long>)
                  statement -> {
                    try (ResultSet resultSet = statement.executeQuery()) {
                      if (!resultSet.next()) {
                        throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create chat thread.");
                      }

                      return resultSet.getLong(1);
                    }
                  });

      return findChatThreadById(createdId);
    } catch (DataIntegrityViolationException exception) {
      throw SqlErrorMapper.mapDataIntegrityViolation(
          exception,
          "Conversation already exists for this participant pair.",
          "Invalid chat thread payload.",
          HttpErrors::badRequest,
          HttpErrors::conflict);
    } catch (UncategorizedSQLException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessage(
              exception.getSQLException(), "Invalid chat thread payload."));
    }
  }

  @Transactional
  public Map<String, Object> patchChatThread(String threadId, Map<String, Object> request) {
    long parsedId = HttpRequestParsers.parsePathId(threadId, "threadId");
    ChatThreadRow existing = findChatThreadRow(parsedId);

    ParticipantPair participantPair =
        request.containsKey(ChatFields.PARTICIPANT_IDS)
            ? requireParticipantPair(request.get(ChatFields.PARTICIPANT_IDS))
            : new ParticipantPair(existing.participantUserId1(), existing.participantUserId2());

    long initiatedById =
        request.containsKey(ChatFields.INITIATED_BY_ID)
            ? HttpRequestParsers.requireLong(
                request.get(ChatFields.INITIATED_BY_ID), ChatFields.INITIATED_BY_ID)
            : existing.initiatedById();

    String initiatedByRole =
        request.containsKey(ChatFields.INITIATED_BY_ROLE)
            ? requireRole(request.get(ChatFields.INITIATED_BY_ROLE), ChatFields.INITIATED_BY_ROLE)
            : existing.initiatedByRole();

    Long lastSenderId =
        request.containsKey(ChatFields.LAST_SENDER_ID)
            ? HttpRequestParsers.optionalLong(
                request.get(ChatFields.LAST_SENDER_ID), ChatFields.LAST_SENDER_ID)
            : existing.lastSenderId();

    OffsetDateTime lastMessageAt =
        request.containsKey(ChatFields.LAST_MESSAGE_AT)
            ? HttpRequestParsers.optionalOffsetDateTime(
                request.get(ChatFields.LAST_MESSAGE_AT), ChatFields.LAST_MESSAGE_AT)
            : existing.lastMessageAt();

    String lastMessagePreview =
        request.containsKey(ChatFields.LAST_MESSAGE_PREVIEW)
            ? HttpRequestParsers.defaultIfBlank(
                HttpRequestParsers.optionalString(request.get(ChatFields.LAST_MESSAGE_PREVIEW)), "")
            : existing.lastMessagePreview();

    OffsetDateTime createdAt =
        request.containsKey(ChatFields.CREATED_AT)
            ? HttpRequestParsers.optionalOffsetDateTime(
                request.get(ChatFields.CREATED_AT), ChatFields.CREATED_AT)
            : existing.createdAt();

    OffsetDateTime updatedAt =
        request.containsKey(ChatFields.UPDATED_AT)
            ? HttpRequestParsers.optionalOffsetDateTime(
                request.get(ChatFields.UPDATED_AT), ChatFields.UPDATED_AT)
            : OffsetDateTime.now(ZoneOffset.UTC);

    String sql = """
        UPDATE chat_threads
        SET
          participant_user_id_1 = ?,
          participant_user_id_2 = ?,
          initiated_by_id = ?,
          initiated_by_role = CAST(? AS user_role_enum),
          last_sender_id = ?,
          last_message_at = ?,
          last_message_preview = ?,
          created_at = ?,
          updated_at = ?
        WHERE id = ?
        """;

    try {
      int updatedRows =
          jdbcTemplate.execute(
              (PreparedStatementCreator)
                  connection -> {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setLong(1, participantPair.first());
                    statement.setLong(2, participantPair.second());
                    statement.setLong(3, initiatedById);
                    statement.setString(4, initiatedByRole);
                    PreparedStatementBinder.setNullableLong(statement, 5, lastSenderId);
                    PreparedStatementBinder.setNullableOffsetDateTime(statement, 6, lastMessageAt);
                    statement.setString(7, lastMessagePreview);
                    statement.setObject(
                        8, createdAt != null ? createdAt : OffsetDateTime.now(ZoneOffset.UTC));
                    statement.setObject(
                        9, updatedAt != null ? updatedAt : OffsetDateTime.now(ZoneOffset.UTC));
                    statement.setLong(10, parsedId);
                    return statement;
                  },
              (PreparedStatementCallback<Integer>) PreparedStatement::executeUpdate);

      if (updatedRows == 0) {
        throw HttpErrors.notFound(CHAT_THREAD_NOT_FOUND_PREFIX + threadId);
      }

      return findChatThreadById(parsedId);
    } catch (DataIntegrityViolationException exception) {
      throw SqlErrorMapper.mapDataIntegrityViolation(
          exception,
          "Chat thread patch conflicts with existing records.",
          "Invalid chat thread patch payload.",
          HttpErrors::badRequest,
          HttpErrors::conflict);
    } catch (UncategorizedSQLException exception) {
      throw HttpErrors.badRequest(
          SqlErrorMapper.extractSqlErrorMessage(
              exception.getSQLException(), "Invalid chat thread patch payload."));
    }
  }

  public boolean isThreadParticipant(String threadId, long userId) {
    long parsedId = HttpRequestParsers.parsePathId(threadId, "threadId");
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_threads WHERE id = ? AND (participant_user_id_1 = ? OR participant_user_id_2 = ?)",
            Integer.class,
            parsedId,
            userId,
            userId);

    return count != null && count > 0;
  }

  public boolean isThreadParticipant(long threadId, long userId) {
    Integer count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM chat_threads WHERE id = ? AND (participant_user_id_1 = ? OR participant_user_id_2 = ?)",
            Integer.class,
            threadId,
            userId,
            userId);

    return count != null && count > 0;
  }

  private ChatThreadRow findChatThreadRow(long id) {
    List<ChatThreadRow> rows = jdbcTemplate.query(THREAD_SELECT + WHERE_ID_CLAUSE, this::mapChatThreadDataRow, id);
    if (rows.isEmpty()) {
      throw HttpErrors.notFound(CHAT_THREAD_NOT_FOUND_PREFIX + id);
    }

    return rows.get(0);
  }

  private Map<String, Object> findChatThreadById(long id) {
    List<Map<String, Object>> rows = jdbcTemplate.query(THREAD_SELECT + WHERE_ID_CLAUSE, this::mapChatThreadRow, id);
    if (rows.isEmpty()) {
      throw HttpErrors.notFound(CHAT_THREAD_NOT_FOUND_PREFIX + id);
    }

    return rows.get(0);
  }

  private ChatThreadRow mapChatThreadDataRow(ResultSet resultSet, int rowNumber) throws SQLException {
    Long lastSenderId = resultSet.getObject("last_sender_id", Long.class);

    return new ChatThreadRow(
        resultSet.getLong("id"),
        resultSet.getLong("participant_user_id_1"),
        resultSet.getLong("participant_user_id_2"),
        resultSet.getLong("initiated_by_id"),
        resultSet.getString("initiated_by_role"),
        lastSenderId,
        resultSet.getObject("last_message_at", OffsetDateTime.class),
        resultSet.getString("last_message_preview"),
        resultSet.getObject("created_at", OffsetDateTime.class),
        resultSet.getObject("updated_at", OffsetDateTime.class));
  }

  private Map<String, Object> mapChatThreadRow(ResultSet resultSet, int rowNumber) throws SQLException {
    ChatThreadRow data = mapChatThreadDataRow(resultSet, rowNumber);

    Map<String, Object> row = new LinkedHashMap<>();
    row.put(ChatFields.ID, String.valueOf(data.id()));
    row.put(
        ChatFields.PARTICIPANT_IDS,
        List.of(String.valueOf(data.participantUserId1()), String.valueOf(data.participantUserId2())));
    row.put(ChatFields.INITIATED_BY_ID, String.valueOf(data.initiatedById()));
    row.put(ChatFields.INITIATED_BY_ROLE, data.initiatedByRole());
    row.put(ChatFields.LAST_SENDER_ID, data.lastSenderId() == null ? null : String.valueOf(data.lastSenderId()));
    row.put(ChatFields.LAST_MESSAGE_AT, data.lastMessageAt());
    row.put(ChatFields.LAST_MESSAGE_PREVIEW, data.lastMessagePreview());
    row.put(ChatFields.CREATED_AT, data.createdAt());
    row.put(ChatFields.UPDATED_AT, data.updatedAt());
    return row;
  }

  private ParticipantPair requireParticipantPair(Object value) {
    if (!(value instanceof List<?> listValue)) {
      throw HttpErrors.badRequest("participantIds must be an array with exactly two unique user ids.");
    }

    if (listValue.size() != 2) {
      throw HttpErrors.badRequest("participantIds must contain exactly two user ids.");
    }

    long first = HttpRequestParsers.requireLong(listValue.get(0), ChatFields.PARTICIPANT_IDS_0);
    long second = HttpRequestParsers.requireLong(listValue.get(1), ChatFields.PARTICIPANT_IDS_1);

    if (first == second) {
      throw HttpErrors.badRequest("participantIds must contain two different user ids.");
    }

    long sortedFirst = Math.min(first, second);
    long sortedSecond = Math.max(first, second);
    return new ParticipantPair(sortedFirst, sortedSecond);
  }

  private String requireRole(Object value, String fieldName) {
    String normalized = HttpRequestParsers.requireNonBlank(value, fieldName).toUpperCase(Locale.ROOT);
    if (!"PLAYER".equals(normalized) && !"RECRUITER".equals(normalized) && !"ADMIN".equals(normalized)) {
      throw HttpErrors.badRequest(fieldName + " must be one of PLAYER, RECRUITER, or ADMIN.");
    }

    return normalized;
  }

  private void enforceInitiatorOwnership(Map<String, Object> request, long actorUserId) {
    if (!request.containsKey(ChatFields.INITIATED_BY_ID)
        || request.get(ChatFields.INITIATED_BY_ID) == null) {
      request.put(ChatFields.INITIATED_BY_ID, String.valueOf(actorUserId));
      return;
    }

    long payloadInitiatorId =
        parseLongValue(request.get(ChatFields.INITIATED_BY_ID), ChatFields.INITIATED_BY_ID);
    if (payloadInitiatorId != actorUserId) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You can only initiate conversations as yourself.");
    }
  }

  private boolean payloadContainsParticipant(Object participantIdsValue, long actorUserId) {
    if (!(participantIdsValue instanceof List<?> participantIds)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "participantIds must be an array with two entries.");
    }

    if (participantIds.size() != 2) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "participantIds must contain exactly two user ids.");
    }

    long first = parseLongValue(participantIds.get(0), ChatFields.PARTICIPANT_IDS_0);
    long second = parseLongValue(participantIds.get(1), ChatFields.PARTICIPANT_IDS_1);
    return first == actorUserId || second == actorUserId;
  }

  private void forbidFieldMutation(Map<String, Object> request, Set<String> forbiddenFields) {
    for (String field : forbiddenFields) {
      if (request.containsKey(field)) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Field '" + field + "' cannot be changed for this operation.");
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

  private record ParticipantPair(long first, long second) {}

  private record ChatThreadRow(
      long id,
      long participantUserId1,
      long participantUserId2,
      long initiatedById,
      String initiatedByRole,
      Long lastSenderId,
      OffsetDateTime lastMessageAt,
      String lastMessagePreview,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt) {}
}
