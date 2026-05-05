import { RECRUITER_DM_OPENNESS } from "../recruiterService";
import {
  PENDING_SIGNUP_MAX_AGE_MS,
  PENDING_SIGNUP_STORAGE_KEY,
  ROLE_PLAYER,
  ROLE_RECRUITER,
} from "./constants";
import { generateBackendPlaceholderPassword, normalizeEmail } from "./shared";

const normalizePendingSignupDraft = (rawDraft) => {
  if (!rawDraft || typeof rawDraft !== "object") {
    return null;
  }

  const role = String(rawDraft.role || "")
    .trim()
    .toUpperCase();
  if (role !== ROLE_PLAYER && role !== ROLE_RECRUITER) {
    return null;
  }

  const email = normalizeEmail(String(rawDraft.email || ""));
  const fullName = String(rawDraft.fullName || "").trim();
  const createdAt = String(rawDraft.createdAt || "").trim();
  const consentRecord = rawDraft.consentRecord;

  if (!email || !fullName || !createdAt || Number.isNaN(Date.parse(createdAt))) {
    return null;
  }

  if (!consentRecord || typeof consentRecord !== "object") {
    return null;
  }

  if (role === ROLE_PLAYER) {
    const username = String(rawDraft.username || "").trim();
    if (!username) {
      return null;
    }

    return {
      role,
      email,
      fullName,
      username,
      consentRecord,
      createdAt,
    };
  }

  const organizationName = String(rawDraft.organizationName || "").trim();
  const linkedinUrl = String(rawDraft.linkedinUrl || "").trim();
  if (!organizationName || !linkedinUrl) {
    return null;
  }

  return {
    role,
    email,
    fullName,
    organizationName,
    linkedinUrl,
    consentRecord,
    createdAt,
  };
};

const persistPendingSignupDraft = (draft) => {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(PENDING_SIGNUP_STORAGE_KEY, JSON.stringify(draft));
  window.sessionStorage.removeItem(PENDING_SIGNUP_STORAGE_KEY);
};

const clearPendingSignupDraft = () => {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(PENDING_SIGNUP_STORAGE_KEY);
  window.sessionStorage.removeItem(PENDING_SIGNUP_STORAGE_KEY);
};

const parseStoredPendingDraft = (rawValue) => {
  if (!rawValue) {
    return null;
  }

  const parsedDraft = normalizePendingSignupDraft(JSON.parse(rawValue));
  if (!parsedDraft) {
    return null;
  }

  const ageMs = Date.now() - Date.parse(parsedDraft.createdAt);
  if (ageMs > PENDING_SIGNUP_MAX_AGE_MS) {
    return null;
  }

  return parsedDraft;
};

const readPendingSignupDraft = () => {
  if (typeof window === "undefined") {
    return null;
  }

  try {
    const localDraft = parseStoredPendingDraft(
      window.localStorage.getItem(PENDING_SIGNUP_STORAGE_KEY),
    );
    if (localDraft) {
      return localDraft;
    }

    window.localStorage.removeItem(PENDING_SIGNUP_STORAGE_KEY);

    const legacySessionDraft = parseStoredPendingDraft(
      window.sessionStorage.getItem(PENDING_SIGNUP_STORAGE_KEY),
    );
    if (!legacySessionDraft) {
      window.sessionStorage.removeItem(PENDING_SIGNUP_STORAGE_KEY);
      return null;
    }

    window.localStorage.setItem(PENDING_SIGNUP_STORAGE_KEY, JSON.stringify(legacySessionDraft));
    window.sessionStorage.removeItem(PENDING_SIGNUP_STORAGE_KEY);
    return legacySessionDraft;
  } catch {
    clearPendingSignupDraft();
    return null;
  }
};

const doesPendingDraftMatchFirebaseSession = (pendingDraft, firebaseSession) => {
  const normalizedSessionEmail = normalizeEmail(String(firebaseSession?.email || ""));
  return Boolean(normalizedSessionEmail) && normalizedSessionEmail === pendingDraft.email;
};

const buildCreateUserPayloadFromPendingDraft = (pendingDraft) => {
  const generatedPassword = generateBackendPlaceholderPassword();

  if (pendingDraft.role === ROLE_PLAYER) {
    return {
      fullName: pendingDraft.fullName,
      username: pendingDraft.username,
      personalEmail: pendingDraft.email,
      email: null,
      password: generatedPassword,
      role: ROLE_PLAYER,
      status: "APPROVED",
      ...pendingDraft.consentRecord,
    };
  }

  return {
    fullName: pendingDraft.fullName,
    email: pendingDraft.email,
    organizationName: pendingDraft.organizationName,
    linkedinUrl: pendingDraft.linkedinUrl,
    password: generatedPassword,
    role: ROLE_RECRUITER,
    status: "PENDING",
    declineReason: "",
    declinedAt: null,
    recruiterDmOpenness: RECRUITER_DM_OPENNESS.CLOSED,
    username: null,
    personalEmail: null,
    ...pendingDraft.consentRecord,
  };
};

export {
  buildCreateUserPayloadFromPendingDraft,
  clearPendingSignupDraft,
  doesPendingDraftMatchFirebaseSession,
  normalizePendingSignupDraft,
  persistPendingSignupDraft,
  readPendingSignupDraft,
};
