package certifiedcarry_api.shared;

public final class RequestAttributeParser {

  private RequestAttributeParser() {}

  public static String attributeAsString(Object value) {
    if (value == null) {
      return null;
    }

    String normalized = String.valueOf(value).trim();
    return normalized.isEmpty() ? null : normalized;
  }

  public static Long attributeAsLong(Object value) {
    if (value == null) {
      return null;
    }

    if (value instanceof Number numericValue) {
      return numericValue.longValue();
    }

    String normalized = attributeAsString(value);
    if (normalized == null) {
      return null;
    }

    try {
      return Long.parseLong(normalized);
    } catch (NumberFormatException exception) {
      return null;
    }
  }

  public static Boolean attributeAsBoolean(Object value) {
    if (value instanceof Boolean booleanValue) {
      return booleanValue;
    }

    if (value instanceof String stringValue) {
      return Boolean.parseBoolean(stringValue.trim());
    }

    return null;
  }
}
