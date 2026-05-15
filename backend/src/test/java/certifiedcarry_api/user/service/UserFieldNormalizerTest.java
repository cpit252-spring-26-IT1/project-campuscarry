package certifiedcarry_api.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class UserFieldNormalizerTest {

  @Test
  void parsesIdsAndNormalizesTextAndEmails() {
    assertEquals(7L, UserFieldNormalizer.parseUserId("7", "invalid id"));
    assertEquals("bluy", UserFieldNormalizer.requireNonBlank("  bluy ", "required"));
    assertEquals("player@example.com", UserFieldNormalizer.normalizeEmail(" Player@Example.com ", "blank"));
    assertEquals("player@example.com", UserFieldNormalizer.normalizeOptionalEmail(" Player@Example.com "));
    assertNull(UserFieldNormalizer.normalizeOptionalEmail("  "));
  }

  @Test
  void normalizesLinkedinUrlsAndRejectsInvalidValues() {
    assertEquals(
        "https://linkedin.com/in/bluy",
        UserFieldNormalizer.normalizeLinkedinUrl(
            "https://linkedin.com/in/bluy", "linkedinUrl cannot be blank"));

    ResponseStatusException blankFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> UserFieldNormalizer.normalizeEmail("   ", "email cannot be blank"));
    assertEquals("email cannot be blank", blankFailure.getReason());

    ResponseStatusException invalidLinkedinFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                UserFieldNormalizer.normalizeLinkedinUrl(
                    "https://example.com/user", "linkedinUrl cannot be blank"));
    assertEquals("linkedinUrl must point to linkedin.com.", invalidLinkedinFailure.getReason());
  }
}
