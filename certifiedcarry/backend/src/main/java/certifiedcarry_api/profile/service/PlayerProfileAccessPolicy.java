package certifiedcarry_api.profile.service;

import certifiedcarry_api.profile.PlayerProfileFields;
import certifiedcarry_api.shared.HttpErrors;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class PlayerProfileAccessPolicy {

  Map<String, Object> sanitizeProfileForActor(Map<String, Object> profile, long actorUserId) {
    long profileUserId = parseLongValue(profile.get(PlayerProfileFields.USER_ID), PlayerProfileFields.USER_ID);
    if (profileUserId == actorUserId) {
      return profile;
    }

    Map<String, Object> sanitized = new LinkedHashMap<>(profile);
    sanitized.put(PlayerProfileFields.PROOF_IMAGE, PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    sanitized.put(PlayerProfileFields.DECLINE_REASON, PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    sanitized.put(PlayerProfileFields.DECLINED_AT, null);
    return sanitized;
  }

  void enforcePayloadUserOwnership(Map<String, Object> request, long actorUserId) {
    if (!request.containsKey(PlayerProfileFields.USER_ID)
        || request.get(PlayerProfileFields.USER_ID) == null) {
      request.put(PlayerProfileFields.USER_ID, String.valueOf(actorUserId));
      return;
    }

    long payloadUserId =
        parseLongValue(request.get(PlayerProfileFields.USER_ID), PlayerProfileFields.USER_ID);
    if (payloadUserId != actorUserId) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "You can only submit updates for your own userId.");
    }
  }

  void stripAdminVerificationFields(Map<String, Object> request) {
    request.remove(PlayerProfileFields.RANK_VERIFICATION_STATUS);
    request.remove(PlayerProfileFields.IS_VERIFIED);
    request.remove(PlayerProfileFields.DECLINE_REASON);
    request.remove(PlayerProfileFields.DECLINED_AT);
    request.remove(PlayerProfileFields.RANK_VERIFIED_AT);
    request.remove(PlayerProfileFields.RANK_EXPIRES_AT);
    request.remove(PlayerProfileFields.RANK_EXPIRY_REMINDER_SENT_AT);
  }

  private long parseLongValue(Object value, String fieldName) {
    if (value instanceof Number numericValue) {
      return numericValue.longValue();
    }

    try {
      return Long.parseLong(String.valueOf(value).trim());
    } catch (NumberFormatException exception) {
      throw HttpErrors.badRequest(fieldName + " must be a numeric string.");
    }
  }
}
