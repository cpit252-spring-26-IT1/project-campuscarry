package certifiedcarry_api.queue.api;

import certifiedcarry_api.queue.PendingQueueFields;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record PendingRecruiterCreateRequest(
    String userId,
    String fullName,
    String email,
    String linkedinUrl,
    String organizationName,
    OffsetDateTime submittedAt,
    OffsetDateTime legalConsentAcceptedAt,
    String legalConsentLocale,
    String termsVersionAccepted,
    String privacyVersionAccepted) {

  Map<String, Object> toServiceRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put(PendingQueueFields.USER_ID, userId);
    request.put(PendingQueueFields.FULL_NAME, fullName);
    request.put(PendingQueueFields.EMAIL, email);
    request.put(PendingQueueFields.LINKEDIN_URL, linkedinUrl);
    request.put(PendingQueueFields.ORGANIZATION_NAME, organizationName);
    request.put(PendingQueueFields.SUBMITTED_AT, submittedAt);
    request.put(PendingQueueFields.LEGAL_CONSENT_ACCEPTED_AT, legalConsentAcceptedAt);
    request.put(PendingQueueFields.LEGAL_CONSENT_LOCALE, legalConsentLocale);
    request.put(PendingQueueFields.TERMS_VERSION_ACCEPTED, termsVersionAccepted);
    request.put(PendingQueueFields.PRIVACY_VERSION_ACCEPTED, privacyVersionAccepted);
    return request;
  }
}

record PendingRecruiterResponse(
    String id,
    String userId,
    String fullName,
    String email,
    String linkedinUrl,
    String organizationName,
    OffsetDateTime submittedAt,
    OffsetDateTime legalConsentAcceptedAt,
    String legalConsentLocale,
    String termsVersionAccepted,
    String privacyVersionAccepted) {

  static PendingRecruiterResponse fromServiceRow(Map<String, Object> row) {
    return new PendingRecruiterResponse(
        (String) row.get(PendingQueueFields.ID),
        (String) row.get(PendingQueueFields.USER_ID),
        (String) row.get(PendingQueueFields.FULL_NAME),
        (String) row.get(PendingQueueFields.EMAIL),
        (String) row.get(PendingQueueFields.LINKEDIN_URL),
        (String) row.get(PendingQueueFields.ORGANIZATION_NAME),
        (OffsetDateTime) row.get(PendingQueueFields.SUBMITTED_AT),
        (OffsetDateTime) row.get(PendingQueueFields.LEGAL_CONSENT_ACCEPTED_AT),
        (String) row.get(PendingQueueFields.LEGAL_CONSENT_LOCALE),
        (String) row.get(PendingQueueFields.TERMS_VERSION_ACCEPTED),
        (String) row.get(PendingQueueFields.PRIVACY_VERSION_ACCEPTED));
  }

  static List<PendingRecruiterResponse> fromServiceRows(List<Map<String, Object>> rows) {
    return rows.stream().map(PendingRecruiterResponse::fromServiceRow).toList();
  }
}
