package certifiedcarry_api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import certifiedcarry_api.shared.LegalVersionValidator;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LegalVersionValidatorTest {

  private final LegalVersionValidator validator =
      new LegalVersionValidator("terms-v1", "privacy-v1");

  @Test
  void acceptsCurrentVersions() {
    assertDoesNotThrow(() -> validator.validateAcceptedVersions("terms-v1", "privacy-v1"));
  }

  @Test
  void rejectsOutdatedVersions() {
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> validator.validateAcceptedVersions("terms-v0", "privacy-v1"));

    assertEquals(400, exception.getStatusCode().value());
    assertEquals(
        "Accepted legal versions are outdated. Refresh and accept the current policies.",
        exception.getReason());
  }
}
