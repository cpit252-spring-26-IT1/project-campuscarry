package certifiedcarry_api.chat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import certifiedcarry_api.chat.ChatFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatApiModelsTest {

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
  }

  @Test
  void threadPatchRequestPreservesExplicitNullFields() throws Exception {
    ChatThreadPatchRequest request =
        objectMapper.readValue("{\"lastSenderId\":null}", ChatThreadPatchRequest.class);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertTrue(serviceRequest.containsKey(ChatFields.LAST_SENDER_ID));
    assertNull(serviceRequest.get(ChatFields.LAST_SENDER_ID));
    assertEquals(1, serviceRequest.size());
  }

  @Test
  void threadPatchRequestOmitsMissingFields() throws Exception {
    ChatThreadPatchRequest request =
        objectMapper.readValue("{}", ChatThreadPatchRequest.class);

    assertTrue(request.toServiceRequest().isEmpty());
  }

  @Test
  void messagePatchRequestPreservesExplicitNullFields() throws Exception {
    ChatMessagePatchRequest request =
        objectMapper.readValue("{\"readAt\":null}", ChatMessagePatchRequest.class);

    Map<String, Object> serviceRequest = request.toServiceRequest();

    assertTrue(serviceRequest.containsKey(ChatFields.READ_AT));
    assertNull(serviceRequest.get(ChatFields.READ_AT));
    assertEquals(1, serviceRequest.size());
  }

  @Test
  void messageResponseKeepsExistingJsonFieldTypes() {
    ChatMessageResponse response =
        ChatMessageResponse.fromServiceRow(
            Map.of(
                ChatFields.ID, "12",
                ChatFields.THREAD_ID, "4",
                ChatFields.SENDER_ID, "7",
                ChatFields.RECIPIENT_ID, "8",
                ChatFields.BODY, "hello"));

    assertEquals("12", response.id());
    assertEquals("4", response.threadId());
    assertEquals("7", response.senderId());
    assertEquals("8", response.recipientId());
    assertEquals("hello", response.body());
    assertNull(response.createdAt());
    assertNull(response.readAt());
  }

  @Test
  void chatCreateRequestsAndConversationSummaryMapFields() {
    OffsetDateTime now = OffsetDateTime.parse("2026-05-13T01:00:00Z");
    ChatThreadCreateRequest threadRequest =
        new ChatThreadCreateRequest(
            List.of("7", "8"), "7", "PLAYER", "7", now, "hello", now, now);
    ChatMessageCreateRequest messageRequest =
        new ChatMessageCreateRequest("4", "7", "8", "hello", now, now);

    Map<String, Object> threadPayload = threadRequest.toServiceRequest();
    Map<String, Object> messagePayload = messageRequest.toServiceRequest();
    ConversationSummaryResponse summary =
        ConversationSummaryResponse.fromServiceRow(
            Map.of(
                ChatFields.ID, "4",
                ChatFields.PARTICIPANT_IDS, List.of("7", "8"),
                ChatFields.LAST_SENDER_ID, "7",
                ChatFields.LAST_MESSAGE_AT, now,
                ChatFields.LAST_MESSAGE_PREVIEW, "hello",
                ChatFields.CREATED_AT, now,
                ChatFields.UPDATED_AT, now,
                "unreadCount", 3L));

    assertEquals(List.of("7", "8"), threadPayload.get(ChatFields.PARTICIPANT_IDS));
    assertEquals("hello", threadPayload.get(ChatFields.LAST_MESSAGE_PREVIEW));
    assertEquals("4", messagePayload.get(ChatFields.THREAD_ID));
    assertEquals(now, messagePayload.get(ChatFields.READ_AT));
    assertEquals(3, summary.unreadCount());
    assertEquals(List.of("7", "8"), summary.participantIds());
  }

  @Test
  void patchRequestsTrackMultipleExplicitValues() throws Exception {
    ChatThreadPatchRequest threadRequest =
        objectMapper.readValue(
            """
            {
              "participantIds": ["7", "8"],
              "initiatedByRole": "PLAYER",
              "lastMessagePreview": "updated"
            }
            """,
            ChatThreadPatchRequest.class);
    ChatMessagePatchRequest messageRequest =
        objectMapper.readValue(
            """
            {
              "threadId": "4",
              "body": "updated",
              "readAt": null
            }
            """,
            ChatMessagePatchRequest.class);

    assertEquals(List.of("7", "8"), threadRequest.toServiceRequest().get(ChatFields.PARTICIPANT_IDS));
    assertEquals("PLAYER", threadRequest.toServiceRequest().get(ChatFields.INITIATED_BY_ROLE));
    assertEquals("4", messageRequest.toServiceRequest().get(ChatFields.THREAD_ID));
    assertTrue(messageRequest.toServiceRequest().containsKey(ChatFields.READ_AT));
  }

  @Test
  void patchRequestsTrackEveryThreadAndMessageFieldType() throws Exception {
    ChatThreadPatchRequest threadRequest =
        objectMapper.readValue(
            """
            {
              "initiatedById": "7",
              "lastSenderId": "8",
              "lastMessageAt": "2026-05-13T00:00:00Z",
              "createdAt": "2026-05-12T23:00:00Z",
              "updatedAt": "2026-05-13T01:00:00Z"
            }
            """,
            ChatThreadPatchRequest.class);
    ChatMessagePatchRequest messageRequest =
        objectMapper.readValue(
            """
            {
              "senderId": "7",
              "recipientId": "8",
              "createdAt": "2026-05-13T00:00:00Z"
            }
            """,
            ChatMessagePatchRequest.class);

    assertEquals("7", threadRequest.toServiceRequest().get(ChatFields.INITIATED_BY_ID));
    assertEquals("8", threadRequest.toServiceRequest().get(ChatFields.LAST_SENDER_ID));
    assertEquals(OffsetDateTime.parse("2026-05-13T00:00:00Z"), threadRequest.toServiceRequest().get(ChatFields.LAST_MESSAGE_AT));
    assertEquals(OffsetDateTime.parse("2026-05-12T23:00:00Z"), threadRequest.toServiceRequest().get(ChatFields.CREATED_AT));
    assertEquals("7", messageRequest.toServiceRequest().get(ChatFields.SENDER_ID));
    assertEquals("8", messageRequest.toServiceRequest().get(ChatFields.RECIPIENT_ID));
    assertEquals(OffsetDateTime.parse("2026-05-13T00:00:00Z"), messageRequest.toServiceRequest().get(ChatFields.CREATED_AT));
  }

  @Test
  void directSetterCoverageBuildsCompletePatchPayloads() {
    OffsetDateTime now = OffsetDateTime.parse("2026-05-13T00:00:00Z");
    ChatThreadPatchRequest threadPatch = new ChatThreadPatchRequest();
    threadPatch.setParticipantIds(List.of("7", "8"));
    threadPatch.setInitiatedById("7");
    threadPatch.setInitiatedByRole("PLAYER");
    threadPatch.setLastSenderId("8");
    threadPatch.setLastMessageAt(now);
    threadPatch.setLastMessagePreview("preview");
    threadPatch.setCreatedAt(now.minusHours(1));
    threadPatch.setUpdatedAt(now.plusHours(1));

    ChatMessagePatchRequest messagePatch = new ChatMessagePatchRequest();
    messagePatch.setThreadId("4");
    messagePatch.setSenderId("7");
    messagePatch.setRecipientId("8");
    messagePatch.setBody("hello");
    messagePatch.setCreatedAt(now);
    messagePatch.setReadAt(now.plusMinutes(5));

    assertEquals(8, threadPatch.toServiceRequest().size());
    assertEquals(List.of("7", "8"), threadPatch.toServiceRequest().get(ChatFields.PARTICIPANT_IDS));
    assertEquals("hello", messagePatch.toServiceRequest().get(ChatFields.BODY));
    assertEquals(now.plusMinutes(5), messagePatch.toServiceRequest().get(ChatFields.READ_AT));
  }
}
