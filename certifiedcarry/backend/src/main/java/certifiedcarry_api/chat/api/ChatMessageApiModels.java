package certifiedcarry_api.chat.api;

import certifiedcarry_api.chat.ChatFields;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record ChatMessageCreateRequest(
    String threadId,
    String senderId,
    String recipientId,
    String body,
    OffsetDateTime createdAt,
    OffsetDateTime readAt) {

  Map<String, Object> toServiceRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put(ChatFields.THREAD_ID, threadId);
    request.put(ChatFields.SENDER_ID, senderId);
    request.put(ChatFields.RECIPIENT_ID, recipientId);
    request.put(ChatFields.BODY, body);
    request.put(ChatFields.CREATED_AT, createdAt);
    request.put(ChatFields.READ_AT, readAt);
    return request;
  }
}

final class ChatMessagePatchRequest {

  private String threadId;
  private boolean threadIdSet;
  private String senderId;
  private boolean senderIdSet;
  private String recipientId;
  private boolean recipientIdSet;
  private String body;
  private boolean bodySet;
  private OffsetDateTime createdAt;
  private boolean createdAtSet;
  private OffsetDateTime readAt;
  private boolean readAtSet;

  @JsonSetter(ChatFields.THREAD_ID)
  void setThreadId(String threadId) {
    this.threadId = threadId;
    threadIdSet = true;
  }

  @JsonSetter(ChatFields.SENDER_ID)
  void setSenderId(String senderId) {
    this.senderId = senderId;
    senderIdSet = true;
  }

  @JsonSetter(ChatFields.RECIPIENT_ID)
  void setRecipientId(String recipientId) {
    this.recipientId = recipientId;
    recipientIdSet = true;
  }

  @JsonSetter(ChatFields.BODY)
  void setBody(String body) {
    this.body = body;
    bodySet = true;
  }

  @JsonSetter(ChatFields.CREATED_AT)
  void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    createdAtSet = true;
  }

  @JsonSetter(ChatFields.READ_AT)
  void setReadAt(OffsetDateTime readAt) {
    this.readAt = readAt;
    readAtSet = true;
  }

  Map<String, Object> toServiceRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    if (threadIdSet) {
      request.put(ChatFields.THREAD_ID, threadId);
    }
    if (senderIdSet) {
      request.put(ChatFields.SENDER_ID, senderId);
    }
    if (recipientIdSet) {
      request.put(ChatFields.RECIPIENT_ID, recipientId);
    }
    if (bodySet) {
      request.put(ChatFields.BODY, body);
    }
    if (createdAtSet) {
      request.put(ChatFields.CREATED_AT, createdAt);
    }
    if (readAtSet) {
      request.put(ChatFields.READ_AT, readAt);
    }
    return request;
  }
}

record ChatMessageResponse(
    String id,
    String threadId,
    String senderId,
    String recipientId,
    String body,
    OffsetDateTime createdAt,
    OffsetDateTime readAt) {

  static ChatMessageResponse fromServiceRow(Map<String, Object> row) {
    return new ChatMessageResponse(
        (String) row.get(ChatFields.ID),
        (String) row.get(ChatFields.THREAD_ID),
        (String) row.get(ChatFields.SENDER_ID),
        (String) row.get(ChatFields.RECIPIENT_ID),
        (String) row.get(ChatFields.BODY),
        (OffsetDateTime) row.get(ChatFields.CREATED_AT),
        (OffsetDateTime) row.get(ChatFields.READ_AT));
  }

  static List<ChatMessageResponse> fromServiceRows(List<Map<String, Object>> rows) {
    return rows.stream().map(ChatMessageResponse::fromServiceRow).toList();
  }
}
