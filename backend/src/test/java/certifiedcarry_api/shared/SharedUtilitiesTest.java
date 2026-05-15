package certifiedcarry_api.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

class SharedUtilitiesTest {

  @Test
  void normalizesStringArraysAndRejectsWrongShape() {
    assertEquals(
        List.of("duelist", "support"),
        RequestArrayNormalizer.normalizeStringArray(
            new ArrayList<>(java.util.Arrays.asList(" duelist ", "", null, "support")), "roles"));

    ResponseStatusException failure =
        assertThrows(
            ResponseStatusException.class,
            () -> RequestArrayNormalizer.normalizeStringArray("bad", "roles"));
    assertEquals("Expected roles to be an array.", failure.getReason());
  }

  @Test
  void normalizesStatusesWithDefaultsAndValidation() {
    assertEquals(
        "PENDING",
        RequestStatusNormalizer.normalize(
            "pending",
            true,
            "NOT_SUBMITTED",
            "status is required",
            Set.of("PENDING", "APPROVED"),
            "invalid status",
            HttpErrors::badRequest));
    assertEquals(
        "NOT_SUBMITTED",
        RequestStatusNormalizer.normalize(
            "   ",
            true,
            "NOT_SUBMITTED",
            "status is required",
            Set.of("PENDING", "APPROVED"),
            "invalid status",
            HttpErrors::badRequest));
    assertNull(
        RequestStatusNormalizer.normalize(
            "",
            false,
            "IGNORED",
            null,
            Set.of("PENDING", "APPROVED"),
            "invalid status",
            HttpErrors::badRequest));

    ResponseStatusException invalidFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                RequestStatusNormalizer.normalize(
                    "DECLINED",
                    false,
                    "IGNORED",
                    "status is required",
                    Set.of("PENDING", "APPROVED"),
                    "invalid status",
                    HttpErrors::badRequest));
    assertEquals("invalid status", invalidFailure.getReason());
  }

  @Test
  void normalizesGameAliasesAndLinkedinUrls() {
    assertEquals("LoL", GameNameAlias.normalize("league of legends"));
    assertEquals("Valorant", GameNameAlias.normalize("Valorant"));
    assertNull(GameNameAlias.normalizeNullable(null));

    assertEquals(
        "https://www.linkedin.com/in/test-user",
        LinkedinUrlNormalizer.normalizeValidatedLinkedinUrl(
            "https://www.linkedin.com/in/test-user", HttpErrors::badRequest));

    ResponseStatusException failure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                LinkedinUrlNormalizer.normalizeValidatedLinkedinUrl(
                    "https://example.com/user", HttpErrors::badRequest));
    assertEquals("linkedinUrl must point to linkedin.com.", failure.getReason());

    ResponseStatusException invalidScheme =
        assertThrows(
            ResponseStatusException.class,
            () ->
                LinkedinUrlNormalizer.normalizeValidatedLinkedinUrl(
                    "ftp://linkedin.com/in/test-user", HttpErrors::badRequest));
    assertEquals("linkedinUrl must start with http:// or https://.", invalidScheme.getReason());

    ResponseStatusException missingPath =
        assertThrows(
            ResponseStatusException.class,
            () ->
                LinkedinUrlNormalizer.normalizeValidatedLinkedinUrl(
                    "https://linkedin.com/", HttpErrors::badRequest));
    assertEquals(
        "linkedinUrl must include a LinkedIn profile or company path.", missingPath.getReason());
  }

  @Test
  void parsesRequestAttributesAcrossSupportedValueTypes() {
    assertNull(RequestAttributeParser.attributeAsString("   "));
    assertEquals("value", RequestAttributeParser.attributeAsString(" value "));
    assertEquals(17L, RequestAttributeParser.attributeAsLong(17));
    assertEquals(18L, RequestAttributeParser.attributeAsLong(" 18 "));
    assertNull(RequestAttributeParser.attributeAsLong("not-a-number"));
    assertEquals(true, RequestAttributeParser.attributeAsBoolean(Boolean.TRUE));
    assertEquals(false, RequestAttributeParser.attributeAsBoolean(" false "));
    assertNull(RequestAttributeParser.attributeAsBoolean(7));
  }

  @Test
  void mapsSqlErrorsAndExtractsUsefulMessages() {
    DataIntegrityViolationException duplicateKey =
        new DataIntegrityViolationException(
            "wrapper",
            new RuntimeException("ERROR: duplicate key value violates unique constraint\n  Detail: boom"));

    ResponseStatusException duplicateFailure =
        SqlErrorMapper.mapDataIntegrityViolation(
            duplicateKey,
            "already exists",
            "invalid payload",
            HttpErrors::badRequest,
            HttpErrors::conflict);
    assertEquals("already exists", duplicateFailure.getReason());

    DataIntegrityViolationException generic =
        new DataIntegrityViolationException(
            "wrapper",
            new RuntimeException("ERROR: invalid input syntax for type bigint\n  Where: statement"));

    ResponseStatusException genericFailure =
        SqlErrorMapper.mapDataIntegrityViolation(
            generic,
            "already exists",
            "invalid payload",
            HttpErrors::badRequest,
            HttpErrors::conflict);
    assertEquals("invalid input syntax for type bigint", genericFailure.getReason());

    assertEquals(
        "fallback",
        SqlErrorMapper.extractSqlErrorMessage(null, "fallback"));
  }

  @Test
  void bindsNullableJdbcValues() throws Exception {
    PreparedStatement statement = mock(PreparedStatement.class);
    OffsetDateTime timestamp = OffsetDateTime.parse("2026-05-13T02:00:00Z");

    PreparedStatementBinder.setNullableString(statement, 1, null);
    PreparedStatementBinder.setNullableString(statement, 2, "hello");
    PreparedStatementBinder.setNullableBigDecimal(statement, 3, null);
    PreparedStatementBinder.setNullableBigDecimal(statement, 4, new BigDecimal("15.5"));
    PreparedStatementBinder.setNullableOffsetDateTime(statement, 5, null);
    PreparedStatementBinder.setNullableOffsetDateTime(statement, 6, timestamp);
    PreparedStatementBinder.setNullableLong(statement, 7, null);
    PreparedStatementBinder.setNullableLong(statement, 8, 99L);

    verify(statement).setNull(1, Types.VARCHAR);
    verify(statement).setString(2, "hello");
    verify(statement).setNull(3, Types.NUMERIC);
    verify(statement).setBigDecimal(4, new BigDecimal("15.5"));
    verify(statement).setNull(5, Types.TIMESTAMP_WITH_TIMEZONE);
    verify(statement).setObject(6, timestamp);
    verify(statement).setNull(7, Types.BIGINT);
    verify(statement).setLong(8, 99L);
  }
}
