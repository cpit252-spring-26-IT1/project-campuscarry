package certifiedcarry_api.queue.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import certifiedcarry_api.queue.PendingQueueFields;
import certifiedcarry_api.shared.LegalVersionValidator;
import certifiedcarry_api.user.model.UserRole;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class PendingRecruiterServiceTest {

  @Mock
  private JdbcTemplate jdbcTemplate;

  private final LegalVersionValidator legalVersionValidator =
      new LegalVersionValidator("terms-v1", "privacy-v1");

  @Test
  void actorGuardsRestrictReadCreateAndDeleteAccess() {
    Map<String, Object> payload = Map.of(PendingQueueFields.ID, "3");

    PendingRecruiterService service =
        new PendingRecruiterService(jdbcTemplate, legalVersionValidator) {
          @Override
          public List<Map<String, Object>> getPendingRecruiters(String userId) {
            return List.of(Map.of(PendingQueueFields.USER_ID, userId == null ? "all" : userId));
          }

          @Override
          public Map<String, Object> createPendingRecruiter(Map<String, Object> request) {
            return request;
          }

          @Override
          public void deletePendingRecruiter(String pendingRecruiterId) {
            // no-op
          }
        };

    assertEquals(
        List.of(Map.of(PendingQueueFields.USER_ID, "9")),
        service.getPendingRecruitersForActor("9", 7L, UserRole.ADMIN.name(), true));
    assertEquals(
        List.of(Map.of(PendingQueueFields.USER_ID, "7")),
        service.getPendingRecruitersForActor(null, 7L, UserRole.RECRUITER.name(), false));

    ResponseStatusException readRoleFailure =
        assertThrows(
            ResponseStatusException.class,
            () -> service.getPendingRecruitersForActor(null, 7L, UserRole.PLAYER.name(), false));
    assertEquals(
        "Only recruiter or admin accounts can access pending recruiter records.",
        readRoleFailure.getReason());

    ResponseStatusException foreignReadFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.getPendingRecruitersForActor(
                    "9", 7L, UserRole.RECRUITER.name(), false));
    assertEquals("You can only access your own recruiter queue record.", foreignReadFailure.getReason());

    Map<String, Object> created =
        service.createPendingRecruiterForActor(new LinkedHashMap<>(), 7L, UserRole.RECRUITER.name(), false);
    assertEquals("7", created.get(PendingQueueFields.USER_ID));

    ResponseStatusException createRoleFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createPendingRecruiterForActor(
                    new LinkedHashMap<>(), 7L, UserRole.PLAYER.name(), false));
    assertEquals("Only recruiter accounts can create this queue record.", createRoleFailure.getReason());

    ResponseStatusException deleteFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.deletePendingRecruiterForActor(
                    "3", 7L, UserRole.RECRUITER.name(), false));
    assertEquals("Admin role is required.", deleteFailure.getReason());
    assertDoesNotThrow(
        () -> service.deletePendingRecruiterForActor("3", 7L, UserRole.ADMIN.name(), true));
  }

  @Test
  void queryCreateAndDeleteMapRowsAndFailures() throws SQLException {
    PendingRecruiterService service = new PendingRecruiterService(jdbcTemplate, legalVersionValidator);

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              return List.of(mapper.mapRow(mockPendingRecruiterResultSet(11L), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), eq(9L));

    List<Map<String, Object>> rows = service.getPendingRecruiters(" 9 ");
    assertEquals(1, rows.size());
    assertEquals("11", rows.get(0).get(PendingQueueFields.ID));
    assertEquals("recruiter@example.com", rows.get(0).get(PendingQueueFields.EMAIL));

    ResponseStatusException legalFailure =
        assertThrows(
            ResponseStatusException.class,
            () ->
                service.createPendingRecruiter(
                    validRecruiterRequest("old-terms", "old-privacy")));
    assertEquals(
        "Accepted legal versions are outdated. Refresh and accept the current policies.",
        legalFailure.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(new DataIntegrityViolationException("boom"));
    ResponseStatusException conflict =
        assertThrows(
            ResponseStatusException.class,
            () -> service.createPendingRecruiter(validRecruiterRequest("terms-v1", "privacy-v1")));
    assertEquals("Pending recruiter record already exists for this user.", conflict.getReason());

    when(jdbcTemplate.execute(any(PreparedStatementCreator.class), any(PreparedStatementCallback.class)))
        .thenThrow(
            new UncategorizedSQLException(
                "task", "sql", new SQLException("ERROR: invalid recruiter payload")));
    ResponseStatusException badRequest =
        assertThrows(
            ResponseStatusException.class,
            () -> service.createPendingRecruiter(validRecruiterRequest("terms-v1", "privacy-v1")));
    assertEquals("invalid recruiter payload", badRequest.getReason());

    when(jdbcTemplate.update(anyString(), eq(15L))).thenReturn(0);
    ResponseStatusException deleteFailure =
        assertThrows(ResponseStatusException.class, () -> service.deletePendingRecruiter("15"));
    assertEquals("Pending recruiter record not found for id 15", deleteFailure.getReason());
  }

  private Map<String, Object> validRecruiterRequest(
      String termsVersionAccepted, String privacyVersionAccepted) {
    return Map.of(
        PendingQueueFields.USER_ID, "7",
        PendingQueueFields.FULL_NAME, "Scout",
        PendingQueueFields.EMAIL, "Recruiter@Example.com",
        PendingQueueFields.LINKEDIN_URL, "https://linkedin.com/in/scout",
        PendingQueueFields.ORGANIZATION_NAME, "CertifiedCarry",
        PendingQueueFields.SUBMITTED_AT, "2026-05-13T00:00:00Z",
        PendingQueueFields.LEGAL_CONSENT_ACCEPTED_AT, "2026-05-13T00:01:00Z",
        PendingQueueFields.LEGAL_CONSENT_LOCALE, "en",
        PendingQueueFields.TERMS_VERSION_ACCEPTED, termsVersionAccepted,
        PendingQueueFields.PRIVACY_VERSION_ACCEPTED, privacyVersionAccepted);
  }

  private ResultSet mockPendingRecruiterResultSet(long id) throws SQLException {
    ResultSet resultSet = mock(ResultSet.class);
    OffsetDateTime submittedAt = OffsetDateTime.parse("2026-05-13T00:00:00Z");
    OffsetDateTime acceptedAt = OffsetDateTime.parse("2026-05-13T00:01:00Z");
    when(resultSet.getLong("id")).thenReturn(id);
    when(resultSet.getLong("user_id")).thenReturn(9L);
    when(resultSet.getString("full_name")).thenReturn("Scout");
    when(resultSet.getString("email")).thenReturn("recruiter@example.com");
    when(resultSet.getString("linkedin_url")).thenReturn("https://linkedin.com/in/scout");
    when(resultSet.getString("organization_name")).thenReturn("CertifiedCarry");
    when(resultSet.getObject("submitted_at", OffsetDateTime.class)).thenReturn(submittedAt);
    when(resultSet.getObject("legal_consent_accepted_at", OffsetDateTime.class)).thenReturn(acceptedAt);
    when(resultSet.getString("legal_consent_locale")).thenReturn("en");
    when(resultSet.getString("terms_version_accepted")).thenReturn("terms-v1");
    when(resultSet.getString("privacy_version_accepted")).thenReturn("privacy-v1");
    return resultSet;
  }
}
