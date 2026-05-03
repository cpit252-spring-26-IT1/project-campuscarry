package certifiedcarry_api.user.service;

import certifiedcarry_api.shared.HttpErrors;
import certifiedcarry_api.shared.LinkedinUrlNormalizer;
import certifiedcarry_api.shared.TextNormalization;
import org.springframework.web.server.ResponseStatusException;

final class UserFieldNormalizer {

  private UserFieldNormalizer() {}

  static Long parseUserId(String value, String invalidMessage) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      throw HttpErrors.badRequest(invalidMessage);
    }
  }

  static String requireNonBlank(String value, String message) {
    String normalized = TextNormalization.trimToNull(value);
    if (normalized == null) {
      throw HttpErrors.badRequest(message);
    }

    return normalized;
  }

  static String normalizeEmail(String value, String blankMessage) {
    String normalized = normalizeOptionalEmail(value);
    if (normalized == null) {
      throw HttpErrors.badRequest(blankMessage);
    }

    return normalized;
  }

  static String normalizeOptionalEmail(String value) {
    String normalized = TextNormalization.trimToNull(value);
    if (normalized == null) {
      return null;
    }

    return normalized.toLowerCase(java.util.Locale.ROOT);
  }

  static String normalizeLinkedinUrl(String value, String blankMessage) {
    String normalized = requireNonBlank(value, blankMessage);
    return LinkedinUrlNormalizer.normalizeValidatedLinkedinUrl(normalized, HttpErrors::badRequest);
  }
}
