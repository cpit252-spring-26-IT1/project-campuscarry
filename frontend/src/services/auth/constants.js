const ROLE_PLAYER = "PLAYER";
const ROLE_RECRUITER = "RECRUITER";
const ROLE_ADMIN = "ADMIN";

const FIREBASE_REQUIRED_MESSAGE =
  "Firebase authentication is required in this environment. Configure VITE_FIREBASE_* variables.";
const UNVERIFIED_SIGNIN_MESSAGE =
  "Please verify your email before signing in. Check your inbox for the verification link.";

const PENDING_SIGNUP_STORAGE_KEY = "cc.pendingSignupDraft";
const PENDING_SIGNUP_MAX_AGE_MS = 24 * 60 * 60 * 1000;

const BACKEND_USER_LOOKUP_RETRY_ATTEMPTS = 4;
const BACKEND_USER_LOOKUP_RETRY_DELAY_MS = 450;
const BACKEND_USER_CREATE_RETRY_ATTEMPTS = 6;
const BACKEND_USER_CREATE_RETRY_DELAY_MS = 1000;
const BACKEND_USER_CREATE_RETRYABLE_STATUS_CODES = new Set([400, 401, 403, 429]);
const BACKEND_USER_CREATE_ENDPOINTS = ["/auth/signup/complete", "/users"];

const REQUEST_FAILED_STATUS_CODE_PREFIX = "request failed with status code";
const RETRYABLE_SIGNUP_LINK_ERROR_SNIPPETS = [
  "firebase account email must be verified",
  "firebase token email does not match",
  "firebase authentication is required",
  "unable to complete player registration",
  "unable to complete recruiter registration",
];

const OPTIONAL_ENDPOINT_STATUS_CODES = new Set([404, 405, 501]);

export {
  BACKEND_USER_CREATE_ENDPOINTS,
  BACKEND_USER_CREATE_RETRY_ATTEMPTS,
  BACKEND_USER_CREATE_RETRY_DELAY_MS,
  BACKEND_USER_CREATE_RETRYABLE_STATUS_CODES,
  BACKEND_USER_LOOKUP_RETRY_ATTEMPTS,
  BACKEND_USER_LOOKUP_RETRY_DELAY_MS,
  FIREBASE_REQUIRED_MESSAGE,
  OPTIONAL_ENDPOINT_STATUS_CODES,
  PENDING_SIGNUP_MAX_AGE_MS,
  PENDING_SIGNUP_STORAGE_KEY,
  REQUEST_FAILED_STATUS_CODE_PREFIX,
  RETRYABLE_SIGNUP_LINK_ERROR_SNIPPETS,
  ROLE_ADMIN,
  ROLE_PLAYER,
  ROLE_RECRUITER,
  UNVERIFIED_SIGNIN_MESSAGE,
};
