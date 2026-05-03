# Frontend API Contract

This document defines the API behavior expected by the frontend services.

## Base URL and Transport

- Frontend client: `src/services/httpClient.js`
- Base URL: `VITE_API_URL` (default `/api`)
- Timeout: `VITE_API_TIMEOUT_MS` (default `15000`)
- Dev proxy target: `VITE_DEV_PROXY_TARGET` (default `http://localhost:8000`)

## General Compatibility Rules

- IDs are treated as string-compatible values (`String(id)` is used heavily).
- List endpoints should return arrays.
- Query filtering currently uses query params such as `id`, `role`, and `status`.
- Timestamps are ISO strings.

## Resources and Endpoints

### auth

- `GET /auth/me`
  - Requires Firebase bearer token.
  - Returns `authenticated`, `firebaseUid`, `firebaseEmail`, `backendUserId`, `backendUserRole`, `principal`.
  - Used to resolve backend user linkage from Firebase session.

### users

- `GET /users`
  - Used for login and registration uniqueness checks.
- `GET /users?role=PLAYER`
  - Used for leaderboard/profile joins.
- `GET /users?role=PLAYER&id=:userId`
  - Used by player profile details.
- `GET /users?role=RECRUITER&status=PENDING`
  - Used in admin pending recruiters.
- `GET /users?role=RECRUITER&status=DECLINED`
  - Used in admin declined items.
- `POST /users`
  - Player and recruiter registration. Must include legal consent metadata.
  - Optional Authorization bearer token is used to bind `firebaseUid` to the newly created backend user.
  - If bearer token is present, backend rejects when token email is unverified or does not match submitted registration email.
- `PATCH /users/:id`
  - Admin approval/decline workflow.

Required read fields by frontend:

- `id`, `fullName`, `username`, `personalEmail`, `email`, `organizationName`, `role`, `status`, `declineReason`, `declinedAt`, `updatedAt`, `legalConsentLocale`, `termsVersionAccepted`, `privacyVersionAccepted`

Security note:

- `password` must never be returned by backend responses.

### player_profiles

- `GET /player_profiles`
- `GET /player_profiles?userId=:userId`
- `POST /player_profiles`
- `PATCH /player_profiles/:id`

Required read fields by frontend:

- `id`, `userId`, `username`, `profileImage`, `game`, `rank`, `allowPlayerChats`, `isWithTeam`, `teamName`, `rocketLeagueModes`, `primaryRocketLeagueMode`, `inGameRoles`, `inGameRole`, `ratingValue`, `ratingLabel`, `proofImage`, `bio`, `clipsUrl`, `rankVerificationStatus`, `declineReason`, `declinedAt`, `isVerified`, `submittedAt`, `updatedAt`

### pending_recruiters

- `POST /pending_recruiters`
- `GET /pending_recruiters`
- `DELETE /pending_recruiters/:id`

Required read fields by frontend:

- `id`, `userId`, `fullName`, `email`, `linkedinUrl`, `organizationName`, `submittedAt`, `legalConsentAcceptedAt`, `legalConsentLocale`, `termsVersionAccepted`, `privacyVersionAccepted`

### pending_ranks

- `GET /pending_ranks`
- `GET /pending_ranks?status=DECLINED`
- `POST /pending_ranks`
- `PATCH /pending_ranks/:id`
- `DELETE /pending_ranks/:id`

Required read fields by frontend:

- `id`, `userId`, `username`, `fullName`, `game`, `claimedRank`, `inGameRoles`, `inGameRole`, `ratingValue`, `ratingLabel`, `rocketLeagueModes`, `primaryRocketLeagueMode`, `proofImage`, `status`, `submittedAt`, `resolvedAt`, `declineReason`, `editedAfterDecline`, `editedAt`, `updatedAt`

### leaderboard

- `GET /leaderboard`
- `POST /leaderboard`
- `PATCH /leaderboard/:id`
- `DELETE /leaderboard/:id`

Required read fields by frontend:

- `id`, `userId`, `username`, `game`, `rank`, `role`, `ratingValue`, `ratingLabel`, `updatedAt`

### chat_threads

- `GET /chat_threads`
- `POST /chat_threads`
- `PATCH /chat_threads/:id`

Required read fields by frontend:

- `id`, `participantIds`, `initiatedById`, `initiatedByRole`, `lastSenderId`, `lastMessageAt`, `lastMessagePreview`, `createdAt`, `updatedAt`

### chat_messages

- `GET /chat_messages`
- `POST /chat_messages`
- `PATCH /chat_messages/:id`

Required read fields by frontend:

- `id`, `threadId`, `senderId`, `recipientId`, `body`, `createdAt`, `readAt`

## Legal Consent (Registration)

Frontend registration now sends the following fields for both player and recruiter signups:

- `legalConsentAccepted` (must be `true`)
- `legalConsentAcceptedAt` (ISO string)
- `legalConsentLocale` (`en` or `ar`)
- `legalConsentSource` (`REGISTER_PAGE`)
- `termsVersionAccepted` (string)
- `privacyVersionAccepted` (string)

Backend currently enforces active versions and rejects outdated consent metadata with `400`.

Backend should reject registration when consent fields are missing/invalid and persist consent metadata atomically with user creation.

Detailed backend handoff: `docs/legal-consent-contract.md`.

## Frontend Service Notes

- `src/services/authService.js`
  - Login relies on Firebase authentication and `/auth/me` for backend linkage.
  - Recruiters must be `APPROVED` to sign in.

- `src/services/playerService.js`
  - Normalizes game/rank/roles client-side.
  - Uses `pending_ranks` to drive verification flow and may de-duplicate multiple pending rows.

- `src/services/adminService.js`
  - Approve/decline workflows update both `users` and `player_profiles`/`pending_ranks`.

- `src/services/chatService.js`
  - Applies role rules client-side before writes.
  - Uses participant pair grouping to reconcile duplicate thread rows.

## Backend Migration Checklist

- Keep route shapes and query params compatible first.
- Preserve string-compatible IDs to avoid implicit mismatches.
- Return arrays for list endpoints, even when empty.
- Ensure timestamps are ISO strings.
- If replacing query-style filtering with dedicated endpoints, update the frontend services in one migration PR.
