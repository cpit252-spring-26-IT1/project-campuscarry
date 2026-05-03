import { useCallback, useEffect, useMemo, useState } from "react";
import AuthContext from "./authContext";
import {
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
} from "../services/authService";
import { isFirebaseConfigured } from "../services/firebaseClient";
import { logoutFromFirebase } from "../services/firebaseAuthService";
import authEventSubject from "../patterns/observer/AuthEventSubject";

const AuthProvider = ({ children }) => {
  const [token, setToken] = useState(null);
  const [user, setUser] = useState(null);
  const [authReady, setAuthReady] = useState(false);

  useEffect(() => {
    let isActive = true;

    const hydrateAuthSession = async () => {
      try {
        const restoredSession = await restoreAuthenticatedSession();
        if (!isActive || !restoredSession) {
          return;
        }

        setToken(restoredSession.token);
        setUser(restoredSession.user);
      } finally {
        if (isActive) {
          setAuthReady(true);
        }
      }
    };

    hydrateAuthSession();

    return () => {
      isActive = false;
    };
  }, []);

  const loginUser = useCallback(async (credentials) => {
    const authResult = await login(credentials);
    setToken(authResult.token);
    setUser(authResult.user);
    authEventSubject.emitLogin(authResult.user);
    await notifySessionLogin({ token: authResult.token });
    return authResult.user;
  }, []);

  const registerUser = useCallback(async (formData) => {
    const registerResult = await register(formData);
    authEventSubject.emitRegister(registerResult?.role || formData?.role || null);
    return registerResult;
  }, []);

  const getSignupVerificationState = useCallback(
    async (options = {}) => getPendingSignupVerificationState(options),
    [],
  );

  const resendSignupVerificationEmail = useCallback(
    async () => resendPendingSignupVerificationEmail(),
    [],
  );

  const completeSignupVerification = useCallback(async () => {
    const completion = await completePendingSignupRegistration();

    if (completion?.shouldAuthenticate && completion?.token && completion?.user) {
      setToken(completion.token);
      setUser(completion.user);
      authEventSubject.emitLogin(completion.user);
      await notifySessionLogin({ token: completion.token });
    }

    return completion;
  }, []);

  const clearSignupVerificationDraft = useCallback(() => {
    clearPendingSignupDraft();
  }, []);

  const deleteCurrentUserAccount = useCallback(async () => {
    const currentUser = user;
    const currentToken = token;

    await deleteCurrentAccount();

    authEventSubject.emitLogout(currentUser);

    if (currentToken) {
      await notifySessionLogout({ token: currentToken });
    }

    setToken(null);
    setUser(null);

    if (isFirebaseConfigured()) {
      await logoutFromFirebase().catch(() => {
        // Keep deletion flow resilient even if Firebase sign-out fails.
      });
    }
  }, [token, user]);

  const logoutUser = useCallback(() => {
    const currentUser = user;
    const currentToken = token;

    authEventSubject.emitLogout(currentUser);

    if (currentToken) {
      notifySessionLogout({ token: currentToken });
    }

    setToken(null);
    setUser(null);

    if (isFirebaseConfigured()) {
      logoutFromFirebase().catch(() => {
        // Keep logout resilient even if Firebase sign-out fails.
      });
    }
  }, [token, user]);

  const contextValue = useMemo(
    () => ({
      token,
      user,
      authReady,
      isAuthenticated: Boolean(token && user),
      loginUser,
      registerUser,
      getSignupVerificationState,
      resendSignupVerificationEmail,
      completeSignupVerification,
      clearSignupVerificationDraft,
      deleteCurrentUserAccount,
      logoutUser,
    }),
    [
      token,
      user,
      authReady,
      loginUser,
      registerUser,
      getSignupVerificationState,
      resendSignupVerificationEmail,
      completeSignupVerification,
      clearSignupVerificationDraft,
      deleteCurrentUserAccount,
      logoutUser,
    ],
  );

  return <AuthContext.Provider value={contextValue}>{children}</AuthContext.Provider>;
};

export default AuthProvider;
