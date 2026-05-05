package certifiedcarry_api.queue.service;

import certifiedcarry_api.queue.PendingQueueFields;
import certifiedcarry_api.shared.HttpErrors;
import certifiedcarry_api.shared.HttpRequestParsers;
import certifiedcarry_api.shared.LegalVersionValidator;
import certifiedcarry_api.shared.LinkedinUrlNormalizer;
import certifiedcarry_api.user.model.UserRole;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PendingRecruiterService {

  private static final String ADMIN_ROLE_REQUIRED_MESSAGE = "Admin role is required.";
  private static final String RECRUITER_ONLY_MESSAGE =
      "Only recruiter accounts can create this queue record.";
  private static final String RECRUITER_OR_ADMIN_REQUIRED_MESSAGE =
      "Only recruiter or admin accounts can access pending recruiter records.";
  private static final String PENDING_RECRUITER_NOT_FOUND_PREFIX =
      "Pending recruiter record not found for id ";
  private static final String PENDING_RECRUITER_EXISTS_MESSAGE =
      "Pending recruiter record already exists for this user.";
  private static final String FAILED_CREATE_PENDING_RECRUITER_MESSAGE =
      "Failed to create pending recruiter record.";
  private static final String INVALID_PENDING_RECRUITER_PAYLOAD_MESSAGE =
      "Invalid pending recruiter payload.";

  private static final String PENDING_RECRUITER_SELECT_SQL = """
    SELECT
    id,
    user_id,
    full_name,
    email,
    linkedin_url,
    organization_name,
    submitted_at,
    legal_consent_accepted_at,
    legal_consent_locale,
    terms_version_accepted,
    privacy_version_accepted
    FROM pending_recruiters
    """;

  private static final String PENDING_RECRUITER_SELECT_BY_ID_SQL =
      PENDING_RECRUITER_SELECT_SQL + " WHERE id = ?";
  private static final String PENDING_RECRUITER_SELECT_BY_USER_ID_SQL =
      PENDING_RECRUITER_SELECT_SQL + " WHERE user_id = ?";

  private final JdbcTemplate jdbcTemplate;
  private final LegalVersionValidator legalVersionValidator;

  public PendingRecruiterService(
      JdbcTemplate jdbcTemplate, LegalVersionValidator legalVersionValidator) {
    this.jdbcTemplate = jdbcTemplate;
    this.legalVersionValidator = legalVersionValidator;
  }

  public List<Map<String, Object>> getPendingRecruiters(String userId) {
    if (userId != null && !userId.isBlank()) {
      long parsedUserId =
          HttpRequestParsers.requireLong(userId.trim(), PendingQueueFields.USER_ID);
      return jdbcTemplate.query(
          PENDING_RECRUITER_SELECT_BY_USER_ID_SQL + " ORDER BY submitted_at DESC, id DESC",
          this::mapPendingRecruiterRow,
          parsedUserId);
    }

    return jdbcTemplate.query(
        PENDING_RECRUITER_SELECT_SQL + " ORDER BY submitted_at DESC, id DESC",
        this::mapPendingRecruiterRow);
  }

  public List<Map<String, Object>> getPendingRecruitersForActor(
      String userId, long actorUserId, String backendUserRole, boolean isAdmin) {
    if (isAdmin) {
      return getPendingRecruiters(userId);
    }

    if (!UserRole.RECRUITER.name().equals(backendUserRole)) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, RECRUITER_OR_ADMIN_REQUIRED_MESSAGE);
    }

    if (userId != null && !userId.isBlank()) {
      long parsedUserId =
          HttpRequestParsers.requireLong(userId.trim(), PendingQueueFields.USER_ID);
      if (parsedUserId != actorUserId) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "You can only access your own recruiter queue record.");
      }
    }

    return getPendingRecruiters(String.valueOf(actorUserId));
  }

  @Transactional
  public Map<String, Object> createPendingRecruiterForActor(
      Map<String, Object> request,
      long actorUserId,
      String backendUserRole,
      boolean isAdmin) {
    Map<String, Object> mutableRequest = new LinkedHashMap<>(request);

    if (!isAdmin) {
      if (!UserRole.RECRUITER.name().equals(backendUserRole)) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, RECRUITER_ONLY_MESSAGE);
      }

      PendingQueueActorSupport.enforcePayloadUserOwnership(mutableRequest, actorUserId);
    }

    return createPendingRecruiter(mutableRequest);
  }

  @Transactional
  public void deletePendingRecruiterForActor(
      String pendingRecruiterId, long actorUserId, String backendUserRole, boolean isAdmin) {
    if (!isAdmin) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, ADMIN_ROLE_REQUIRED_MESSAGE);
    }

    deletePendingRecruiter(pendingRecruiterId);
  }

  @Transactional
  public Map<String, Object> createPendingRecruiter(Map<String, Object> request) {
    Long userId =
        HttpRequestParsers.requireLong(request.get(PendingQueueFields.USER_ID), PendingQueueFields.USER_ID);
    String fullName =
        HttpRequestParsers.requireNonBlank(request.get(PendingQueueFields.FULL_NAME), PendingQueueFields.FULL_NAME);
    String email =
        HttpRequestParsers.requireNonBlank(request.get(PendingQueueFields.EMAIL), PendingQueueFields.EMAIL)
            .toLowerCase(Locale.ROOT);
    String linkedinUrl =
        normalizeLinkedinUrl(
            HttpRequestParsers.requireNonBlank(
                request.get(PendingQueueFields.LINKEDIN_URL), PendingQueueFields.LINKEDIN_URL));
    String organizationName =
        HttpRequestParsers.requireNonBlank(
            request.get(PendingQueueFields.ORGANIZATION_NAME), PendingQueueFields.ORGANIZATION_NAME);
    OffsetDateTime submittedAt =
        HttpRequestParsers.optionalOffsetDateTime(
            request.get(PendingQueueFields.SUBMITTED_AT), PendingQueueFields.SUBMITTED_AT);
    OffsetDateTime legalConsentAcceptedAt =
        HttpRequestParsers.requireOffsetDateTime(
            request.get(PendingQueueFields.LEGAL_CONSENT_ACCEPTED_AT),
            PendingQueueFields.LEGAL_CONSENT_ACCEPTED_AT);
    String legalConsentLocale =
        HttpRequestParsers.requireNonBlank(
            request.get(PendingQueueFields.LEGAL_CONSENT_LOCALE), PendingQueueFields.LEGAL_CONSENT_LOCALE);
    String termsVersionAccepted =
        HttpRequestParsers.requireNonBlank(
            request.get(PendingQueueFields.TERMS_VERSION_ACCEPTED), PendingQueueFields.TERMS_VERSION_ACCEPTED);
    String privacyVersionAccepted =
        HttpRequestParsers.requireNonBlank(
            request.get(PendingQueueFields.PRIVACY_VERSION_ACCEPTED), PendingQueueFields.PRIVACY_VERSION_ACCEPTED);

    legalVersionValidator.validateAcceptedVersions(termsVersionAccepted, privacyVersionAccepted);

    String sql = """
        INSERT INTO pending_recruiters (
          user_id,
          full_name,
          email,
          linkedin_url,
          organization_name,
          submitted_at,
          legal_consent_accepted_at,
          legal_consent_locale,
          terms_version_accepted,
          privacy_version_accepted
        )
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    try {
      Long createdId =
          jdbcTemplate.execute(
              (PreparedStatementCreator)
                  connection -> {
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setLong(1, userId);
                    statement.setString(2, fullName);
                    statement.setString(3, email);
                    statement.setString(4, linkedinUrl);
                    statement.setString(5, organizationName);
                    statement.setObject(
                        6, submittedAt != null ? submittedAt : OffsetDateTime.now(ZoneOffset.UTC));
                    statement.setObject(7, legalConsentAcceptedAt);
                    statement.setString(8, legalConsentLocale);
                    statement.setString(9, termsVersionAccepted);
                    statement.setString(10, privacyVersionAccepted);
                    return statement;
                  },
              (PreparedStatementCallback<Long>)
                  statement -> {
                    try (ResultSet resultSet = statement.executeQuery()) {
                      if (!resultSet.next()) {
                        throw new ResponseStatusException(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            FAILED_CREATE_PENDING_RECRUITER_MESSAGE);
                      }

                      return resultSet.getLong(1);
                    }
                  });

      return findPendingRecruiterById(createdId);
    } catch (DataIntegrityViolationException exception) {
      throw HttpErrors.conflict(PENDING_RECRUITER_EXISTS_MESSAGE);
    } catch (UncategorizedSQLException exception) {
      throw HttpErrors.badRequest(
          certifiedcarry_api.shared.SqlErrorMapper.extractSqlErrorMessagePreservingDetail(
              exception.getSQLException(), INVALID_PENDING_RECRUITER_PAYLOAD_MESSAGE));
    }
  }

  @Transactional
  public void deletePendingRecruiter(String pendingRecruiterId) {
    long parsedId = HttpRequestParsers.parsePathId(pendingRecruiterId, "pendingRecruiterId");
    int deletedRows = jdbcTemplate.update("DELETE FROM pending_recruiters WHERE id = ?", parsedId);

    if (deletedRows == 0) {
      throw HttpErrors.notFound(PENDING_RECRUITER_NOT_FOUND_PREFIX + pendingRecruiterId);
    }
  }

  private Map<String, Object> findPendingRecruiterById(long id) {
    List<Map<String, Object>> rows =
        jdbcTemplate.query(PENDING_RECRUITER_SELECT_BY_ID_SQL, this::mapPendingRecruiterRow, id);
    if (rows.isEmpty()) {
      throw HttpErrors.notFound(PENDING_RECRUITER_NOT_FOUND_PREFIX + id);
    }

    return rows.get(0);
  }

  private Map<String, Object> mapPendingRecruiterRow(ResultSet resultSet, int rowNumber)
      throws SQLException {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put(PendingQueueFields.ID, String.valueOf(resultSet.getLong("id")));
    row.put(PendingQueueFields.USER_ID, String.valueOf(resultSet.getLong("user_id")));
    row.put(PendingQueueFields.FULL_NAME, resultSet.getString("full_name"));
    row.put(PendingQueueFields.EMAIL, resultSet.getString("email"));
    row.put(PendingQueueFields.LINKEDIN_URL, resultSet.getString("linkedin_url"));
    row.put(PendingQueueFields.ORGANIZATION_NAME, resultSet.getString("organization_name"));
    row.put(PendingQueueFields.SUBMITTED_AT, resultSet.getObject("submitted_at", OffsetDateTime.class));
    row.put(
        PendingQueueFields.LEGAL_CONSENT_ACCEPTED_AT,
        resultSet.getObject("legal_consent_accepted_at", OffsetDateTime.class));
    row.put(PendingQueueFields.LEGAL_CONSENT_LOCALE, resultSet.getString("legal_consent_locale"));
    row.put(PendingQueueFields.TERMS_VERSION_ACCEPTED, resultSet.getString("terms_version_accepted"));
    row.put(PendingQueueFields.PRIVACY_VERSION_ACCEPTED, resultSet.getString("privacy_version_accepted"));
    return row;
  }

  private String normalizeLinkedinUrl(String value) {
    String normalized = HttpRequestParsers.requireNonBlank(value, PendingQueueFields.LINKEDIN_URL);
    return LinkedinUrlNormalizer.normalizeValidatedLinkedinUrl(normalized, HttpErrors::badRequest);
  }
}
