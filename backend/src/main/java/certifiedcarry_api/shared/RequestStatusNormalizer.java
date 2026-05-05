package certifiedcarry_api.shared;

import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import org.springframework.web.server.ResponseStatusException;

public final class RequestStatusNormalizer {

  private RequestStatusNormalizer() {
  }

  public static String normalize(
      String value,
      boolean defaultOnBlank,
      String defaultValue,
      String requiredMessage,
      Set<String> allowedValues,
      String invalidMessage,
      Function<String, ResponseStatusException> badRequestFactory) {
    if (value == null || value.isBlank()) {
      if (defaultOnBlank) {
        return defaultValue;
      }

      if (requiredMessage == null) {
        return null;
      }

      throw badRequestFactory.apply(requiredMessage);
    }

    String normalized = value.trim().toUpperCase(Locale.ROOT);
    if (!allowedValues.contains(normalized)) {
      throw badRequestFactory.apply(invalidMessage);
    }

    return normalized;
  }
}
