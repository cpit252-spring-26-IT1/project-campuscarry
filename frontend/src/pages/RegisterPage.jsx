import { useEffect, useState } from "react";
import { Link as RouterLink, useNavigate, useSearchParams } from "react-router-dom";
import { Button, Checkbox, Input, Link, Select, SelectItem } from "@heroui/react";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { toast } from "react-toastify";
import AuthFormLayout from "../components/AuthFormLayout";
import FormErrorAlert from "../components/FormErrorAlert";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";
import {
  LEGAL_PRIVACY_VERSION,
  LEGAL_TERMS_VERSION,
} from "../constants/legalConfig";
import { getSelectValue } from "../utils/selectHelpers";

const RegisterPage = () => {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { registerUser } = useAuth();
  const { t, isArabic } = useLanguage();

  const [role, setRole] = useState("PLAYER");
  const [fullName, setFullName] = useState("");
  const [username, setUsername] = useState("");
  const [playerEmail, setPlayerEmail] = useState("");
  const [organizationName, setOrganizationName] = useState("");
  const [scoutEmail, setScoutEmail] = useState("");
  const [linkedInUrl, setLinkedInUrl] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [hasAcceptedLegal, setHasAcceptedLegal] = useState(false);

  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const isPlayer = role === "PLAYER";
  const labelAlignmentClass = isArabic ? "text-right" : "text-left";
  const fieldLabelClass = `mb-2 text-sm font-semibold text-[#1d1d1d] dark:text-[#cae9ea] ${labelAlignmentClass}`;

  useEffect(() => {
    const roleFromQuery = searchParams.get("role")?.toUpperCase();
    if (["PLAYER", "RECRUITER"].includes(roleFromQuery)) {
      setRole(roleFromQuery);
    }
  }, [searchParams]);

  const validateForm = () => {
    if (!fullName || !password || !confirmPassword || !role) {
      return t("auth.unableToCompleteRegistration") || "Please complete all required fields.";
    }

    if (password !== confirmPassword) {
      return t("auth.passwordsDoNotMatch") || "Password and confirm password must match.";
    }

    if (isPlayer) {
      if (!username || !playerEmail) {
        return (
          t("auth.playerEmailRequired") ||
          "Player registration requires full name, gamertag, email, and password."
        );
      }
    } else if (!organizationName || !scoutEmail || !linkedInUrl) {
      return (
        t("auth.recruiterFieldsRequired") ||
        "Scout registration requires full name, organization, email, LinkedIn URL, and password."
      );
    }

    if (!hasAcceptedLegal) {
      return (
        t("auth.legalConsentRequired") ||
        "You must accept the Terms of Use and Privacy Policy to continue."
      );
    }

    return "";
  };

  const handleRoleChange = (keys) => {
    const selectedRole = getSelectValue(keys);

    if (selectedRole) {
      setRole(selectedRole);
      setSubmitError("");
    }
  };

  const handleLegalLinkClick = (path) => (event) => {
    event.preventDefault();
    event.stopPropagation();
    navigate(path);
  };

  const submitForm = async (event) => {
    event.preventDefault();
    setSubmitError("");

    const validationError = validateForm();
    if (validationError) {
      setSubmitError(validationError);
      toast.error(validationError);
      return;
    }

    setIsSubmitting(true);

    try {
      const consentTimestamp = new Date().toISOString();
      const consentPayload = {
        legalConsentAccepted: true,
        legalConsentAcceptedAt: consentTimestamp,
        legalConsentLocale: isArabic ? "ar" : "en",
        legalConsentSource: "REGISTER_PAGE",
        termsVersionAccepted: LEGAL_TERMS_VERSION,
        privacyVersionAccepted: LEGAL_PRIVACY_VERSION,
      };

      const registrationPayload = isPlayer
        ? {
            role,
            fullName,
            username,
            personalEmail: playerEmail,
            password,
            ...consentPayload,
          }
        : {
            role,
            fullName,
            organizationName,
            email: scoutEmail,
            linkedinUrl: linkedInUrl,
            password,
            ...consentPayload,
          };

      const registrationResult = await registerUser(registrationPayload);
      const verificationEmail =
        registrationResult?.email || (isPlayer ? String(playerEmail || "").trim() : scoutEmail);

      if (registrationResult?.verificationEmailSent) {
        toast.success(
          isArabic
            ? "تم إرسال رابط التحقق إلى بريدك الإلكتروني."
            : "Verification email sent. Check your inbox to continue.",
        );
      } else {
        toast.info(
          isArabic
            ? "تم إنشاء الحساب. تابع خطوة التحقق، ويمكنك إعادة إرسال البريد من الصفحة التالية."
            : "Account created. Continue to verification and resend the email from the next step if needed.",
        );
      }

      navigate("/verify-email-signup", {
        state: {
          email: verificationEmail,
          role: registrationResult?.role || role,
        },
      });
    } catch (error) {
      const message =
        error instanceof Error ? error.message : t("auth.unableToCompleteRegistration");
      setSubmitError(message);
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthFormLayout
      title={t("auth.createAccount")}
      onSubmit={submitForm}
      footer={
        <p className="cc-body-muted mt-4 text-center text-[#3c4748] dark:text-[#cae9ea]/85">
          {t("auth.alreadyHaveAccount")}{" "}
          <Link
            as={RouterLink}
            to="/login"
            className="text-[#208c8c] hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
          >
            {t("auth.signInLink")}
          </Link>
        </p>
      }
    >
      <FormErrorAlert message={submitError} />

      <div className="mb-4 pt-1">
        <p className={fieldLabelClass}>{t("auth.role")}</p>
        <Select selectedKeys={[role]} onSelectionChange={handleRoleChange}>
          <SelectItem key="PLAYER">{isArabic ? "لاعب" : "Player"}</SelectItem>
          <SelectItem key="RECRUITER">{isArabic ? "مستقطب" : "Scout"}</SelectItem>
        </Select>
      </div>

      <div className="mb-4 pt-2">
        <p className={fieldLabelClass}>{t("auth.fullName")}</p>
        <Input
          type="text"
          isRequired
          placeholder="Saud Alshehri"
          value={fullName}
          onValueChange={setFullName}
        />
      </div>

      {isPlayer ? (
        <>
          <div className="mb-4 pt-2">
            <p className={fieldLabelClass}>{isArabic ? "اسمك في اللعبه" : t("auth.gamertag")}</p>
            <Input
              type="text"
              isRequired
              placeholder="CertifiedCarry"
              value={username}
              onValueChange={setUsername}
            />
          </div>

          <div className="mb-4 pt-2">
            <p className={fieldLabelClass}>{t("auth.playerEmail")}</p>
            <Input
              type="email"
              isRequired
              placeholder="you@gmail.com"
              value={playerEmail}
              onValueChange={setPlayerEmail}
            />
          </div>
        </>
      ) : (
        <>
          <div className="mb-4 pt-2">
            <p className={fieldLabelClass}>{t("auth.organizationName")}</p>
            <Input
              type="text"
              isRequired
              placeholder="Falcons Esports"
              value={organizationName}
              onValueChange={setOrganizationName}
            />
          </div>

          <div className="mb-4 pt-2">
            <p className={fieldLabelClass}>{t("auth.email")}</p>
            <Input
              type="email"
              isRequired
              placeholder="recruitment@team.sa"
              value={scoutEmail}
              onValueChange={setScoutEmail}
            />
          </div>

          <div className="mb-4 pt-2">
            <p className={fieldLabelClass}>{t("auth.linkedInUrl")}</p>
            <Input
              type="url"
              isRequired
              placeholder="https://www.linkedin.com/in/your-profile"
              value={linkedInUrl}
              onValueChange={setLinkedInUrl}
            />
            <p className="mt-2 text-xs text-[#3c4748] dark:text-[#cae9ea]/75">
              To confirm affiliation with the organization. If that is not possible, contact support
              to validate your credibility.
            </p>
          </div>
        </>
      )}

      <div className="mb-6 pt-2">
        <p className={fieldLabelClass}>{t("auth.password")}</p>
        <Input
          type={showPassword ? "text" : "password"}
          isRequired
          placeholder={isArabic ? "كلمة مرور قوية" : "A strong password"}
          value={password}
          onValueChange={setPassword}
          endContent={
            <button
              type="button"
              onClick={() => setShowPassword((currentValue) => !currentValue)}
              className="text-[#273b40] transition-colors hover:text-[#208c8c] dark:text-[#cae9ea]/80 dark:hover:text-[#cae9ea]"
              aria-label={isArabic ? "إظهار أو إخفاء كلمة المرور" : "Toggle password visibility"}
            >
              {showPassword ? <FaEyeSlash /> : <FaEye />}
            </button>
          }
        />
      </div>

      <div className="mb-6 pt-2">
        <p className={fieldLabelClass}>{t("auth.confirmPassword")}</p>
        <Input
          type={showConfirmPassword ? "text" : "password"}
          isRequired
          placeholder={isArabic ? "أعد إدخال كلمة المرور" : "Re-enter your password"}
          value={confirmPassword}
          onValueChange={setConfirmPassword}
          endContent={
            <button
              type="button"
              onClick={() => setShowConfirmPassword((currentValue) => !currentValue)}
              className="text-[#273b40] transition-colors hover:text-[#208c8c] dark:text-[#cae9ea]/80 dark:hover:text-[#cae9ea]"
              aria-label={
                isArabic ? "إظهار أو إخفاء تأكيد كلمة المرور" : "Toggle confirm password visibility"
              }
            >
              {showConfirmPassword ? <FaEyeSlash /> : <FaEye />}
            </button>
          }
        />
      </div>

      <div className="mb-6 rounded-lg border border-[#3c4748]/35 bg-[#eaf4f4]/70 p-4 dark:border-[#3c4748]/70 dark:bg-[#1d1d1d]/35">
        <div className="flex items-start gap-3">
          <Checkbox
            isSelected={hasAcceptedLegal}
            onValueChange={(nextValue) => {
              setHasAcceptedLegal(nextValue);

              if (nextValue) {
                setSubmitError("");
              }
            }}
            color="success"
            size="sm"
            className="mt-0.5"
            aria-label={
              t("auth.legalConsentRequired") ||
              "You must accept the Terms of Use and Privacy Policy to continue."
            }
          />

          <div className="flex-1">
            <p className="cc-body-muted pt-0.5 text-[#273b40] dark:text-[#cae9ea]">
              {t("auth.legalConsentPrefix", isArabic ? "أوافق على" : "I agree to the")}{" "}
              <Link
                as={RouterLink}
                to="/terms"
                onClick={handleLegalLinkClick("/terms")}
                className="cc-body-muted inline text-[#208c8c] underline underline-offset-2 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
              >
                {t("legal.termsTitle", isArabic ? "شروط الاستخدام" : "Terms of Use")}
              </Link>{" "}
              {t("auth.legalConsentAnd", isArabic ? "و" : "and")}{" "}
              <Link
                as={RouterLink}
                to="/privacy"
                onClick={handleLegalLinkClick("/privacy")}
                className="cc-body-muted inline text-[#208c8c] underline underline-offset-2 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
              >
                {t("legal.privacyTitle", isArabic ? "سياسة الخصوصية" : "Privacy Policy")}
              </Link>
              {t("auth.legalConsentSuffix", ".")}
            </p>
          </div>
        </div>
      </div>

      <Button
        className="cc-button-text w-full bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
        type="submit"
        isLoading={isSubmitting}
        isDisabled={isSubmitting}
      >
        {isSubmitting ? t("auth.creatingAccount") : t("auth.register")}
      </Button>
    </AuthFormLayout>
  );
};

export default RegisterPage;
