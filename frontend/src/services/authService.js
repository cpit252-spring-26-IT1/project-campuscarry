import api from "./httpClient";
import { isFirebaseConfigured } from "./firebaseClient";
import {
  getCurrentFirebaseSession,
  loginWithFirebase,
  logoutFromFirebase,
  registerWithFirebase,
  sendSignupVerificationEmail,
} from "./firebaseAuthService";
import {
  BACKEND_USER_CREATE_RETRY_ATTEMPTS,
  BACKEND_USER_CREATE_RETRY_DELAY_MS,
  ROLE_ADMIN,
  ROLE_PLAYER,
  ROLE_RECRUITER,
  UNVERIFIED_SIGNIN_MESSAGE,
} from "./auth/constants";
import {
  createBackendUserFromPendingDraft,
  ensureBackendUserByFirebaseSessionWithRetry,
  ensurePendingRecruiterQueueRecord,
} from "./auth/backendBootstrapService";
import {
  clearPendingSignupDraft,
  doesPendingDraftMatchFirebaseSession,
  normalizePendingSignupDraft,
  persistPendingSignupDraft,
  readPendingSignupDraft,
} from "./auth/pendingSignupDraftService";
import {
  buildAuthUser,
  buildConsentRecord,
  buildSessionRequestConfig,
  getApiErrorMessage,
  getBlockedStatusMessage,
  getEmailVerificationContinueUrl,
  getFriendlyFirebaseAuthMessage,
  getRequestStatusCode,
  isEmailVerificationBypassed,
  isFirebaseAuthError,
  isOptionalEndpointError,
  isRetryableBackendSignupLinkError,
  isValidLinkedInUrl,
  logBackgroundAuthIssue,
  normalizeEmail,
  normalizeLinkedInUrl,
  requireFirebaseConfiguration,
  waitFor,
} from "./auth/shared";

const getPendingSignupVerificationState = async ({ forceRefresh = false } = {}) => {
  requireFirebaseConfiguration();

  const pendingDraft = readPendingSignupDraft();
  if (!pendingDraft) {
    return {
      hasPendingSignup: false,
      email: "",
      role: null,
      signedIn: false,
      signedInEmail: null,
      matchesSignedInEmail: false,
      emailVerified: false,
      canComplete: false,
    };
  }

  const firebaseSession = await getCurrentFirebaseSession({ forceRefresh });
  const matchesSignedInEmail = doesPendingDraftMatchFirebaseSession(pendingDraft, firebaseSession);
  const emailVerified = Boolean(matchesSignedInEmail && firebaseSession?.emailVerified);

  return {
    hasPendingSignup: true,
    email: pendingDraft.email,
    role: pendingDraft.role,
    signedIn: Boolean(firebaseSession),
    signedInEmail: firebaseSession?.email || null,
    matchesSignedInEmail,
    emailVerified,
    canComplete: emailVerified,
  };
};

const resendPendingSignupVerificationEmail = async () => {
  requireFirebaseConfiguration();

  const pendingDraft = readPendingSignupDraft();
  if (!pendingDraft) {
    throw new Error("No pending signup verification found. Please register again.");
  }

  const firebaseSession = await getCurrentFirebaseSession({ forceRefresh: true });
  if (!firebaseSession) {
    throw new Error("Please sign in with the same account used for registration.");
  }

  if (!doesPendingDraftMatchFirebaseSession(pendingDraft, firebaseSession)) {
    throw new Error("Signed-in email does not match the pending signup email.");
  }

  if (firebaseSession.emailVerified) {
    return {
      alreadyVerified: true,
      email: pendingDraft.email,
    };
  }

  await sendSignupVerificationEmail({
    continueUrl: getEmailVerificationContinueUrl(),
  });

  return {
    alreadyVerified: false,
    email: pendingDraft.email,
    sentAt: new Date().toISOString(),
  };
};

const resolveVerifiedSignupSession = async (pendingDraft) => {
  const refreshedSession = await getCurrentFirebaseSession({ forceRefresh: true });
  if (!refreshedSession) {
    throw new Error("Please sign in with the same account used for registration.");
  }

  if (!doesPendingDraftMatchFirebaseSession(pendingDraft, refreshedSession)) {
    throw new Error("Signed-in email does not match the pending signup email.");
  }

  if (!refreshedSession.emailVerified) {
    throw new Error(UNVERIFIED_SIGNIN_MESSAGE);
  }

  return refreshedSession;
};

