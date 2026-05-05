package certifiedcarry_api.shared;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public final class HttpRequestParsers {

  private HttpRequestParsers() {
  }

  public static long parsePathId(String value, String fieldName) {
    return RequestValueParser.parsePathId(value, fieldName, HttpErrors::badRequest);
  }

  public static long requireLong(Object value, String fieldName) {
    return RequestValueParser.requireLong(value, fieldName, HttpErrors::badRequest);
  }

  public static Long optionalLong(Object value, String fieldName) {
    return RequestValueParser.optionalLong(value, fieldName, HttpErrors::badRequest);
  }

  public static String requireNonBlank(Object value, String fieldName) {
    return RequestValueParser.requireNonBlank(value, fieldName, HttpErrors::badRequest);
  }

  public static String optionalString(Object value) {
    return RequestValueParser.optionalString(value);
  }

  public static String defaultIfBlank(String value, String fallback) {
    return RequestValueParser.defaultIfBlank(value, fallback);
  }

  public static BigDecimal optionalBigDecimal(Object value, String fieldName) {
    return RequestValueParser.optionalBigDecimal(value, fieldName, HttpErrors::badRequest);
  }

  public static boolean optionalBoolean(Object value, String fieldName, boolean fallback) {
    return RequestValueParser.optionalBoolean(value, fieldName, fallback, HttpErrors::badRequest);
  }

  public static OffsetDateTime optionalOffsetDateTime(Object value, String fieldName) {
    return RequestValueParser.optionalOffsetDateTime(value, fieldName, HttpErrors::badRequest);
  }

  public static OffsetDateTime requireOffsetDateTime(Object value, String fieldName) {
    OffsetDateTime parsed = optionalOffsetDateTime(value, fieldName);
    if (parsed == null) {
      throw HttpErrors.badRequest(fieldName + " is required.");
    }

    return parsed;
  }
}
