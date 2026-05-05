import { useCallback, useEffect, useMemo, useState } from "react";
import { getNestedTranslation, translations } from "../constants/translations";
import LanguageContext from "./languageContext";

const LANGUAGE_STORAGE_KEY = "certifiedcarry-language";
const FIRST_VISIT_BOOTSTRAP_KEY = "certifiedcarry-first-visit-bootstrap";

const getInitialLanguage = () => {
  if (globalThis.window === undefined) {
    return "ar";
  }

  const hasBootstrappedFirstVisit =
    globalThis.localStorage.getItem(FIRST_VISIT_BOOTSTRAP_KEY) === "1";

  if (!hasBootstrappedFirstVisit) {
    globalThis.localStorage.setItem(LANGUAGE_STORAGE_KEY, "ar");
    globalThis.localStorage.setItem("certifiedcarry-theme", "light");
    globalThis.localStorage.setItem(FIRST_VISIT_BOOTSTRAP_KEY, "1");
    return "ar";
  }

  const savedLanguage = globalThis.localStorage.getItem(LANGUAGE_STORAGE_KEY);
  if (savedLanguage === "ar" || savedLanguage === "en") {
    return savedLanguage;
  }

  return "ar";
};

const LanguageProvider = ({ children }) => {
  const [language, setLanguage] = useState(getInitialLanguage);
  const isArabic = language === "ar";

  useEffect(() => {
    document.documentElement.setAttribute("lang", language);
    document.documentElement.setAttribute("dir", isArabic ? "rtl" : "ltr");
    globalThis.localStorage.setItem(LANGUAGE_STORAGE_KEY, language);
  }, [isArabic, language]);

  const setAppLanguage = (nextLanguage) => {
    if (nextLanguage === "ar" || nextLanguage === "en") {
      setLanguage(nextLanguage);
    }
  };

  const toggleLanguage = () => {
    setLanguage((currentLanguage) => (currentLanguage === "en" ? "ar" : "en"));
  };

  const t = useCallback(
    (path, fallback = "") => {
      const languageDictionary = translations[language] || translations.en;
      const fallbackDictionary = translations.en;

      const localizedValue = getNestedTranslation(languageDictionary, path);
      if (localizedValue !== undefined) {
        return localizedValue;
      }

      const fallbackValue = getNestedTranslation(fallbackDictionary, path);
      if (fallbackValue !== undefined) {
        return fallbackValue;
      }

      return fallback;
    },
    [language],
  );

  const contextValue = useMemo(
    () => ({
      language,
      isArabic,
      setLanguage: setAppLanguage,
      toggleLanguage,
      t,
    }),
    [isArabic, language, t],
  );

  return <LanguageContext.Provider value={contextValue}>{children}</LanguageContext.Provider>;
};

export default LanguageProvider;