const completePendingSignupRegistration = async () => {
  requireFirebaseConfiguration();

  const pendingDraft = readPendingSignupDraft();
  if (!pendingDraft) {
    throw new Error("No pending signup verification found. Please register again.");
  }

  let firebaseSession = await resolveVerifiedSignupSession(pendingDraft);
  const completionFallbackMessage =
    pendingDraft.role === ROLE_PLAYER
      ? "Unable to complete player registration."
      : "Unable to complete recruiter registration.";

  let user = await ensureBackendUserByFirebaseSessionWithRetry({ firebaseSession });
  if (!user) {
    let createError = null;

    for (let attempt = 1; attempt <= BACKEND_USER_CREATE_RETRY_ATTEMPTS; attempt += 1) {
      try {
        user = await createBackendUserFromPendingDraft({ pendingDraft, firebaseSession });
        createError = null;
        break;
      } catch (error) {
        createError = error;

        if (
          attempt >= BACKEND_USER_CREATE_RETRY_ATTEMPTS ||
          !isRetryableBackendSignupLinkError(error)
        ) {
          throw new Error(getApiErrorMessage(error, completionFallbackMessage));
        }

        await waitFor(BACKEND_USER_CREATE_RETRY_DELAY_MS * attempt);
        firebaseSession = await resolveVerifiedSignupSession(pendingDraft);

        user = await ensureBackendUserByFirebaseSessionWithRetry({ firebaseSession });
        if (user) {
          createError = null;
          clearPendingSignupDraft();
          break;
        }
      }
    }

    if (!user) {
      throw new Error(getApiErrorMessage(createError, completionFallbackMessage));
    }
  } else {
    clearPendingSignupDraft();
  }

  void ensurePendingRecruiterQueueRecord(user);

  const authUser = buildAuthUser(user);
  const shouldAuthenticate = user.role === ROLE_PLAYER && user.status === "APPROVED";

  if (!shouldAuthenticate) {
    try {
      await logoutFromFirebase();
    } catch {
      // Keep completion resilient even if sign-out fails.
    }

    return {
      token: null,
      user: authUser,
      shouldAuthenticate: false,
    };
  }

  return {
    token: firebaseSession.idToken,
    user: authUser,
    shouldAuthenticate: true,
  };
};

const notifySessionLogin = async ({ token } = {}) => {
  try {
    await api.post("/auth/session/login", {}, buildSessionRequestConfig(token));
  } catch (error) {
    if (!isOptionalEndpointError(error)) {
      logBackgroundAuthIssue("Unable to report login session state:", error);
    }
  }
};

const notifySessionLogout = async ({ token } = {}) => {
  try {
    await api.post("/auth/session/logout", {}, buildSessionRequestConfig(token));
  } catch (error) {
    if (!isOptionalEndpointError(error)) {
      logBackgroundAuthIssue("Unable to report logout session state:", error);
    }
  }
};

const deleteCurrentAccount = async () => {
  try {
    await api.delete("/users/me");
    clearPendingSignupDraft();
  } catch (error) {
    const statusCode = getRequestStatusCode(error);

    if (statusCode === 401) {
      throw new Error("Please sign in again before deleting your account.");
    }

    if (statusCode === 403) {
      throw new Error("Only player and recruiter accounts can use self-delete.");
    }

    if (statusCode === 404) {
      throw new Error("No matching account was found for this user.");
    }

    throw new Error(getApiErrorMessage(error, "Unable to delete account."));
  }
};

const login = async ({ email, password }) => {
  if (!email || !password) {
    throw new Error("Email and password are required.");
  }

  const normalizedEmail = normalizeEmail(email);

  requireFirebaseConfiguration();
  let hasActiveFirebaseSession = false;

  try {
    const firebaseSession = await loginWithFirebase({
      email: normalizedEmail,
      password,
    });
    hasActiveFirebaseSession = true;

    let user = await ensureBackendUserByFirebaseSessionWithRetry({ firebaseSession });

    if (!user) {
      const pendingDraft = readPendingSignupDraft();
      const shouldTryPendingCompletion = Boolean(
        pendingDraft && doesPendingDraftMatchFirebaseSession(pendingDraft, firebaseSession),
      );

      if (shouldTryPendingCompletion) {
        if (
          !firebaseSession.emailVerified &&
          !isEmailVerificationBypassed({ email: normalizedEmail })
        ) {
          throw new Error(UNVERIFIED_SIGNIN_MESSAGE);
        }

        const completion = await completePendingSignupRegistration();
        if (!completion.shouldAuthenticate) {
          throw new Error(getBlockedStatusMessage(completion.user));
        }

        return {
          token: completion.token,
          user: completion.user,
        };
      }

      throw new Error("No CertifiedCarry account found for this Firebase user.");
    }

    if (
      !firebaseSession.emailVerified &&
      !isEmailVerificationBypassed({ email: normalizedEmail, user })
    ) {
      throw new Error(UNVERIFIED_SIGNIN_MESSAGE);
    }

    void ensurePendingRecruiterQueueRecord(user);

    if (user.role !== ROLE_PLAYER && user.status !== "APPROVED") {
      throw new Error(getBlockedStatusMessage(user));
    }

    return {
      token: firebaseSession.idToken,
      user: buildAuthUser(user),
    };
  } catch (error) {
    if (hasActiveFirebaseSession) {
      try {
        await logoutFromFirebase();
      } catch {
        // Keep original auth error.
      }
    }

    if (isFirebaseAuthError(error)) {
      throw new Error(getFriendlyFirebaseAuthMessage(error, "Unable to sign in."));
    }

    throw error;
  }
};

