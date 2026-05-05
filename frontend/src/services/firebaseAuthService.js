import {
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  sendEmailVerification,
  signInWithEmailAndPassword,
  signOut,
} from "firebase/auth";
import { getFirebaseAuth } from "./firebaseClient";

const mapFirebaseSession = async (user, forceRefresh = false) => {
  if (!user) {
    return null;
  }

  if (forceRefresh) {
    await user.reload();
  }

  const idToken = await user.getIdToken(forceRefresh);

  return {
    uid: user.uid,
    email: user.email,
    idToken,
    emailVerified: Boolean(user.emailVerified),
  };
};

const registerWithFirebase = async ({ email, password }) => {
  const auth = getFirebaseAuth();
  const credentials = await createUserWithEmailAndPassword(auth, email, password);
  return mapFirebaseSession(credentials.user);
};

const loginWithFirebase = async ({ email, password }) => {
  const auth = getFirebaseAuth();
  const credentials = await signInWithEmailAndPassword(auth, email, password);
  return mapFirebaseSession(credentials.user);
};

const logoutFromFirebase = async () => {
  const auth = getFirebaseAuth();
  await signOut(auth);
};

const getCurrentFirebaseSession = async ({ forceRefresh = false } = {}) => {
  const auth = getFirebaseAuth();
  return mapFirebaseSession(auth.currentUser, forceRefresh);
};

const sendSignupVerificationEmail = async ({ continueUrl } = {}) => {
  const auth = getFirebaseAuth();
  const currentUser = auth.currentUser;

  if (!currentUser) {
    throw new Error("No active Firebase signup session found.");
  }

  const actionCodeSettings = continueUrl
    ? {
        url: continueUrl,
        handleCodeInApp: false,
      }
    : undefined;

  await sendEmailVerification(currentUser, actionCodeSettings);
};

const observeFirebaseAuthState = (callback) => {
  const auth = getFirebaseAuth();
  return onAuthStateChanged(auth, callback);
};

export {
  getCurrentFirebaseSession,
  loginWithFirebase,
  logoutFromFirebase,
  observeFirebaseAuthState,
  registerWithFirebase,
  sendSignupVerificationEmail,
};
