import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { HeroUIProvider } from "@heroui/react";
import "./index.css";
import App from "./App.jsx";
import AuthProvider from "./context/AuthProvider.jsx";
import LanguageProvider from "./context/LanguageProvider.jsx";
import ThemeProvider from "./context/ThemeProvider.jsx";

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <LanguageProvider>
      <ThemeProvider>
        <HeroUIProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </HeroUIProvider>
      </ThemeProvider>
    </LanguageProvider>
  </StrictMode>,
);