const restoreAuthenticatedSession = async () => {
  if (!isFirebaseConfigured()) {
    return null;
  }

  const firebaseSession = await getCurrentFirebaseSession();
  if (!firebaseSession) {
    return null;
  }

  const user = await ensureBackendUserByFirebaseSessionWithRetry({ firebaseSession });
  if (!user) {
    return null;
  }

  void ensurePendingRecruiterQueueRecord(user);

  if (user.role !== ROLE_PLAYER && user.status !== "APPROVED") {
    return null;
  }

  return {
    token: firebaseSession.idToken,
    user: buildAuthUser(user),
  };
};

const buildPendingDraftForRegistration = ({ formData, consentRecord, createdAt }) => {
  const { fullName } = formData;
  const role = String(formData?.role || "").toUpperCase();
  const normalizedFullName = String(fullName || "").trim();

  if (role === ROLE_PLAYER) {
    const { username, personalEmail } = formData;

    if (!username || !personalEmail) {
      throw new Error("Player registration requires full name, gamertag, email, and password.");
    }

    return normalizePendingSignupDraft({
      role: ROLE_PLAYER,
      email: normalizeEmail(personalEmail),
      fullName: normalizedFullName,
      username,
      consentRecord,
      createdAt,
    });
  }

  if (role === ROLE_RECRUITER) {
    const { email, organizationName, linkedinUrl } = formData;

    if (!email || !organizationName || !linkedinUrl) {
      throw new Error("Recruiter registration requires organization, email, and LinkedIn URL.");
    }

    const normalizedLinkedIn = normalizeLinkedInUrl(linkedinUrl);
    if (!isValidLinkedInUrl(normalizedLinkedIn)) {
      throw new Error("LinkedIn URL must be a valid linkedin.com profile or company link.");
    }

    return normalizePendingSignupDraft({
      role: ROLE_RECRUITER,
      email: normalizeEmail(email),
      fullName: normalizedFullName,
      organizationName,
      linkedinUrl: normalizedLinkedIn,
      consentRecord,
      createdAt,
    });
  }

  if (role === ROLE_ADMIN) {
    throw new Error("Admin accounts cannot be created from registration.");
  }

  throw new Error("Selected role is not supported for self registration.");
};

const register = async (formData) => {
  const { fullName, password } = formData;
  const role = String(formData?.role || "").toUpperCase();

  if (!fullName || !password || !role) {
    throw new Error("Please complete all required fields.");
  }

  requireFirebaseConfiguration();

  const consentRecord = buildConsentRecord(formData);
  const pendingDraft = buildPendingDraftForRegistration({
    formData,
    consentRecord,
    createdAt: new Date().toISOString(),
  });

  if (!pendingDraft) {
    throw new Error("Unable to prepare registration.");
  }

  let firebaseSession;

  try {
    firebaseSession = await registerWithFirebase({
      email: pendingDraft.email,
      password,
    });
  } catch (error) {
    if (isFirebaseAuthError(error)) {
      throw new Error(getFriendlyFirebaseAuthMessage(error, "Unable to complete registration."));
    }

    throw error;
  }

  persistPendingSignupDraft(pendingDraft);

  let verificationEmailSent = false;
  if (!firebaseSession.emailVerified) {
    try {
      await sendSignupVerificationEmail({
        continueUrl: getEmailVerificationContinueUrl(),
      });
      verificationEmailSent = true;
    } catch (error) {
      logBackgroundAuthIssue("Unable to send signup verification email:", error);
    }
  }

  return {
    email: pendingDraft.email,
    role: pendingDraft.role,
    requiresEmailVerification: !firebaseSession.emailVerified,
    verificationEmailSent,
  };
};

export {
  clearPendingSignupDraft,
  completePendingSignupRegistration,
  deleteCurrentAccount,
  getPendingSignupVerificationState,
  login,
  notifySessionLogin,
  notifySessionLogout,
  register,
  restoreAuthenticatedSession,
  resendPendingSignupVerificationEmail,
};
