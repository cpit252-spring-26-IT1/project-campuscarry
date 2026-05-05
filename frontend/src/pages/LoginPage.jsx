import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button, Input } from "@heroui/react";
import { FaEye, FaEyeSlash } from "react-icons/fa";
import { toast } from "react-toastify";
import AuthFormLayout from "../components/AuthFormLayout";
import FormErrorAlert from "../components/FormErrorAlert";
import { getAuthenticatedHomePath } from "../components/routeGuardUtils";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";

const LoginPage = () => {
  const navigate = useNavigate();
  const { loginUser } = useAuth();
  const { t, isArabic } = useLanguage();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submitForm = async (e) => {
    e.preventDefault();
    setSubmitError("");
    setIsSubmitting(true);

    try {
      const loggedInUser = await loginUser({ email, password });
      toast.success(t("auth.welcomeBack"));
      navigate(getAuthenticatedHomePath(loggedInUser));
    } catch (error) {
      const message = error instanceof Error ? error.message : t("auth.unableToSignIn");
      setSubmitError(message);
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <AuthFormLayout title={t("auth.signIn")} onSubmit={submitForm}>
      <FormErrorAlert message={submitError} />

      <div className="mb-4">
        <Input
          type="email"
          label={t("auth.email")}
          labelPlacement="outside"
          placeholder={isArabic ? "you@gmail.com" : "you@gmail.com"}
          value={email}
          onValueChange={setEmail}
        />
      </div>

      <div className="mb-6 pt-3">
        <Input
          type={showPassword ? "text" : "password"}
          label={t("auth.password")}
          labelPlacement="outside"
          placeholder={isArabic ? "12345678" : "12345678"}
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

      <Button
        className="cc-button-text w-full bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
        type="submit"
        isLoading={isSubmitting}
        isDisabled={isSubmitting}
      >
        {isSubmitting ? t("auth.signingIn") : t("auth.signIn")}
      </Button>
    </AuthFormLayout>
  );
};

export default LoginPage;
