package certifiedcarry_api.shared;

import java.util.Locale;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

public final class SqlErrorMapper {

  private SqlErrorMapper() {
  }

  public static ResponseStatusException mapDataIntegrityViolation(
      DataIntegrityViolationException exception,
      String conflictMessage,
      String badRequestFallbackMessage,
      Function<String, ResponseStatusException> badRequestFactory,
      Function<String, ResponseStatusException> conflictFactory) {
    String sqlMessage = extractSqlErrorMessage(exception.getMostSpecificCause(), badRequestFallbackMessage);
    String normalizedMessage = sqlMessage.toLowerCase(Locale.ROOT);

    if (normalizedMessage.contains("duplicate key")) {
      return conflictFactory.apply(conflictMessage);
    }

    return badRequestFactory.apply(sqlMessage);
  }

  public static String extractSqlErrorMessage(Throwable throwable, String fallbackMessage) {
    String message = extractSqlErrorMessagePreservingDetail(throwable, fallbackMessage);

    int detailSectionStart = message.indexOf("\n");
    if (detailSectionStart >= 0) {
      message = message.substring(0, detailSectionStart).trim();
    }

    return message.isBlank() ? fallbackMessage : message;
  }

  public static String extractSqlErrorMessagePreservingDetail(
      Throwable throwable, String fallbackMessage) {
    if (throwable == null || throwable.getMessage() == null) {
      return fallbackMessage;
    }

    String message = throwable.getMessage().replace("ERROR:", "").trim();

    int whereSectionStart = message.indexOf("\n  Where:");
    if (whereSectionStart >= 0) {
      message = message.substring(0, whereSectionStart).trim();
    }

    return message.isBlank() ? fallbackMessage : message;
  }
}