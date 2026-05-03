import { getApp, getApps, initializeApp } from "firebase/app";
import { getAuth } from "firebase/auth";

const firebaseConfig = {
  apiKey: String(import.meta.env.VITE_FIREBASE_API_KEY || "").trim(),
  authDomain: String(import.meta.env.VITE_FIREBASE_AUTH_DOMAIN || "").trim(),
  projectId: String(import.meta.env.VITE_FIREBASE_PROJECT_ID || "").trim(),
  storageBucket: String(import.meta.env.VITE_FIREBASE_STORAGE_BUCKET || "").trim(),
  messagingSenderId: String(import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID || "").trim(),
  appId: String(import.meta.env.VITE_FIREBASE_APP_ID || "").trim(),
  measurementId: String(import.meta.env.VITE_FIREBASE_MEASUREMENT_ID || "").trim(),
};

const hasRequiredFirebaseConfig =
  Boolean(firebaseConfig.apiKey) &&
  Boolean(firebaseConfig.authDomain) &&
  Boolean(firebaseConfig.projectId) &&
  Boolean(firebaseConfig.appId);

let firebaseApp = null;
let firebaseAuth = null;

if (hasRequiredFirebaseConfig) {
  firebaseApp = getApps().length > 0 ? getApp() : initializeApp(firebaseConfig);
  firebaseAuth = getAuth(firebaseApp);
}

const isFirebaseConfigured = () => hasRequiredFirebaseConfig;

const getFirebaseAuth = () => {
  if (!firebaseAuth) {
    throw new Error(
      "Firebase auth is not configured. Add Firebase keys to your Vite environment variables.",
    );
  }

  return firebaseAuth;
};

const getCurrentFirebaseIdToken = async () => {
  if (!firebaseAuth?.currentUser) {
    return null;
  }

  return firebaseAuth.currentUser.getIdToken();
};

export {
  firebaseApp,
  firebaseConfig,
  getCurrentFirebaseIdToken,
  getFirebaseAuth,
  isFirebaseConfigured,
};
