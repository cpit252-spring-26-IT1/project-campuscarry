package certifiedcarry_api.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RequestValueParserTest {

  @Test
  void parsesPathAndNumericLongValues() {
    assertEquals(42L, RequestValueParser.parsePathId("42", "id", HttpErrors::badRequest));
    assertEquals(
        84L, RequestValueParser.parseNumericStringLong("84", "userId", HttpErrors::badRequest));
    assertEquals(
        126L, RequestValueParser.parseNumericStringLong(126, "userId", HttpErrors::badRequest));
  }

  @Test
  void rejectsInvalidPathAndNumericStrings() {
    ResponseStatusException pathFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> RequestValueParser.parsePathId("abc", "id", HttpErrors::badRequest));
    assertEquals("400 BAD_REQUEST", pathFailure.getStatusCode().toString());
    assertEquals("id must be a numeric string.", pathFailure.getReason());

    ResponseStatusException valueFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                RequestValueParser.parseNumericStringLong(
                    "  nope  ", "userId", HttpErrors::badRequest));
    assertEquals("userId must be a numeric string.", valueFailure.getReason());
  }

  @Test
  void requiresValuesAndParsesRequiredLongs() {
    assertEquals(17L, RequestValueParser.requireNumericStringLong("17", "id", HttpErrors::badRequest));
    assertEquals(18L, RequestValueParser.requireLong(18, "id", HttpErrors::badRequest));
    assertEquals(19L, RequestValueParser.requireLong("19", "id", HttpErrors::badRequest));

    ResponseStatusException requiredNumericFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> RequestValueParser.requireNumericStringLong(null, "id", HttpErrors::badRequest));
    assertEquals("id is required.", requiredNumericFailure.getReason());

    ResponseStatusException requiredLongFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> RequestValueParser.requireLong("bad", "id", HttpErrors::badRequest));
    assertEquals("id must be numeric.", requiredLongFailure.getReason());
  }

  @Test
  void handlesOptionalNumbersStringsAndFallbacks() {
    assertNull(RequestValueParser.optionalLong(null, "id", HttpErrors::badRequest));
    assertNull(RequestValueParser.optionalLong("   ", "id", HttpErrors::badRequest));
    assertEquals(8L, RequestValueParser.optionalLong("8", "id", HttpErrors::badRequest));
    assertEquals(9L, RequestValueParser.optionalLong(9, "id", HttpErrors::badRequest));
    assertNull(RequestValueParser.optionalString(null));
    assertNull(RequestValueParser.optionalString("   "));
    assertEquals("value", RequestValueParser.optionalString("  value "));
    assertEquals("fallback", RequestValueParser.defaultIfBlank(null, "fallback"));
    assertEquals("fallback", RequestValueParser.defaultIfBlank(" ", "fallback"));
    assertEquals("actual", RequestValueParser.defaultIfBlank("actual", "fallback"));
  }

  @Test
  void parsesBigDecimalBooleanAndOffsetDateTimeValues() {
    assertEquals(
        new BigDecimal("15.75"),
        RequestValueParser.optionalBigDecimal("15.75", "rating", HttpErrors::badRequest));
    assertEquals(
        BigDecimal.valueOf(4.5d),
        RequestValueParser.optionalBigDecimal(4.5d, "rating", HttpErrors::badRequest));
    assertNull(RequestValueParser.optionalBigDecimal("   ", "rating", HttpErrors::badRequest));

    assertEquals(true, RequestValueParser.optionalBoolean("true", "flag", false, HttpErrors::badRequest));
    assertEquals(false, RequestValueParser.optionalBoolean("FALSE", "flag", true, HttpErrors::badRequest));
    assertEquals(true, RequestValueParser.optionalBoolean(null, "flag", true, HttpErrors::badRequest));

    OffsetDateTime timestamp = OffsetDateTime.parse("2026-05-13T01:02:03Z");
    assertEquals(
        timestamp,
        RequestValueParser.optionalOffsetDateTime(
            "2026-05-13T01:02:03Z", "createdAt", HttpErrors::badRequest));
    assertEquals(
        timestamp,
        RequestValueParser.optionalOffsetDateTime(timestamp, "createdAt", HttpErrors::badRequest));
    assertNull(
        RequestValueParser.optionalOffsetDateTime("  ", "createdAt", HttpErrors::badRequest));
  }

  @Test
  void rejectsInvalidNonBlankBigDecimalBooleanAndOffsetDateTimeValues() {
    ResponseStatusException stringFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> RequestValueParser.requireNonBlank("   ", "username", HttpErrors::badRequest));
    assertEquals("username is required.", stringFailure.getReason());

    ResponseStatusException decimalFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> RequestValueParser.optionalBigDecimal("bad", "rating", HttpErrors::badRequest));
    assertEquals("rating must be numeric.", decimalFailure.getReason());

    ResponseStatusException booleanFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> RequestValueParser.optionalBoolean("maybe", "flag", false, HttpErrors::badRequest));
    assertEquals("flag must be a boolean value.", booleanFailure.getReason());

    ResponseStatusException dateFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                RequestValueParser.optionalOffsetDateTime(
                    "not-a-date", "createdAt", HttpErrors::badRequest));
    assertEquals("createdAt must be a valid ISO-8601 timestamp.", dateFailure.getReason());
  }
}
