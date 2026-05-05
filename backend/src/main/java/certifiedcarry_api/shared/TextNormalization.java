package certifiedcarry_api.shared;

public final class TextNormalization {

  private TextNormalization() {}

  public static String trimToNull(String value) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim();
    return normalized.isEmpty() ? null : normalized;
  }
}