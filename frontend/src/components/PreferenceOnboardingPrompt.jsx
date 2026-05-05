import { useState } from "react";
import { Button } from "@heroui/react";
import Card from "./Card";
import useLanguage from "../hooks/useLanguage";
import useTheme from "../hooks/useTheme";
import { trackUiEvent } from "../services/analyticsService";

const PREFERENCE_ONBOARDING_STORAGE_KEY = "cc.preference-onboarding-complete";
const ACTIVE_CHOICE_CLASS = "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]";
const INACTIVE_CHOICE_CLASS =
  "cc-button-text border-[#3c4748]/50 text-[#1d1d1d] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30";

const PreferenceOnboardingPrompt = () => {
  const { isArabic, setLanguage } = useLanguage();
  const { isDarkMode, setTheme } = useTheme();
  const [isVisible, setIsVisible] = useState(() => {
    if (globalThis.window === undefined) {
      return false;
    }

    const onboardingState = String(
      globalThis.localStorage.getItem(PREFERENCE_ONBOARDING_STORAGE_KEY) || "",
    ).trim();

    return !onboardingState;
  });

  const closePrompt = (reason) => {
    globalThis.localStorage.setItem(PREFERENCE_ONBOARDING_STORAGE_KEY, "done");
    setIsVisible(false);
    trackUiEvent("preference_onboarding_completed", {
      reason,
    });
  };

  if (!isVisible) {
    return null;
  }

  return (
    <Card
      bg="bg-[#f4fbfb]/92 dark:bg-[#273b40]/70"
      className="mb-4 border-[#208c8c]/40 shadow-xl backdrop-blur-sm"
    >
      <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
        {isArabic ? "تخصيص سريع" : "Quick Personalization"}
      </h2>
      <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
        {isArabic
          ? "اختر اللغة والمظهر المفضلين لك. يمكنك تعديلهما لاحقا من الإعدادات."
          : "Pick your preferred language and theme. You can change both anytime in Settings."}
      </p>

      <div className="mt-4 space-y-4">
        <div>
          <p className="cc-body-muted mb-2 text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic ? "اللغة" : "Language"}
          </p>
          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant={isArabic ? "solid" : "bordered"}
              className={isArabic ? ACTIVE_CHOICE_CLASS : INACTIVE_CHOICE_CLASS}
              aria-pressed={isArabic}
              onPress={() => {
                setLanguage("ar");
                trackUiEvent("preference_onboarding_language_selected", { language: "ar" });
              }}
            >
              العربية
            </Button>
            <Button
              type="button"
              variant={isArabic ? "bordered" : "solid"}
              className={!isArabic ? ACTIVE_CHOICE_CLASS : INACTIVE_CHOICE_CLASS}
              aria-pressed={!isArabic}
              onPress={() => {
                setLanguage("en");
                trackUiEvent("preference_onboarding_language_selected", { language: "en" });
              }}
            >
              English
            </Button>
          </div>
        </div>

        <div>
          <p className="cc-body-muted mb-2 text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic ? "المظهر" : "Theme"}
          </p>
          <div className="flex flex-wrap gap-2">
            <Button
              type="button"
              variant={isDarkMode ? "bordered" : "solid"}
              className={!isDarkMode ? ACTIVE_CHOICE_CLASS : INACTIVE_CHOICE_CLASS}
              aria-pressed={!isDarkMode}
              onPress={() => {
                setTheme("light");
                trackUiEvent("preference_onboarding_theme_selected", { theme: "light" });
              }}
            >
              {isArabic ? "الوضع الفاتح" : "Light Theme"}
            </Button>
            <Button
              type="button"
              variant={isDarkMode ? "solid" : "bordered"}
              className={isDarkMode ? ACTIVE_CHOICE_CLASS : INACTIVE_CHOICE_CLASS}
              aria-pressed={isDarkMode}
              onPress={() => {
                setTheme("dark");
                trackUiEvent("preference_onboarding_theme_selected", { theme: "dark" });
              }}
            >
              {isArabic ? "الوضع الداكن" : "Dark Theme"}
            </Button>
          </div>
        </div>
      </div>

      <div className="mt-4 flex justify-end">
        <Button
          type="button"
          size="sm"
          variant="light"
          className="cc-button-text text-[#273b40] dark:text-[#cae9ea]"
          onPress={() => closePrompt("dismiss")}
        >
          {isArabic ? "تم" : "Done"}
        </Button>
      </div>
    </Card>
  );
};

export default PreferenceOnboardingPrompt;
