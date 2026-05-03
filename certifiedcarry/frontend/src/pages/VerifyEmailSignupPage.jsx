import { Link, useLocation } from "react-router-dom";
import { Button } from "@heroui/react";
import Card from "../components/Card";
import FormErrorAlert from "../components/FormErrorAlert";
import useLanguage from "../hooks/useLanguage";
import useVerifyEmailSignupFlow from "../hooks/useVerifyEmailSignupFlow";

const VERIFY_EMAIL_COPY = {
  en: {
    title: "Verify Your Email",
    subtitle: "Complete email verification to finish creating your account.",
    emailLabel: "Email in progress",
    loadingMessage: "Checking your verification status...",
    noPendingMessage:
      "No pending signup was found on this device. Start a new registration or sign in.",
    createNewAccount: "Create New Account",
    goToLogin: "Go to Login",
    verifiedInstruction: "Email verified. Click Complete Signup to continue.",
    pendingInstruction:
      "Open your email and click the verification link, then return and refresh status.",
    completeSignup: "Complete Signup",
    completingSignup: "Completing...",
    refreshStatus: "Refresh Status",
    refreshingStatus: "Refreshing...",
    resendEmail: "Resend Email",
    sendingEmail: "Sending...",
    signInAnotherAccount: "Sign In with Another Account",
    startOver: "Start Over",
  },
  ar: {
    title: "تحقق من بريدك الإلكتروني",
    subtitle: "أكمل خطوة التحقق حتى نتمكن من إنهاء إنشاء حسابك.",
    emailLabel: "البريد المستخدم",
    loadingMessage: "جار التحقق من حالة البريد الإلكتروني...",
    noPendingMessage:
      "لا يوجد طلب تسجيل قيد الانتظار على هذا الجهاز. يمكنك بدء تسجيل جديد أو تسجيل الدخول.",
    createNewAccount: "إنشاء حساب جديد",
    goToLogin: "الذهاب لتسجيل الدخول",
    verifiedInstruction: "تم تأكيد البريد. اضغط زر إكمال التسجيل للمتابعة.",
    pendingInstruction: "افتح البريد الإلكتروني واضغط رابط التحقق، ثم ارجع واضغط تحديث الحالة.",
    completeSignup: "إكمال التسجيل",
    completingSignup: "جار إكمال التسجيل...",
    refreshStatus: "تحديث الحالة",
    refreshingStatus: "جار التحديث...",
    resendEmail: "إعادة إرسال البريد",
    sendingEmail: "جار الإرسال...",
    signInAnotherAccount: "تسجيل الدخول بحساب آخر",
    startOver: "بدء تسجيل جديد",
  },
};

const VerifyEmailSignupPage = () => {
  const location = useLocation();
  const { isArabic } = useLanguage();
  const copy = isArabic ? VERIFY_EMAIL_COPY.ar : VERIFY_EMAIL_COPY.en;
  const {
    displayEmail,
    submitError,
    isLoadingState,
    isRefreshingState,
    isResending,
    isCompleting,
    hasPendingSignup,
    emailVerified,
    isActionInProgress,
    handleRefreshStatus,
    handleResendVerification,
    handleCompleteSignup,
    handleStartOver,
  } = useVerifyEmailSignupFlow({
    initialEmail: location.state?.email,
    isArabic,
  });

  const pendingInstruction = emailVerified ? copy.verifiedInstruction : copy.pendingInstruction;
  const completeSignupLabel = isCompleting ? copy.completingSignup : copy.completeSignup;
  const refreshStatusLabel = isRefreshingState ? copy.refreshingStatus : copy.refreshStatus;
  const resendEmailLabel = isResending ? copy.sendingEmail : copy.resendEmail;

  return (
    <section className="bg-transparent px-4 py-12">
      <div className="m-auto max-w-3xl">
        <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
          <h1 className="cc-title-section mb-3 text-[#1d1d1d] dark:text-[#cae9ea]">
            {copy.title}
          </h1>

          <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/90">
            {copy.subtitle}
          </p>

          {displayEmail ? (
            <p className="cc-body-muted mt-2 text-[#3c4748] dark:text-[#cae9ea]/80">
              {`${copy.emailLabel}: ${displayEmail}`}
            </p>
          ) : null}

          <FormErrorAlert message={submitError} />

          {isLoadingState ? (
            <p className="cc-body-muted mt-5 text-[#3c4748] dark:text-[#cae9ea]/75">
              {copy.loadingMessage}
            </p>
          ) : null}

          {!isLoadingState && !hasPendingSignup ? (
            <>
              <p className="cc-body-muted mt-5 text-[#3c4748] dark:text-[#cae9ea]/80">
                {copy.noPendingMessage}
              </p>

              <div className="mt-6 flex flex-wrap gap-3">
                <Button
                  as={Link}
                  to="/register"
                  className="cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                >
                  {copy.createNewAccount}
                </Button>
                <Button
                  as={Link}
                  to="/login"
                  variant="bordered"
                  className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]"
                >
                  {copy.goToLogin}
                </Button>
              </div>
            </>
          ) : null}

          {!isLoadingState && hasPendingSignup ? (
            <>
              <p className="cc-body-muted mt-5 text-[#3c4748] dark:text-[#cae9ea]/80">
                {pendingInstruction}
              </p>

              <div className="mt-6 flex flex-wrap gap-3">
                <Button
                  className="cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                  onPress={handleCompleteSignup}
                  isLoading={isCompleting}
                  isDisabled={isActionInProgress}
                >
                  {completeSignupLabel}
                </Button>

                <Button
                  variant="bordered"
                  className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]"
                  onPress={handleRefreshStatus}
                  isLoading={isRefreshingState}
                  isDisabled={isActionInProgress}
                >
                  {refreshStatusLabel}
                </Button>

                <Button
                  variant="bordered"
                  className="cc-button-text border-[#208c8c]/65 text-[#208c8c] transition-colors hover:bg-[#208c8c]/10 dark:border-[#cae9ea]/70 dark:text-[#cae9ea] dark:hover:bg-[#cae9ea]/15"
                  onPress={handleResendVerification}
                  isLoading={isResending}
                  isDisabled={isActionInProgress}
                >
                  {resendEmailLabel}
                </Button>
              </div>

              <div className="mt-5 flex flex-wrap gap-3">
                <Button
                  as={Link}
                  to="/login"
                  variant="light"
                  className="cc-button-text text-[#3c4748] hover:text-[#208c8c] dark:text-[#cae9ea]/85 dark:hover:text-[#cae9ea]"
                >
                  {copy.signInAnotherAccount}
                </Button>
                <Button
                  variant="light"
                  onPress={handleStartOver}
                  className="cc-button-text text-[#3c4748] hover:text-[#208c8c] dark:text-[#cae9ea]/85 dark:hover:text-[#cae9ea]"
                >
                  {copy.startOver}
                </Button>
              </div>
            </>
          ) : null}
        </Card>
      </div>
    </section>
  );
};

export default VerifyEmailSignupPage;
