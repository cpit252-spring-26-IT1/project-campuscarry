import { useCallback, useEffect, useReducer } from "react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { getAuthenticatedHomePath } from "../components/routeGuardUtils";
import useAuth from "./useAuth";

const ACTIONS = {
  SET_ERROR: "set-error",
  SET_LOADING: "set-loading",
  SET_REFRESHING: "set-refreshing",
  SET_RESENDING: "set-resending",
  SET_COMPLETING: "set-completing",
  SET_VERIFICATION_STATE: "set-verification-state",
};

const createInitialState = (initialEmail) => ({
  verificationState: null,
  displayEmail: String(initialEmail || "").trim(),
  submitError: "",
  isLoadingState: true,
  isRefreshingState: false,
  isResending: false,
  isCompleting: false,
});

const verifyEmailSignupReducer = (state, action) => {
  switch (action.type) {
    case ACTIONS.SET_ERROR:
      return { ...state, submitError: String(action.payload || "") };
    case ACTIONS.SET_LOADING:
      return { ...state, isLoadingState: Boolean(action.payload) };
    case ACTIONS.SET_REFRESHING:
      return { ...state, isRefreshingState: Boolean(action.payload) };
    case ACTIONS.SET_RESENDING:
      return { ...state, isResending: Boolean(action.payload) };
    case ACTIONS.SET_COMPLETING:
      return { ...state, isCompleting: Boolean(action.payload) };
    case ACTIONS.SET_VERIFICATION_STATE: {
      const nextVerificationState = action.payload || null;
      const nextEmail = String(nextVerificationState?.email || "").trim();
      return {
        ...state,
        verificationState: nextVerificationState,
        displayEmail: nextEmail || state.displayEmail,
      };
    }
    default:
      return state;
  }
};

const resolveAsyncErrorMessage = ({ error, isArabic, englishFallback, arabicFallback }) => {
  if (error instanceof Error) {
    return error.message;
  }

  return isArabic ? arabicFallback : englishFallback;
};

const useVerifyEmailSignupFlow = ({ initialEmail, isArabic }) => {
  const navigate = useNavigate();
  const {
    clearSignupVerificationDraft,
    completeSignupVerification,
    getSignupVerificationState,
    resendSignupVerificationEmail,
  } = useAuth();

  const [state, dispatch] = useReducer(verifyEmailSignupReducer, initialEmail, createInitialState);

  const loadVerificationState = useCallback(
    async ({ forceRefresh = false, silent = false } = {}) => {
      if (!silent) {
        dispatch({ type: ACTIONS.SET_ERROR, payload: "" });
      }

      try {
        const nextState = await getSignupVerificationState({ forceRefresh });
        dispatch({ type: ACTIONS.SET_VERIFICATION_STATE, payload: nextState });
      } catch (error) {
        dispatch({
          type: ACTIONS.SET_ERROR,
          payload: resolveAsyncErrorMessage({
            error,
            isArabic,
            englishFallback: "Unable to check email verification status.",
            arabicFallback: "تعذر التحقق من حالة البريد الإلكتروني.",
          }),
        });
      } finally {
        dispatch({ type: ACTIONS.SET_LOADING, payload: false });
      }
    },
    [getSignupVerificationState, isArabic],
  );

  useEffect(() => {
    void loadVerificationState({ forceRefresh: true });
  }, [loadVerificationState]);

  const handleRefreshStatus = useCallback(async () => {
    dispatch({ type: ACTIONS.SET_REFRESHING, payload: true });
    await loadVerificationState({ forceRefresh: true, silent: true });
    dispatch({ type: ACTIONS.SET_REFRESHING, payload: false });
  }, [loadVerificationState]);

  const handleResendVerification = useCallback(async () => {
    dispatch({ type: ACTIONS.SET_ERROR, payload: "" });
    dispatch({ type: ACTIONS.SET_RESENDING, payload: true });

    try {
      const resendResult = await resendSignupVerificationEmail();
      if (resendResult?.alreadyVerified) {
        toast.success(isArabic ? "تم التحقق من البريد الإلكتروني بالفعل." : "This email is already verified.");
      } else {
        toast.success(isArabic ? "تم إرسال رابط تحقق جديد." : "A new verification email has been sent.");
      }

      await loadVerificationState({ forceRefresh: true, silent: true });
    } catch (error) {
      const errorMessage = resolveAsyncErrorMessage({
        error,
        isArabic,
        englishFallback: "Unable to resend the verification email.",
        arabicFallback: "تعذر إعادة إرسال البريد.",
      });
      dispatch({ type: ACTIONS.SET_ERROR, payload: errorMessage });
      toast.error(errorMessage);
    } finally {
      dispatch({ type: ACTIONS.SET_RESENDING, payload: false });
    }
  }, [isArabic, loadVerificationState, resendSignupVerificationEmail]);

  const handleCompleteSignup = useCallback(async () => {
    dispatch({ type: ACTIONS.SET_ERROR, payload: "" });
    dispatch({ type: ACTIONS.SET_COMPLETING, payload: true });

    try {
      const completion = await completeSignupVerification();

      if (completion?.shouldAuthenticate && completion?.user) {
        toast.success(isArabic ? "تم تفعيل حسابك بنجاح." : "Your account is verified and ready.");
        navigate(getAuthenticatedHomePath(completion.user), { replace: true });
        return;
      }

      if (completion?.user?.role === "RECRUITER") {
        toast.success(
          isArabic
            ? "تم إرسال طلبك للمراجعة."
            : "Registration submitted. Your scout account is now pending admin approval.",
        );
        navigate("/pending-approval", { replace: true });
        return;
      }

      toast.success(isArabic ? "تمت العملية بنجاح." : "Signup completed. Please sign in.");
      navigate("/login", { replace: true });
    } catch (error) {
      const errorMessage = resolveAsyncErrorMessage({
        error,
        isArabic,
        englishFallback: "Unable to complete signup.",
        arabicFallback: "تعذر إكمال التسجيل.",
      });
      dispatch({ type: ACTIONS.SET_ERROR, payload: errorMessage });
      toast.error(errorMessage);
    } finally {
      dispatch({ type: ACTIONS.SET_COMPLETING, payload: false });
    }
  }, [completeSignupVerification, isArabic, navigate]);

  const handleStartOver = useCallback(() => {
    clearSignupVerificationDraft();
    navigate("/register", { replace: true });
  }, [clearSignupVerificationDraft, navigate]);

  return {
    verificationState: state.verificationState,
    displayEmail: state.displayEmail,
    submitError: state.submitError,
    isLoadingState: state.isLoadingState,
    isRefreshingState: state.isRefreshingState,
    isResending: state.isResending,
    isCompleting: state.isCompleting,
    hasPendingSignup: Boolean(state.verificationState?.hasPendingSignup),
    emailVerified: Boolean(state.verificationState?.emailVerified),
    isActionInProgress: state.isCompleting || state.isResending || state.isRefreshingState,
    handleRefreshStatus,
    handleResendVerification,
    handleCompleteSignup,
    handleStartOver,
  };
};

export default useVerifyEmailSignupFlow;
