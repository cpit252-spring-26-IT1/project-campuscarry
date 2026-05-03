package certifiedcarry_api.chat.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import certifiedcarry_api.chat.ChatFields;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
}
