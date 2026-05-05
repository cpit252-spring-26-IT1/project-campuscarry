import { Link, Outlet, useLocation } from "react-router-dom";
import { useEffect } from "react";
import Navbar from "../components/Navbar";
import FlickeringGrid from "../components/FlickeringGrid";
import { ToastContainer } from "react-toastify";
import useTheme from "../hooks/useTheme";
import useLanguage from "../hooks/useLanguage";
import logoDarkMode from "../assets/images/logo-darkmode.svg";
import logoLightMode from "../assets/images/logo-lightmode.svg";
import authEventSubject from "../patterns/observer/AuthEventSubject";
import Observer from "../patterns/observer/Observer";
import "react-toastify/dist/ReactToastify.css";

const AUTH_EVENT_STORAGE_KEY = "cc.lastAuthEvent";
const FOOTER_LINK_CLASS =
  "cc-body-muted text-[#208c8c] hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]";

const PLATFORM_LINKS = [
  { to: "/about", labelAr: "عن المنصة", labelEn: "About" },
  { to: "/contact", labelAr: "تواصل معنا", labelEn: "Contact" },
];

const LEGAL_LINKS = [
  { to: "/terms", labelAr: "شروط الخدمة", labelEn: "Terms of Service" },
  { to: "/privacy", labelAr: "سياسة الخصوصية", labelEn: "Privacy Policy" },
  {
    to: "/cookies",
    labelAr: "سياسة ملفات تعريف الارتباط",
    labelEn: "Cookie Policy",
  },
  {
    to: "/community-guidelines",
    labelAr: "إرشادات المجتمع",
    labelEn: "Community Guidelines",
  },
];

class AuthAuditObserver extends Observer {
  update(event) {
    if (globalThis.window === undefined) {
      return;
    }

    globalThis.sessionStorage.setItem(AUTH_EVENT_STORAGE_KEY, JSON.stringify(event));
  }
}

const MainLayout = () => {
  const { theme } = useTheme();
  const { isArabic } = useLanguage();
  const { pathname } = useLocation();
  const isLanding = pathname === "/";
  const gridColor = theme === "dark" ? "#208c8c" : "#3c4748";
  const gridMaxOpacity = theme === "dark" ? 0.35 : 0.2;
  const footerTextAlignClass = isArabic ? "text-right" : "text-left";

  useEffect(() => {
    const authAuditObserver = new AuthAuditObserver();
    const unsubscribe = authEventSubject.subscribe(authAuditObserver);

    return () => {
      unsubscribe();
    };
  }, []);

  return (
    <div className="relative flex min-h-screen flex-col overflow-hidden bg-transparent">
      <FlickeringGrid
        className="pointer-events-none absolute inset-0 z-0"
        squareSize={4}
        gridGap={8}
        color={gridColor}
        maxOpacity={gridMaxOpacity}
        flickerChance={0.28}
      />
      <div
        className={`pointer-events-none absolute inset-0 z-0 ${
          theme === "dark" ? "bg-[#1d1d1d]/45" : "bg-[#eaf4f4]/30"
        }`}
        aria-hidden="true"
      />
      <div className="relative z-10 flex min-h-screen flex-col">
        <Navbar />
        <main className={`cc-page-host flex-1 ${isLanding ? "" : "pt-36 xl:pt-24"}`}>
          <Outlet />
        </main>
        <footer className="border-t border-[#3c4748]/25 bg-[#f4fbfb]/75 px-4 py-8 text-[#273b40] backdrop-blur-sm dark:border-[#3c4748]/55 dark:bg-[#1d1d1d]/65 dark:text-[#cae9ea] sm:px-6 lg:px-8">
          <div className={`mx-auto w-full max-w-6xl ${footerTextAlignClass}`}>
            <div className="grid grid-cols-1 gap-8 lg:grid-cols-3">
              <div>
                <Link to="/" className="inline-flex items-center gap-2 sm:gap-3">
                  <img
                    src={theme === "dark" ? logoDarkMode : logoLightMode}
                    alt="CertifiedCarry"
                    className="h-10 w-auto shrink-0 sm:h-12 md:h-14"
                  />
                  <span className="whitespace-nowrap font-display text-lg font-bold tracking-tight sm:text-xl md:text-2xl">
                    CertifiedCarry
                  </span>
                </Link>
                <p className="cc-body-muted mt-0 text-[#3c4748] dark:text-[#cae9ea]/80">
                  {isArabic
                    ? "موطن المواهب السعودية التنافسية"
                    : "The home of Saudi competitive talent"}
                </p>
              </div>

              <div className="grid grid-cols-1 gap-6 sm:grid-cols-2">
                <div>
                  <p className="cc-body-muted font-semibold text-[#1d1d1d] dark:text-[#cae9ea]">
                    {isArabic ? "المنصة" : "Platform"}
                  </p>
                  <div className="mt-3 flex flex-col gap-2">
                    {PLATFORM_LINKS.map((item) => (
                      <Link key={item.to} to={item.to} className={FOOTER_LINK_CLASS}>
                        {isArabic ? item.labelAr : item.labelEn}
                      </Link>
                    ))}
                  </div>
                </div>

                <div>
                  <p className="cc-body-muted font-semibold text-[#1d1d1d] dark:text-[#cae9ea]">
                    {isArabic ? "القانونية" : "Legal"}
                  </p>
                  <div className="mt-3 flex flex-col gap-2">
                    {LEGAL_LINKS.map((item) => (
                      <Link key={item.to} to={item.to} className={FOOTER_LINK_CLASS}>
                        {isArabic ? item.labelAr : item.labelEn}
                      </Link>
                    ))}
                  </div>
                </div>
              </div>

              <div className={isArabic ? "lg:text-left" : ""}>
                <p className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                  {isArabic ? "جاهز لتلفت أنظار المستقطبين؟" : "Ready to get scouted?"}
                </p>
                <Link
                  to="/register?role=PLAYER"
                  className="cc-button-text mt-4 inline-flex items-center rounded-md bg-[#208c8c] px-4 py-2 text-[#cae9ea] transition-colors hover:bg-[#273b40]"
                >
                  {isArabic ? "إنشاء حساب" : "Register"}
                </Link>
              </div>
            </div>

            <div className="mt-7 border-t border-[#3c4748]/25 pt-4 text-xs text-[#3c4748] dark:text-[#cae9ea]/75">
              <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                <p>
                  © 2026 <span className="font-display">CertifiedCarry™</span>. All rights reserved.
                </p>
                <p>{isArabic ? "صنع في المملكة العربية السعودية 🇸🇦" : "Made in Saudi Arabia 🇸🇦"}</p>
              </div>
              <p className="mt-2 text-center text-[11px] text-[#3c4748] dark:text-[#cae9ea]/70">
                {isArabic
                  ? "تنبيه النسخة التجريبية: قد تتغير الميزات، ولا نضمن التوفر المستمر، ونتوقع ونرحب بملاحظاتكم."
                  : "Beta notice: Features may change, uptime is not guaranteed, and feedback is expected."}
              </p>
            </div>
          </div>
        </footer>
      </div>
      <ToastContainer theme={theme} autoClose={9000} pauseOnHover closeOnClick={false} />
    </div>
  );
};

export default MainLayout;
