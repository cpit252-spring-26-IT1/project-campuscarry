import api from "../httpClient";
import { ensurePlayerProfile } from "../playerProfileService";
import {
  BACKEND_USER_CREATE_ENDPOINTS,
  BACKEND_USER_LOOKUP_RETRY_ATTEMPTS,
  BACKEND_USER_LOOKUP_RETRY_DELAY_MS,
  ROLE_PLAYER,
  ROLE_RECRUITER,
} from "./constants";
import {
  buildSessionRequestConfig,
  firstArrayItemOrNull,
  isConflictError,
  isOptionalEndpointError,
  isRetryableCreateEndpointError,
  logBackgroundAuthIssue,
  waitFor,
} from "./shared";
import {
  buildCreateUserPayloadFromPendingDraft,
  clearPendingSignupDraft,
} from "./pendingSignupDraftService";

const ensureBackendUserByFirebaseSession = async ({ firebaseSession }) => {
  const authMeRequestConfig = buildSessionRequestConfig(firebaseSession?.idToken);
  const { data: authMe } = await api.get("/auth/me", authMeRequestConfig);
  const backendUserId = String(authMe?.backendUserId || "").trim();

  if (!backendUserId) {
    return null;
  }

  const { data: users } = await api.get("/users", {
    params: {
      id: backendUserId,
    },
  });

  return firstArrayItemOrNull(users);
};

const ensureBackendUserByFirebaseSessionWithRetry = async ({ firebaseSession }) => {
  for (let attempt = 1; attempt <= BACKEND_USER_LOOKUP_RETRY_ATTEMPTS; attempt += 1) {
    const user = await ensureBackendUserByFirebaseSession({ firebaseSession });
    if (user) {
      return user;
    }

    if (attempt < BACKEND_USER_LOOKUP_RETRY_ATTEMPTS) {
      await waitFor(BACKEND_USER_LOOKUP_RETRY_DELAY_MS);
    }
  }

  return null;
};

const maybeCreatePlayerProfile = async ({ createdPlayer, username }) => {
  try {
    return await ensurePlayerProfile({
      userId: createdPlayer.id,
      username,
    });
  } catch (error) {
    if (isOptionalEndpointError(error) || isConflictError(error)) {
      return null;
    }

    throw error;
  }
};

const maybeCreatePendingRecruiterRecord = async ({
  createdRecruiter,
  fullName,
  email,
  linkedinUrl,
  organizationName,
  consentRecord,
}) => {
  try {
    const { data: pendingRecruiters } = await api.get("/pending_recruiters", {
      params: {
        userId: String(createdRecruiter.id),
      },
    });

    const existingRecord = firstArrayItemOrNull(pendingRecruiters);
    if (existingRecord) {
      return existingRecord;
    }

    const { data: createdRecord } = await api.post("/pending_recruiters", {
      userId: String(createdRecruiter.id),
      fullName,
      email,
      linkedinUrl,
      organizationName,
      submittedAt: new Date().toISOString(),
      legalConsentAcceptedAt: consentRecord.legalConsentAcceptedAt,
      legalConsentLocale: consentRecord.legalConsentLocale,
      termsVersionAccepted: consentRecord.termsVersionAccepted,
      privacyVersionAccepted: consentRecord.privacyVersionAccepted,
    });

    return createdRecord;
  } catch (error) {
    if (isOptionalEndpointError(error) || isConflictError(error)) {
      return null;
    }

    throw error;
  }
};

const ensurePendingRecruiterQueueRecord = async (user) => {
  if (!user || user.role !== ROLE_RECRUITER || user.status !== "PENDING") {
    return;
  }

  try {
    await maybeCreatePendingRecruiterRecord({
      createdRecruiter: user,
      fullName: user.fullName || "",
      email: user.email || user.personalEmail || "",
      linkedinUrl: user.linkedinUrl || "",
      organizationName: user.organizationName || "",
      consentRecord: {
        legalConsentAcceptedAt: user.legalConsentAcceptedAt,
        legalConsentLocale: user.legalConsentLocale,
        termsVersionAccepted: user.termsVersionAccepted,
        privacyVersionAccepted: user.privacyVersionAccepted,
      },
    });
  } catch (error) {
    logBackgroundAuthIssue("Unable to ensure pending recruiter queue record:", error, "error");
  }
};

const getPlayerProfileImage = async (user) => {
  if (!user || user.role !== ROLE_PLAYER) {
    return "";
  }

  try {
    const playerProfile = await maybeCreatePlayerProfile({
      createdPlayer: user,
      username: user.username || user.fullName || `player-${user.id}`,
    });

    return playerProfile?.profileImage || "";
  } catch (error) {
    logBackgroundAuthIssue("Unable to ensure player profile during login:", error, "error");
    return "";
  }
};

const createBackendUserFromPendingDraft = async ({ pendingDraft, firebaseSession }) => {
  const payload = buildCreateUserPayloadFromPendingDraft(pendingDraft);
  const requestConfig = buildSessionRequestConfig(firebaseSession?.idToken);
  let createdUser;

  try {
    let lastCreateError = null;

    for (const endpoint of BACKEND_USER_CREATE_ENDPOINTS) {
      try {
        const response = await api.post(endpoint, payload, requestConfig);
        createdUser = response.data;
        lastCreateError = null;
        break;
      } catch (error) {
        lastCreateError = error;

        if (!isRetryableCreateEndpointError(error)) {
          throw error;
        }
      }
    }

    if (!createdUser && lastCreateError) {
      throw lastCreateError;
    }
  } catch (error) {
    if (isConflictError(error)) {
      const existingUser = await ensureBackendUserByFirebaseSessionWithRetry({ firebaseSession });
      if (existingUser) {
        createdUser = existingUser;
      } else {
        clearPendingSignupDraft();

        if (pendingDraft.role === ROLE_PLAYER) {
          throw new Error("An account with this email or gamertag already exists.");
        }

        throw new Error("An account with this email already exists.");
      }
    } else {
      throw error;
    }
  }

  if (!createdUser) {
    throw new Error("Unable to complete registration.");
  }

  try {
    if (pendingDraft.role === ROLE_PLAYER) {
      await maybeCreatePlayerProfile({
        createdPlayer: createdUser,
        username: pendingDraft.username,
      });
    } else {
      await maybeCreatePendingRecruiterRecord({
        createdRecruiter: createdUser,
        fullName: pendingDraft.fullName,
        email: pendingDraft.email,
        linkedinUrl: pendingDraft.linkedinUrl,
        organizationName: pendingDraft.organizationName,
        consentRecord: pendingDraft.consentRecord,
      });
    }
  } catch (error) {
    logBackgroundAuthIssue("Post-registration bootstrap failed:", error, "error");
  }

  clearPendingSignupDraft();
  return createdUser;
};

export {
  createBackendUserFromPendingDraft,
  ensureBackendUserByFirebaseSessionWithRetry,
  ensurePendingRecruiterQueueRecord,
  getPlayerProfileImage,
};
