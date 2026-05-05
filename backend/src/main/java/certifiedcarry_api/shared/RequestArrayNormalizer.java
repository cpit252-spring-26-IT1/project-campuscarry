package certifiedcarry_api.shared;

import java.util.ArrayList;
import java.util.List;

public final class RequestArrayNormalizer {

  private RequestArrayNormalizer() {
  }

  public static List<String> normalizeStringArray(Object value, String fieldName) {
    if (value == null) {
      return List.of();
    }

    if (value instanceof List<?> values) {
      List<String> normalized = new ArrayList<>();
      for (Object candidate : values) {
        String cleaned = HttpRequestParsers.optionalString(candidate);
        if (cleaned != null && !cleaned.isBlank()) {
          normalized.add(cleaned);
        }
      }
      return normalized;
    }

    throw HttpErrors.badRequest("Expected " + fieldName + " to be an array.");
  }
}
