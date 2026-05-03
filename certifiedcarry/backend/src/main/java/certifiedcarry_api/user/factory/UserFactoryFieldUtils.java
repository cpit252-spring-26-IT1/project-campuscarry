package certifiedcarry_api.user.factory;

final class UserFactoryFieldUtils {

  private UserFactoryFieldUtils() {}

  static String requireText(String value, String message) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(message);
    }

    return value.trim();
  }

  static String normalizeEmail(String email) {
    return email.trim().toLowerCase();
  }
}