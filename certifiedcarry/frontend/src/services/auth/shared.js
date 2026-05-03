import { isFirebaseConfigured } from "../firebaseClient";
import { normalizeRecruiterDmOpenness } from "../recruiterService";
import {
  BACKEND_USER_CREATE_RETRYABLE_STATUS_CODES,
  FIREBASE_REQUIRED_MESSAGE,
  OPTIONAL_ENDPOINT_STATUS_CODES,
  REQUEST_FAILED_STATUS_CODE_PREFIX,
  RETRYABLE_SIGNUP_LINK_ERROR_SNIPPETS,
  ROLE_ADMIN,
  ROLE_PLAYER,
} from "./constants";

const normalizeEmail = (email) => email.trim().toLowerCase();
const normalizeOptionalText = (value) => String(value || "").trim();
const normalizeLinkedInUrl = (linkedinUrl) => normalizeOptionalText(linkedinUrl);

const firstNonEmptyText = (values) => {
  for (const value of values) {
    const normalized = normalizeOptionalText(value);
    if (normalized) {
      return normalized;
    }
  }

  return "";
};

const isRequestStatusCodeMessage = (value) =>
  normalizeOptionalText(value).toLowerCase().startsWith(REQUEST_FAILED_STATUS_CODE_PREFIX);

const EMAIL_VERIFICATION_BYPASS_ENABLED =
  !import.meta.env.PROD &&
  String(import.meta.env.VITE_ENABLE_EMAIL_VERIFICATION_BYPASS || "")
    .trim()
    .toLowerCase() === "true";

const EMAIL_VERIFICATION_BYPASS_EMAILS = EMAIL_VERIFICATION_BYPASS_ENABLED
  ? new Set(
      String(import.meta.env.VITE_EMAIL_VERIFICATION_BYPASS_EMAILS || "")
        .split(",")
        .map((email) => normalizeEmail(String(email || "").trim()))
        .filter(Boolean),
    )
  : new Set();

const isEmailVerificationBypassed = ({ email, user } = {}) => {
  if (String(user?.role || "").toUpperCase() === ROLE_ADMIN) {
    return true;
  }

  const userEmail = normalizeEmail(String(user?.email || ""));
  const candidateEmail = normalizeEmail(String(email || userEmail || ""));
  return Boolean(candidateEmail) && EMAIL_VERIFICATION_BYPASS_EMAILS.has(candidateEmail);
};

const waitFor = (delayMs) =>
  new Promise((resolve) => {
    setTimeout(resolve, delayMs);
  });

const isValidLinkedInUrl = (linkedinUrl) => {
  try {
    const parsedUrl = new URL(linkedinUrl);
    const normalizedHost = parsedUrl.hostname.toLowerCase();

    return (
      (parsedUrl.protocol === "https:" || parsedUrl.protocol === "http:") &&
      (normalizedHost === "linkedin.com" || normalizedHost.endsWith(".linkedin.com")) &&
      parsedUrl.pathname &&
      parsedUrl.pathname !== "/"
    );
  } catch {
    return false;
  }
};

const getBlockedStatusMessage = (user) => {
  const status = user?.status;

  if (status === "PENDING") {
    return "Your account is pending approval. Please wait for admin confirmation.";
  }

  if (status === "DECLINED") {
    const reason = normalizeOptionalText(user?.declineReason);
    if (reason) {
      return `Your account was declined. Reason: ${reason}`;
    }

    return "Your account was declined. Please contact support.";
  }

  return "Unable to sign in.";
};

const getRequestStatusCode = (error) => {
  const status = error?.response?.status;
  return Number.isInteger(status) ? status : null;
};

const isConflictError = (error) => getRequestStatusCode(error) === 409;

const isRetryableCreateEndpointError = (error) => {
  const statusCode = getRequestStatusCode(error);
  return statusCode === 403 || statusCode === 404 || statusCode === 405;
};

const getResponseErrorMessage = (error) => {
  const responsePayload = error?.response?.data;

  if (typeof responsePayload === "string") {
    return normalizeOptionalText(responsePayload);
  }

  if (!responsePayload || typeof responsePayload !== "object") {
    return "";
  }

  return firstNonEmptyText([
    responsePayload.message,
    responsePayload.error,
    responsePayload.detail,
    responsePayload.title,
  ]);
};

const isRetryableBackendSignupLinkError = (error) => {
  const statusCode = getRequestStatusCode(error);
  if (statusCode === null || !BACKEND_USER_CREATE_RETRYABLE_STATUS_CODES.has(statusCode)) {
    return false;
  }

  const normalizedMessage = normalizeOptionalText(
    getResponseErrorMessage(error) || error?.message,
  ).toLowerCase();

  if (!normalizedMessage) {
    return true;
  }

  return RETRYABLE_SIGNUP_LINK_ERROR_SNIPPETS.some((snippet) =>
    normalizedMessage.includes(snippet),
  );
};

const firstArrayItemOrNull = (value) => (Array.isArray(value) ? value[0] || null : null);

