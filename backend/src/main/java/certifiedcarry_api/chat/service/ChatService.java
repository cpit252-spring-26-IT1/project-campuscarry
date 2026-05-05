package certifiedcarry_api.chat.service;

import certifiedcarry_api.chat.ChatFields;
import certifiedcarry_api.shared.HttpErrors;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ChatService {

  private final ChatThreadService chatThreadService;
  private final ChatMessageService chatMessageService;

  public ChatService(ChatThreadService chatThreadService, ChatMessageService chatMessageService) {
    this.chatThreadService = chatThreadService;
    this.chatMessageService = chatMessageService;
  }

  public List<Map<String, Object>> getChatThreadsForActor(long actorUserId, boolean isAdmin) {
    return chatThreadService.getChatThreadsForActor(actorUserId, isAdmin);
  }

  public List<Map<String, Object>> getChatMessagesForActor(long actorUserId, boolean isAdmin) {
    return chatMessageService.getChatMessagesForActor(actorUserId, isAdmin);
  }

  public List<Map<String, Object>> getConversationSummariesForActor(
      long actorUserId, boolean isAdmin) {
    List<Map<String, Object>> threads = chatThreadService.getChatThreadsForActor(actorUserId, isAdmin);
    List<Map<String, Object>> messages =
        chatMessageService.getChatMessagesForActor(actorUserId, isAdmin);

    Map<String, ConversationAccumulator> conversationsByPairKey = new LinkedHashMap<>();
    Map<String, String> pairKeyByThreadId = new LinkedHashMap<>();

    for (Map<String, Object> thread : threads) {
      String pairKey = getPairKey(thread);
      if (pairKey == null) {
        continue;
      }

      ConversationAccumulator accumulator =
          conversationsByPairKey.computeIfAbsent(pairKey, ignored -> new ConversationAccumulator());
      accumulator.acceptThread(thread);
      String resolvedThreadId = stringValue(thread.get(ChatFields.ID));
      if (resolvedThreadId != null) {
        pairKeyByThreadId.put(resolvedThreadId, pairKey);
      }
    }

    for (Map<String, Object> message : messages) {
      String threadId = stringValue(message.get(ChatFields.THREAD_ID));
      if (threadId == null) {
        continue;
      }

      String pairKey = pairKeyByThreadId.get(threadId);
      if (pairKey == null) {
        continue;
      }

      conversationsByPairKey.get(pairKey).acceptMessage(message, actorUserId);
    }

    return conversationsByPairKey.values().stream()
        .map(ConversationAccumulator::toSummaryRow)
        .sorted(
            Comparator.<Map<String, Object>, Long>comparing(
                    (row) -> timestampValue(row.get(ChatFields.LAST_MESSAGE_AT), row.get(ChatFields.UPDATED_AT),
                        row.get(ChatFields.CREATED_AT)))
                .reversed()
                .thenComparing(
                    (row) -> stringValue(row.get(ChatFields.ID)),
                    Comparator.nullsLast(Comparator.reverseOrder())))
        .toList();
  }

  public List<Map<String, Object>> getConversationMessagesForActor(
      String threadId, long actorUserId, boolean isAdmin) {
    List<Map<String, Object>> threads = chatThreadService.getChatThreadsForActor(actorUserId, isAdmin);
    Map<String, Object> requestedThread =
        threads.stream()
            .filter((candidate) -> threadId.equals(stringValue(candidate.get(ChatFields.ID))))
            .findFirst()
            .orElseThrow(() -> HttpErrors.notFound("Chat thread not found for id " + threadId));

    String pairKey = getPairKey(requestedThread);
    if (pairKey == null) {
      return List.of();
    }

    Set<String> threadIds =
        threads.stream()
            .filter((candidate) -> pairKey.equals(getPairKey(candidate)))
            .map((candidate) -> stringValue(candidate.get(ChatFields.ID)))
            .filter((candidateId) -> candidateId != null && !candidateId.isBlank())
            .collect(java.util.stream.Collectors.toSet());

    return chatMessageService.getChatMessagesForActor(actorUserId, isAdmin).stream()
        .filter((message) -> threadIds.contains(stringValue(message.get(ChatFields.THREAD_ID))))
        .sorted(
            Comparator.<Map<String, Object>, Long>comparing(
                    (row) -> timestampValue(row.get(ChatFields.CREATED_AT), null, null))
                .thenComparing((row) -> stringValue(row.get(ChatFields.ID)), Comparator.nullsLast(String::compareTo)))
        .toList();
  }

  @Transactional
  public Map<String, Object> createChatThreadForActor(
      Map<String, Object> request, long actorUserId, boolean isAdmin) {
    return chatThreadService.createChatThreadForActor(request, actorUserId, isAdmin);
  }

  @Transactional
  public Map<String, Object> patchChatThreadForActor(
      String threadId, Map<String, Object> request, long actorUserId, boolean isAdmin) {
    return chatThreadService.patchChatThreadForActor(threadId, request, actorUserId, isAdmin);
  }

  @Transactional
  public Map<String, Object> createChatMessageForActor(
      Map<String, Object> request, long actorUserId, boolean isAdmin) {
    return chatMessageService.createChatMessageForActor(request, actorUserId, isAdmin);
  }

  @Transactional
  public Map<String, Object> patchChatMessageForActor(
      String messageId, Map<String, Object> request, long actorUserId, boolean isAdmin) {
    return chatMessageService.patchChatMessageForActor(messageId, request, actorUserId, isAdmin);
  }

  public List<Map<String, Object>> getChatThreads() {
    return chatThreadService.getChatThreads();
  }

  public List<Map<String, Object>> getChatThreadsForUser(long userId) {
    return chatThreadService.getChatThreadsForUser(userId);
  }

  @Transactional
  public Map<String, Object> createChatThread(Map<String, Object> request) {
    return chatThreadService.createChatThread(request);
  }

  @Transactional
  public Map<String, Object> patchChatThread(String threadId, Map<String, Object> request) {
    return chatThreadService.patchChatThread(threadId, request);
  }

  public List<Map<String, Object>> getChatMessages() {
    return chatMessageService.getChatMessages();
  }

  public List<Map<String, Object>> getChatMessagesForUser(long userId) {
    return chatMessageService.getChatMessagesForUser(userId);
  }

  public boolean isThreadParticipant(String threadId, long userId) {
    return chatThreadService.isThreadParticipant(threadId, userId);
  }

  public boolean isThreadParticipant(long threadId, long userId) {
    return chatThreadService.isThreadParticipant(threadId, userId);
  }

  public boolean isMessageRecipient(String messageId, long userId) {
    return chatMessageService.isMessageRecipient(messageId, userId);
  }

  @Transactional
  public Map<String, Object> createChatMessage(Map<String, Object> request) {
    return chatMessageService.createChatMessage(request);
  }

  @Transactional
  public Map<String, Object> patchChatMessage(String messageId, Map<String, Object> request) {
    return chatMessageService.patchChatMessage(messageId, request);
  }

  private String getPairKey(Map<String, Object> thread) {
    Object participantIdsValue = thread.get(ChatFields.PARTICIPANT_IDS);
    if (!(participantIdsValue instanceof List<?> participantIds) || participantIds.size() != 2) {
      return null;
    }

    String firstParticipantId = stringValue(participantIds.get(0));
    String secondParticipantId = stringValue(participantIds.get(1));
    if (firstParticipantId == null || secondParticipantId == null) {
      return null;
    }

    return firstParticipantId.compareTo(secondParticipantId) <= 0
        ? firstParticipantId + "::" + secondParticipantId
        : secondParticipantId + "::" + firstParticipantId;
  }

  private String stringValue(Object value) {
    if (value == null) {
      return null;
    }

    String normalized = String.valueOf(value).trim();
    return normalized.isBlank() ? null : normalized;
  }

  private long timestampValue(Object primary, Object secondary, Object tertiary) {
    return timestampValue(primary, timestampValue(secondary, timestampValue(tertiary, 0L)));
  }

  private long timestampValue(Object value, long fallback) {
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime.toInstant().toEpochMilli();
    }

    return fallback;
  }

  private final class ConversationAccumulator {

    private Map<String, Object> canonicalThread;
    private final Set<String> threadIds = new java.util.LinkedHashSet<>();
    private int unreadCount;

    private void acceptThread(Map<String, Object> thread) {
      if (canonicalThread == null
          || timestampValue(thread.get(ChatFields.LAST_MESSAGE_AT), thread.get(ChatFields.UPDATED_AT),
                  thread.get(ChatFields.CREATED_AT))
              > timestampValue(
                  canonicalThread.get(ChatFields.LAST_MESSAGE_AT),
                  canonicalThread.get(ChatFields.UPDATED_AT),
                  canonicalThread.get(ChatFields.CREATED_AT))) {
        canonicalThread = thread;
      }

      String resolvedThreadId = stringValue(thread.get(ChatFields.ID));
      if (resolvedThreadId != null) {
        threadIds.add(resolvedThreadId);
      }
    }

    private void acceptMessage(Map<String, Object> message, long actorUserId) {
      String recipientId = stringValue(message.get(ChatFields.RECIPIENT_ID));
      Object readAt = message.get(ChatFields.READ_AT);
      if (recipientId != null && recipientId.equals(String.valueOf(actorUserId)) && readAt == null) {
        unreadCount += 1;
      }
    }

    private Map<String, Object> toSummaryRow() {
      Map<String, Object> row = new LinkedHashMap<>(canonicalThread);
      row.put("unreadCount", unreadCount);
      return row;
    }
  }
}
