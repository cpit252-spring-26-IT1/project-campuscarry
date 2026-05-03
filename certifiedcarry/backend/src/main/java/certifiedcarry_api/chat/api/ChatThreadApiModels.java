package certifiedcarry_api.chat.api;

import certifiedcarry_api.chat.ChatFields;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record ChatThreadCreateRequest(
    List<String> participantIds,
    String initiatedById,
    String initiatedByRole,
    String lastSenderId,
    OffsetDateTime lastMessageAt,
    String lastMessagePreview,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  Map<String, Object> toServiceRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put(ChatFields.PARTICIPANT_IDS, participantIds);
    request.put(ChatFields.INITIATED_BY_ID, initiatedById);
    request.put(ChatFields.INITIATED_BY_ROLE, initiatedByRole);
    request.put(ChatFields.LAST_SENDER_ID, lastSenderId);
    request.put(ChatFields.LAST_MESSAGE_AT, lastMessageAt);
    request.put(ChatFields.LAST_MESSAGE_PREVIEW, lastMessagePreview);
    request.put(ChatFields.CREATED_AT, createdAt);
    request.put(ChatFields.UPDATED_AT, updatedAt);
    return request;
  }
}

final class ChatThreadPatchRequest {

  private List<String> participantIds;
  private boolean participantIdsSet;
  private String initiatedById;
  private boolean initiatedByIdSet;
  private String initiatedByRole;
  private boolean initiatedByRoleSet;
  private String lastSenderId;
  private boolean lastSenderIdSet;
  private OffsetDateTime lastMessageAt;
  private boolean lastMessageAtSet;
  private String lastMessagePreview;
  private boolean lastMessagePreviewSet;
  private OffsetDateTime createdAt;
  private boolean createdAtSet;
  private OffsetDateTime updatedAt;
  private boolean updatedAtSet;

  @JsonSetter(ChatFields.PARTICIPANT_IDS)
  void setParticipantIds(List<String> participantIds) {
    this.participantIds = participantIds;
    participantIdsSet = true;
  }

  @JsonSetter(ChatFields.INITIATED_BY_ID)
  void setInitiatedById(String initiatedById) {
    this.initiatedById = initiatedById;
    initiatedByIdSet = true;
  }

  @JsonSetter(ChatFields.INITIATED_BY_ROLE)
  void setInitiatedByRole(String initiatedByRole) {
    this.initiatedByRole = initiatedByRole;
    initiatedByRoleSet = true;
  }

  @JsonSetter(ChatFields.LAST_SENDER_ID)
  void setLastSenderId(String lastSenderId) {
    this.lastSenderId = lastSenderId;
    lastSenderIdSet = true;
  }

  @JsonSetter(ChatFields.LAST_MESSAGE_AT)
  void setLastMessageAt(OffsetDateTime lastMessageAt) {
    this.lastMessageAt = lastMessageAt;
    lastMessageAtSet = true;
  }

  @JsonSetter(ChatFields.LAST_MESSAGE_PREVIEW)
  void setLastMessagePreview(String lastMessagePreview) {
    this.lastMessagePreview = lastMessagePreview;
    lastMessagePreviewSet = true;
  }

  @JsonSetter(ChatFields.CREATED_AT)
  void setCreatedAt(OffsetDateTime createdAt) {
    this.createdAt = createdAt;
    createdAtSet = true;
  }

  @JsonSetter(ChatFields.UPDATED_AT)
  void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    updatedAtSet = true;
  }

  Map<String, Object> toServiceRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    if (participantIdsSet) {
      request.put(ChatFields.PARTICIPANT_IDS, participantIds);
    }
    if (initiatedByIdSet) {
      request.put(ChatFields.INITIATED_BY_ID, initiatedById);
    }
    if (initiatedByRoleSet) {
      request.put(ChatFields.INITIATED_BY_ROLE, initiatedByRole);
    }
    if (lastSenderIdSet) {
      request.put(ChatFields.LAST_SENDER_ID, lastSenderId);
    }
    if (lastMessageAtSet) {
      request.put(ChatFields.LAST_MESSAGE_AT, lastMessageAt);
    }
    if (lastMessagePreviewSet) {
      request.put(ChatFields.LAST_MESSAGE_PREVIEW, lastMessagePreview);
    }
    if (createdAtSet) {
      request.put(ChatFields.CREATED_AT, createdAt);
    }
    if (updatedAtSet) {
      request.put(ChatFields.UPDATED_AT, updatedAt);
    }
    return request;
  }
}

record ChatThreadResponse(
    String id,
    List<String> participantIds,
    String initiatedById,
    String initiatedByRole,
    String lastSenderId,
    OffsetDateTime lastMessageAt,
    String lastMessagePreview,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {

  static ChatThreadResponse fromServiceRow(Map<String, Object> row) {
    return new ChatThreadResponse(
        (String) row.get(ChatFields.ID),
        castStringList(row.get(ChatFields.PARTICIPANT_IDS)),
        (String) row.get(ChatFields.INITIATED_BY_ID),
        (String) row.get(ChatFields.INITIATED_BY_ROLE),
        (String) row.get(ChatFields.LAST_SENDER_ID),
        (OffsetDateTime) row.get(ChatFields.LAST_MESSAGE_AT),
        (String) row.get(ChatFields.LAST_MESSAGE_PREVIEW),
        (OffsetDateTime) row.get(ChatFields.CREATED_AT),
        (OffsetDateTime) row.get(ChatFields.UPDATED_AT));
  }

  static List<ChatThreadResponse> fromServiceRows(List<Map<String, Object>> rows) {
    return rows.stream().map(ChatThreadResponse::fromServiceRow).toList();
  }

  @SuppressWarnings("unchecked")
  private static List<String> castStringList(Object value) {
    return (List<String>) value;
  }
}