const isOptionalEndpointError = (error) => {
  const statusCode = getRequestStatusCode(error);
  return statusCode !== null && OPTIONAL_ENDPOINT_STATUS_CODES.has(statusCode);
};

const isFirebaseAuthError = (error) =>
  typeof error?.code === "string" && error.code.startsWith("auth/");

const getFriendlyFirebaseAuthMessage = (error, fallbackMessage) => {
  const code = String(error?.code || "").toLowerCase();

  if (
    code === "auth/invalid-credential" ||
    code === "auth/wrong-password" ||
    code === "auth/user-not-found" ||
    code === "auth/invalid-email"
  ) {
    return "Invalid email or password.";
  }

  if (code === "auth/email-already-in-use") {
    return "An account with this email already exists.";
  }

  if (code === "auth/weak-password") {
    return "Password should be at least 6 characters.";
  }

  if (code === "auth/too-many-requests") {
    return "Too many attempts. Please try again later.";
  }

  return fallbackMessage;
};

const getApiErrorMessage = (error, fallbackMessage) => {
  const responseMessage = getResponseErrorMessage(error);
  if (responseMessage) {
    return responseMessage;
  }

  const directMessage = normalizeOptionalText(error?.message);
  if (directMessage && !isRequestStatusCodeMessage(directMessage)) {
    return directMessage;
  }

  return fallbackMessage;
};

const requireFirebaseConfiguration = () => {
  if (!isFirebaseConfigured()) {
    throw new Error(FIREBASE_REQUIRED_MESSAGE);
  }
};

const getEmailVerificationContinueUrl = () => {
  const configuredUrl = String(import.meta.env.VITE_EMAIL_VERIFICATION_CONTINUE_URL || "").trim();
  if (configuredUrl) {
    return configuredUrl;
  }

  if (typeof window !== "undefined" && window.location?.origin) {
    return `${window.location.origin}/verify-email-signup`;
  }

  return undefined;
};

const generateBackendPlaceholderPassword = () => {
  const randomId =
    typeof globalThis.crypto?.randomUUID === "function"
      ? globalThis.crypto.randomUUID()
      : `${Date.now()}-${Math.random().toString(36).slice(2, 14)}`;

  return `cc-firebase-${randomId}`;
};

const buildAuthUser = (user, profileImage = "") => ({
  id: user.id,
  fullName: user.fullName,
  username: user.username,
  organizationName: user.organizationName,
  role: user.role,
  status: user.status,
  email: user.role === ROLE_PLAYER ? user.personalEmail || user.email : user.email,
  profileImage,
  recruiterDmOpenness: normalizeRecruiterDmOpenness(user.recruiterDmOpenness || user.dmOpenness),
});

const buildConsentRecord = (formData) => {
  const {
    legalConsentAccepted,
    legalConsentAcceptedAt,
    legalConsentLocale,
    legalConsentSource,
    termsVersionAccepted,
    privacyVersionAccepted,
  } = formData;

  if (!legalConsentAccepted) {
    throw new Error("You must accept the Terms of Use and Privacy Policy to continue.");
  }

  if (!legalConsentAcceptedAt || Number.isNaN(Date.parse(String(legalConsentAcceptedAt)))) {
    throw new Error("A valid consent timestamp is required.");
  }

  if (!termsVersionAccepted || !privacyVersionAccepted) {
    throw new Error("Missing legal policy version metadata.");
  }

  return {
    legalConsentAccepted: true,
    legalConsentAcceptedAt,
    legalConsentLocale: legalConsentLocale || "en",
    legalConsentSource: legalConsentSource || "REGISTER_PAGE",
    termsVersionAccepted,
    privacyVersionAccepted,
  };
};

const buildSessionRequestConfig = (token) => {
  const normalizedToken = String(token || "").trim();
  if (!normalizedToken) {
    return undefined;
  }

  return {
    headers: {
      Authorization: `Bearer ${normalizedToken}`,
    },
  };
};

const logBackgroundAuthIssue = (message, error, level = "warn") => {
  const logger = console?.[level];
  if (typeof logger === "function") {
    logger(message, error);
  }
};

export {
  buildAuthUser,
  buildConsentRecord,
  buildSessionRequestConfig,
  firstArrayItemOrNull,
  firstNonEmptyText,
  generateBackendPlaceholderPassword,
  getApiErrorMessage,
  getBlockedStatusMessage,
  getEmailVerificationContinueUrl,
  getFriendlyFirebaseAuthMessage,
  getRequestStatusCode,
  getResponseErrorMessage,
  isConflictError,
  isEmailVerificationBypassed,
  isFirebaseAuthError,
  isOptionalEndpointError,
  isRequestStatusCodeMessage,
  isRetryableBackendSignupLinkError,
  isRetryableCreateEndpointError,
  isValidLinkedInUrl,
  logBackgroundAuthIssue,
  normalizeEmail,
  normalizeLinkedInUrl,
  normalizeOptionalText,
  requireFirebaseConfiguration,
  waitFor,
};
