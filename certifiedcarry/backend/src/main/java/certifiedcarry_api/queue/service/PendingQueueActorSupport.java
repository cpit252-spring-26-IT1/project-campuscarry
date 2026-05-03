package certifiedcarry_api.queue.service;

import certifiedcarry_api.queue.PendingQueueFields;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class PendingQueueActorSupport {

  private static final String USER_OWNERSHIP_MESSAGE =
      "You can only submit updates for your own userId.";

  private PendingQueueActorSupport() {}

  static void enforcePayloadUserOwnership(Map<String, Object> request, long actorUserId) {
    if (!request.containsKey(PendingQueueFields.USER_ID)
        || request.get(PendingQueueFields.USER_ID) == null) {
      request.put(PendingQueueFields.USER_ID, String.valueOf(actorUserId));
      return;
    }

    long payloadUserId = parseLongValue(request.get(PendingQueueFields.USER_ID), PendingQueueFields.USER_ID);
    if (payloadUserId != actorUserId) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, USER_OWNERSHIP_MESSAGE);
    }
  }

  static long parseLongValue(Object value, String fieldName) {
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
}
