# Legal Consent Contract (Frontend -> Spring Boot)

This contract defines how legal consent is captured in the frontend registration flow and what the backend must validate and persist.

## Scope

Applies to registration requests for:

- `PLAYER`
- `RECRUITER`

## Required Request Fields

The frontend now sends these consent fields with registration payloads:

- `legalConsentAccepted` (boolean)
- `legalConsentAcceptedAt` (ISO 8601 timestamp in UTC)
- `legalConsentLocale` (`en` or `ar`)
- `legalConsentSource` (`REGISTER_PAGE`)
- `termsVersionAccepted` (string)
- `privacyVersionAccepted` (string)

Current frontend policy versions:

- Terms: `cc-terms-2026-04-04`
- Privacy: `cc-privacy-2026-04-04`

Backend active-version config keys:

- `legal.terms-active-version`
- `legal.privacy-active-version`

## Validation Rules (Backend)

Spring Boot registration should reject with `400` when any rule fails:

1. `legalConsentAccepted` must be `true`.
2. `legalConsentAcceptedAt` must be parseable ISO datetime and not in the future beyond clock-skew tolerance.
3. `termsVersionAccepted` must match active terms version in backend config.
4. `privacyVersionAccepted` must match active privacy version in backend config.
5. `legalConsentLocale` must be in allowed locale set.
6. `legalConsentSource` must be in allowed source enum.

## Persistence Contract

Minimum fields to persist on the user/account record:

- `legal_consent_accepted_at` (timestamp with time zone, not null)
- `legal_consent_locale` (varchar)
- `legal_consent_source` (varchar)
- `terms_version_accepted` (varchar)
- `privacy_version_accepted` (varchar)

Recommended audit table for compliance traceability:

- Table: `user_consent_events`
- Columns:
  - `id` (uuid)
  - `user_id` (fk)
  - `event_type` (`REGISTER_CONSENT_ACCEPTED` or `RECONSENT_ACCEPTED`)
  - `accepted_at` (timestamp with time zone)
  - `terms_version` (varchar)
  - `privacy_version` (varchar)
  - `locale` (varchar)
  - `source` (varchar)
  - `request_id` (varchar, nullable)
  - `ip_hash` (varchar, nullable)
  - `user_agent_hash` (varchar, nullable)
  - `created_at` (timestamp with time zone)

## API Behavior Contract

1. Registration endpoint must atomically create user and consent metadata.
2. If user creation succeeds but consent persistence fails, rollback transaction.
3. Response should include persisted consent fields on created user object.
4. If registration includes an Authorization bearer token, backend links `firebaseUid` only when:

- token email is verified
- token email equals submitted registration email for that role

5. Login endpoint may later enforce re-consent when active policy versions change.

## Future Re-Consent Extension

When policy versions change, backend can flag re-consent using a response contract such as:

```json
{
  "requiresReconsent": true,
  "requiredTermsVersion": "cc-terms-YYYY-MM-DD",
  "requiredPrivacyVersion": "cc-privacy-YYYY-MM-DD"
}
```

Frontend can then route users to a dedicated re-consent screen before full access.
