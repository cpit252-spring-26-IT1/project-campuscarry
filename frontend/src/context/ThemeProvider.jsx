import { useEffect, useMemo, useState } from "react";
import ThemeContext from "./themeContext";

const THEME_STORAGE_KEY = "certifiedcarry-theme";
const FIRST_VISIT_BOOTSTRAP_KEY = "certifiedcarry-first-visit-bootstrap";

const getInitialTheme = () => {
  if (globalThis.window === undefined) {
    return "light";
  }

  const hasBootstrappedFirstVisit =
    globalThis.localStorage.getItem(FIRST_VISIT_BOOTSTRAP_KEY) === "1";

  if (!hasBootstrappedFirstVisit) {
    globalThis.localStorage.setItem("certifiedcarry-language", "ar");
    globalThis.localStorage.setItem(THEME_STORAGE_KEY, "light");
    globalThis.localStorage.setItem(FIRST_VISIT_BOOTSTRAP_KEY, "1");
    return "light";
  }

  const savedTheme = globalThis.localStorage.getItem(THEME_STORAGE_KEY);
  if (savedTheme === "dark" || savedTheme === "light") {
    return savedTheme;
  }

  return "light";
};

const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState(getInitialTheme);

  useEffect(() => {
    const isDark = theme === "dark";
    document.documentElement.classList.toggle("dark", isDark);
    document.documentElement.dataset.theme = theme;
    globalThis.localStorage.setItem(THEME_STORAGE_KEY, theme);
  }, [theme]);

  const toggleTheme = () => {
    setTheme((currentTheme) => (currentTheme === "dark" ? "light" : "dark"));
  };

  const setAppTheme = (nextTheme) => {
    if (nextTheme === "dark" || nextTheme === "light") {
      setTheme(nextTheme);
    }
  };

  const contextValue = useMemo(
    () => ({
      theme,
      isDarkMode: theme === "dark",
      setTheme: setAppTheme,
      toggleTheme,
    }),
    [theme],
  );

  return <ThemeContext.Provider value={contextValue}>{children}</ThemeContext.Provider>;
};

export default ThemeProvider;
