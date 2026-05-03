package certifiedcarry_api.shared;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.function.Function;
import org.springframework.web.server.ResponseStatusException;

public final class RequestValueParser {

  private RequestValueParser() {}

  public static long parsePathId(
      String value,
      String fieldName,
      Function<String, ResponseStatusException> badRequestFactory) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException exception) {
      throw badRequestFactory.apply(fieldName + " must be a numeric string.");
    }
  }

  public static long parseNumericStringLong(
      Object value,
      String fieldName,
      Function<String, ResponseStatusException> badRequestFactory) {
    if (value instanceof Number numericValue) {
      return numericValue.longValue();
    }

    try {
      return Long.parseLong(String.valueOf(value).trim());
    } catch (NumberFormatException exception) {
      throw badRequestFactory.apply(fieldName + " must be a numeric string.");
    }
  }

  public static long requireNumericStringLong(
      Object value,
      String fieldName,
      Function<String, ResponseStatusException> badRequestFactory) {
    if (value == null) {
      throw badRequestFactory.apply(fieldName + " is required.");
    }

    return parseNumericStringLong(value, fieldName, badRequestFactory);
  }

  public static long requireLong(
      Object value,
      String fieldName,
      Function<String, ResponseStatusException> badRequestFactory) {
    if (value == null) {
      throw badRequestFactory.apply(fieldName + " is required.");
    }

    if (value instanceof Number numericValue) {
      return numericValue.longValue();
    }

    try {
      return Long.parseLong(String.valueOf(value).trim());
    } catch (NumberFormatException exception) {
      throw badRequestFactory.apply(fieldName + " must be numeric.");
    }
  }

  public static Long optionalLong(
      Object value,
      String fieldName,
      Function<String, ResponseStatusException> badRequestFactory) {
    if (value == null) {
      return null;
    }

    if (value instanceof Number numericValue) {
      return numericValue.longValue();
    }

    String normalized = String.valueOf(value).trim();
    if (normalized.isEmpty()) {
      return null;
    }

    try {
      return Long.parseLong(normalized);
    } catch (NumberFormatException exception) {
      throw badRequestFactory.apply(fieldName + " must be numeric.");
    }
  }

  public static String requireNonBlank(
      Object value,
      String fieldName,
      Function<String, ResponseStatusException> badRequestFactory) {
    String normalized = optionalString(value);
    if (normalized == null || normalized.isBlank()) {
      throw badRequestFactory.apply(fieldName + " is required.");
    }

    return normalized;
  }

  public static String optionalString(Object value) {
    if (value == null) {
      return null;
    }

    String normalized = String.valueOf(value).trim();
    return normalized.isEmpty() ? null : normalized;
  }

  public static String defaultIfBlank(String value, String fallback) {
    if (value == null || value.isBlank()) {
      return fallback;
    }

    return value;
  }

  public static BigDecimal optionalBigDecimal(
      Object value,
      String fieldName,
      Function<String, ResponseStatusException> badRequestFactory) {
    if (value == null) {
      return null;
    }

    if (value instanceof BigDecimal decimalValue) {
      return decimalValue;
    }

    if (value instanceof Number numericValue) {
      return BigDecimal.valueOf(numericValue.doubleValue());
    }

    try {
      String normalized = String.valueOf(value).trim();
      if (normalized.isEmpty()) {
        return null;
      }

      return new BigDecimal(normalized);
    } catch (NumberFormatException exception) {
      throw badRequestFactory.apply(fieldName + " must be numeric.");
    }
  }

  public static boolean optionalBoolean(
      Object value,
      String fieldName,
      boolean fallback,
      Function<String, ResponseStatusException> badRequestFactory) {
    if (value == null) {
      return fallback;
    }

    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }

    String normalized = String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    if ("true".equals(normalized)) {
      return true;
    }

    if ("false".equals(normalized)) {
      return false;
    }

    throw badRequestFactory.apply(fieldName + " must be a boolean value.");
  }

  public static OffsetDateTime optionalOffsetDateTime(
      Object value,
      String fieldName,
      Function<String, ResponseStatusException> badRequestFactory) {
    if (value == null) {
      return null;
    }

    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime;
    }

    try {
      String normalized = String.valueOf(value).trim();
      if (normalized.isEmpty()) {
        return null;
      }

      return OffsetDateTime.parse(normalized);
    } catch (DateTimeParseException exception) {
      throw badRequestFactory.apply(fieldName + " must be a valid ISO-8601 timestamp.");
    }
  }
}