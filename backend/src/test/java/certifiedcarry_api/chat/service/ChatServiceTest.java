package certifiedcarry_api.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import certifiedcarry_api.chat.ChatFields;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

  @Mock
  private ChatThreadService chatThreadService;

  @Mock
  private ChatMessageService chatMessageService;

  @InjectMocks
  private ChatService chatService;

  @Test
  void groupsConversationSummariesAcrossThreadsAndCountsUnreadMessages() {
    OffsetDateTime older = OffsetDateTime.parse("2026-05-12T10:00:00Z");
    OffsetDateTime newer = OffsetDateTime.parse("2026-05-13T10:00:00Z");

    when(chatThreadService.getChatThreadsForActor(13L, false))
        .thenReturn(
            List.of(
                threadRow("2", List.of("12", "13"), older, "old preview"),
                threadRow("9", List.of("13", "12"), newer, "new preview"),
                threadRow("10", List.of("bad"), newer, "ignored")));
    when(chatMessageService.getChatMessagesForActor(13L, false))
        .thenReturn(
            List.of(
                messageRow("m1", "2", "12", "13", null, older),
                messageRow("m2", "9", "13", "12", null, newer),
                messageRow("m3", "9", "12", "13", null, newer.plusMinutes(2)),
                messageRow("m4", "404", "12", "13", null, newer.plusMinutes(3)),
                messageRow("m5", "9", "12", "13", newer.plusMinutes(5), newer.plusMinutes(4))));

    List<Map<String, Object>> summaries = chatService.getConversationSummariesForActor(13L, false);

    assertEquals(1, summaries.size());
    Map<String, Object> summary = summaries.get(0);
    assertEquals("9", summary.get(ChatFields.ID));
    assertEquals("new preview", summary.get(ChatFields.LAST_MESSAGE_PREVIEW));
    assertEquals(2, summary.get("unreadCount"));
  }

  @Test
  void returnsConversationMessagesSortedAcrossMatchingParticipantThreads() {
    OffsetDateTime early = OffsetDateTime.parse("2026-05-13T10:00:00Z");
    OffsetDateTime late = OffsetDateTime.parse("2026-05-13T11:00:00Z");

    when(chatThreadService.getChatThreadsForActor(13L, false))
        .thenReturn(
            List.of(
                threadRow("2", List.of("12", "13"), early, "one"),
                threadRow("5", List.of("13", "12"), late, "two"),
                threadRow("7", List.of("13", "14"), late, "other")));
    when(chatMessageService.getChatMessagesForActor(13L, false))
        .thenReturn(
            List.of(
                messageRow("4", "5", "13", "12", null, late),
                messageRow("1", "2", "12", "13", null, early),
                messageRow("3", "7", "14", "13", null, late.minusMinutes(2))));

    List<Map<String, Object>> messages = chatService.getConversationMessagesForActor("2", 13L, false);

    assertEquals(2, messages.size());
    assertEquals("1", messages.get(0).get(ChatFields.ID));
    assertEquals("4", messages.get(1).get(ChatFields.ID));
  }

  @Test
  void throwsNotFoundWhenRequestedConversationIsMissing() {
    when(chatThreadService.getChatThreadsForActor(13L, false))
        .thenReturn(List.of(threadRow("2", List.of("12", "13"), OffsetDateTime.now(), "preview")));

    ResponseStatusException failure =
        assertThrows(
            ResponseStatusException.class,
            () -> chatService.getConversationMessagesForActor("99", 13L, false));

    assertEquals("Chat thread not found for id 99", failure.getReason());
  }

  @Test
  void delegatesActorScopedReadOperationsToUnderlyingServices() {
    List<Map<String, Object>> threadRows =
        List.of(threadRow("2", List.of("12", "13"), OffsetDateTime.parse("2026-05-13T10:00:00Z"), "preview"));
    List<Map<String, Object>> messageRows =
        List.of(
            messageRow(
                "4",
                "2",
                "12",
                "13",
                null,
                OffsetDateTime.parse("2026-05-13T11:00:00Z")));

    when(chatThreadService.getChatThreadsForActor(13L, true)).thenReturn(threadRows);
    when(chatMessageService.getChatMessagesForActor(13L, true)).thenReturn(messageRows);

    assertSame(threadRows, chatService.getChatThreadsForActor(13L, true));
    assertSame(messageRows, chatService.getChatMessagesForActor(13L, true));
  }

  @Test
  void delegatesRawReadOperationsAndOwnershipChecks() {
    List<Map<String, Object>> threadRows =
        List.of(threadRow("2", List.of("12", "13"), OffsetDateTime.parse("2026-05-13T10:00:00Z"), "preview"));
    List<Map<String, Object>> messageRows =
        List.of(
            messageRow(
                "4",
                "2",
                "12",
                "13",
                null,
                OffsetDateTime.parse("2026-05-13T11:00:00Z")));

    when(chatThreadService.getChatThreads()).thenReturn(threadRows);
    when(chatThreadService.getChatThreadsForUser(13L)).thenReturn(threadRows);
    when(chatMessageService.getChatMessages()).thenReturn(messageRows);
    when(chatMessageService.getChatMessagesForUser(13L)).thenReturn(messageRows);
    when(chatThreadService.isThreadParticipant("2", 13L)).thenReturn(true);
    when(chatThreadService.isThreadParticipant(2L, 13L)).thenReturn(false);
    when(chatMessageService.isMessageRecipient("4", 13L)).thenReturn(true);

    assertSame(threadRows, chatService.getChatThreads());
    assertSame(threadRows, chatService.getChatThreadsForUser(13L));
    assertSame(messageRows, chatService.getChatMessages());
    assertSame(messageRows, chatService.getChatMessagesForUser(13L));
    assertEquals(true, chatService.isThreadParticipant("2", 13L));
    assertEquals(false, chatService.isThreadParticipant(2L, 13L));
    assertEquals(true, chatService.isMessageRecipient("4", 13L));
  }

  @Test
  void delegatesMutationOperationsToUnderlyingServices() {
    Map<String, Object> threadRequest = Map.of(ChatFields.PARTICIPANT_IDS, List.of("12", "13"));
    Map<String, Object> messageRequest = Map.of(ChatFields.BODY, "hello");
    Map<String, Object> threadResponse =
        threadRow("2", List.of("12", "13"), OffsetDateTime.parse("2026-05-13T10:00:00Z"), "preview");
    Map<String, Object> messageResponse =
        messageRow(
            "4",
            "2",
            "12",
            "13",
            null,
            OffsetDateTime.parse("2026-05-13T11:00:00Z"));

    when(chatThreadService.createChatThreadForActor(threadRequest, 13L, false)).thenReturn(threadResponse);
    when(chatThreadService.patchChatThreadForActor("2", threadRequest, 13L, false)).thenReturn(threadResponse);
    when(chatMessageService.createChatMessageForActor(messageRequest, 13L, false)).thenReturn(messageResponse);
    when(chatMessageService.patchChatMessageForActor("4", messageRequest, 13L, false))
        .thenReturn(messageResponse);
    when(chatThreadService.createChatThread(threadRequest)).thenReturn(threadResponse);
    when(chatThreadService.patchChatThread("2", threadRequest)).thenReturn(threadResponse);
    when(chatMessageService.createChatMessage(messageRequest)).thenReturn(messageResponse);
    when(chatMessageService.patchChatMessage("4", messageRequest)).thenReturn(messageResponse);

    assertSame(threadResponse, chatService.createChatThreadForActor(threadRequest, 13L, false));
    assertSame(threadResponse, chatService.patchChatThreadForActor("2", threadRequest, 13L, false));
    assertSame(messageResponse, chatService.createChatMessageForActor(messageRequest, 13L, false));
    assertSame(messageResponse, chatService.patchChatMessageForActor("4", messageRequest, 13L, false));
    assertSame(threadResponse, chatService.createChatThread(threadRequest));
    assertSame(threadResponse, chatService.patchChatThread("2", threadRequest));
    assertSame(messageResponse, chatService.createChatMessage(messageRequest));
    assertSame(messageResponse, chatService.patchChatMessage("4", messageRequest));

    verify(chatThreadService).createChatThreadForActor(threadRequest, 13L, false);
    verify(chatThreadService).patchChatThreadForActor("2", threadRequest, 13L, false);
    verify(chatMessageService).createChatMessageForActor(messageRequest, 13L, false);
    verify(chatMessageService).patchChatMessageForActor("4", messageRequest, 13L, false);
    verify(chatThreadService).createChatThread(threadRequest);
    verify(chatThreadService).patchChatThread("2", threadRequest);
    verify(chatMessageService).createChatMessage(messageRequest);
    verify(chatMessageService).patchChatMessage("4", messageRequest);
  }

  private Map<String, Object> threadRow(
      String id, List<String> participants, OffsetDateTime timestamp, String preview) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(ChatFields.ID, id);
    row.put(ChatFields.PARTICIPANT_IDS, participants);
    row.put(ChatFields.INITIATED_BY_ID, participants.get(0));
    row.put(ChatFields.INITIATED_BY_ROLE, "PLAYER");
    row.put(ChatFields.LAST_SENDER_ID, participants.get(0));
    row.put(ChatFields.LAST_MESSAGE_AT, timestamp);
    row.put(ChatFields.LAST_MESSAGE_PREVIEW, preview);
    row.put(ChatFields.CREATED_AT, timestamp.minusHours(1));
    row.put(ChatFields.UPDATED_AT, timestamp);
    return row;
  }

  private Map<String, Object> messageRow(
      String id,
      String threadId,
      String senderId,
      String recipientId,
      OffsetDateTime readAt,
      OffsetDateTime createdAt) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(ChatFields.ID, id);
    row.put(ChatFields.THREAD_ID, threadId);
    row.put(ChatFields.SENDER_ID, senderId);
    row.put(ChatFields.RECIPIENT_ID, recipientId);
    row.put(ChatFields.BODY, "hello");
    row.put(ChatFields.CREATED_AT, createdAt);
    row.put(ChatFields.READ_AT, readAt);
    return row;
  }
}
