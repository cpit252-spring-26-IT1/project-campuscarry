import { useEffect, useState } from "react";
import {
  Button,
  Navbar as HeroNavbar,
  NavbarBrand,
  NavbarContent,
  NavbarItem,
} from "@heroui/react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import darkModeIcon from "../assets/images/Darkmode-icon.svg";
import eToADarkMode from "../assets/images/E-to-A-darkmode.svg";
import eToALightMode from "../assets/images/E-to-A-lightmode.svg";
import lightModeIcon from "../assets/images/Lightmode-icon.svg";
import profileDarkMode from "../assets/images/Profile-darkmode.svg";
import profileLightMode from "../assets/images/Profile-lightmode.svg";
import logoDarkMode from "../assets/images/logo-darkmode.svg";
import logoLightMode from "../assets/images/logo-lightmode.svg";
import NavbarProfileMenu from "./navigation/NavbarProfileMenu";
import NavbarUtilityButtons from "./navigation/NavbarUtilityButtons";
import { getAuthenticatedDashboardPath, getAuthenticatedHomePath } from "./routeGuardUtils";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";
import useTheme from "../hooks/useTheme";
import { getPlayerProfileByUserId } from "../services/playerProfileService";

const NAVBAR_THEME = {
  dark: {
    islandClasses:
      "!bg-[#1d1d1d]/15 border border-[#3c4748]/45 shadow-[0_16px_42px_rgba(0,0,0,0.36)] backdrop-blur-sm backdrop-saturate-150 rounded-2xl",
    activeButtonClass: "bg-[#208c8c]/30 text-[#cae9ea] hover:bg-[#208c8c]/45",
    inactiveButtonClass: "text-[#cae9ea]/90 hover:bg-[#273b40]/70 hover:text-[#cae9ea]",
    logoutButtonClass: "bg-[#273b40] text-[#cae9ea] hover:bg-[#3c4748] shrink-0 whitespace-nowrap",
    themeToggleClass: "text-[#cae9ea] hover:bg-[#273b40]/70",
    profileButtonClass: "border border-[#3c4748]/55 bg-[#273b40]/55 hover:bg-[#3c4748]/75",
    brandTextClass: "text-[#cae9ea]",
  },
  light: {
    islandClasses:
      "!bg-[#f4fbfb]/50 border border-[#3c4748]/15 shadow-[0_16px_38px_rgba(39,59,64,0.22)] backdrop-blur-sm backdrop-saturate-125 rounded-2xl",
    activeButtonClass:
      "bg-[#cae9ea]/75 text-[#1d3034] shadow-[0_2px_8px_rgba(39,59,64,0.14)] hover:bg-[#bfe5e6]",
    inactiveButtonClass:
      "text-[#2b464a] hover:border hover:border-[#3c4748]/42 hover:bg-[#e4f2f3] hover:text-[#1f3438]",
    logoutButtonClass:
      "border border-[#208c8c]/46 bg-[#cae9ea]/78 text-[#1f3438] shadow-[0_2px_8px_rgba(32,140,140,0.16)] hover:bg-[#bfe5e6] shrink-0 whitespace-nowrap",
    themeToggleClass: "text-[#2b464a] hover:border hover:border-[#3c4748]/42 hover:bg-[#e4f2f3]",
    profileButtonClass: "border border-[#3c4748]/30 bg-[#cae9ea]/70 hover:bg-[#bfe5e6]",
    brandTextClass: "text-[#273b40]",
  },
};

const AUTHENTICATED_PREFETCH_DELAY_MS = 700;

const Navbar = () => {
  const [resolvedProfileImage, setResolvedProfileImage] = useState("");

  const navigate = useNavigate();
  const { pathname } = useLocation();
  const { isAuthenticated, user, logoutUser } = useAuth();
  const { isDarkMode, toggleTheme } = useTheme();
  const { isArabic, language, toggleLanguage, t } = useLanguage();
  const navTheme = isDarkMode ? NAVBAR_THEME.dark : NAVBAR_THEME.light;

  useEffect(() => {
    let isActive = true;

    const loadProfileImage = async () => {
      if (!isAuthenticated || !user) {
        if (isActive) {
          setResolvedProfileImage("");
        }
        return;
      }

      if (user.role !== "PLAYER") {
        if (isActive) {
          setResolvedProfileImage("");
        }
        return;
      }

      if (user.profileImage) {
        if (isActive) {
          setResolvedProfileImage(user.profileImage);
        }
        return;
      }

      try {
        const playerProfile = await getPlayerProfileByUserId(user.id, { bustCache: true });
        if (!isActive) {
          return;
        }

        setResolvedProfileImage(playerProfile?.profileImage || "");
      } catch {
        if (isActive) {
          setResolvedProfileImage("");
        }
      }
    };

    loadProfileImage();

    return () => {
      isActive = false;
    };
  }, [isAuthenticated, user]);

  useEffect(() => {
    if (!isAuthenticated) {
      return undefined;
    }

    const timeoutId = globalThis.setTimeout(() => {
      void import("../pages/ChatsPage.jsx");
      void import("../pages/DashboardPage.jsx");
      void import("../pages/LeaderboardPage.jsx");
    }, AUTHENTICATED_PREFETCH_DELAY_MS);

    return () => {
      globalThis.clearTimeout(timeoutId);
    };
  }, [isAuthenticated, user?.role]);

  const iconToggleButtonClass = `${navTheme.themeToggleClass} h-10 w-10 min-w-10 shrink-0 p-0 md:h-10 md:w-10 md:min-w-10`;
  const hasProfileImage = Boolean(resolvedProfileImage);
  const profileMenuTriggerClass = `h-10 min-w-[3.5rem] shrink-0 gap-1 px-1 ${navTheme.profileButtonClass}`;
  const profileImageSrc = hasProfileImage
    ? resolvedProfileImage
    : isDarkMode
      ? profileDarkMode
      : profileLightMode;
  const profileMenuAriaLabel = isArabic ? "قائمة الحساب" : "Account menu";
  const profileMenuProfileLabel =
    user?.role === "PLAYER"
      ? isArabic
        ? "ملف اللاعب"
        : "Player Profile"
      : isArabic
        ? "الملف الشخصي"
        : "My Profile";
  const profileMenuSettingsLabel = isArabic ? "الإعدادات" : "Settings";
  const profileMenuLogoutLabel = t("nav.logout");
  const profileMenuLanguageLabel = isArabic ? "اللغة" : "Language";
  const profileMenuThemeLabel = isArabic ? "المظهر" : "Theme";
  const switchLanguageLabel =
    language === "ar" ? t("nav.switchToEnglish") : t("nav.switchToArabic");
  const switchThemeLabel = isDarkMode ? t("nav.switchToLight") : t("nav.switchToDark");
  const isAdmin = user?.role === "ADMIN";
  const isPlayer = user?.role === "PLAYER";
  const isRecruiter = user?.role === "RECRUITER";
  const homePath = getAuthenticatedHomePath(user);
  const dashboardPath = getAuthenticatedDashboardPath(user);
  const homeLabel = isRecruiter
    ? isArabic
      ? "رئيسية الاستقطاب"
      : "Scouting Home"
    : isPlayer
      ? isArabic
        ? "رئيسية اللاعب"
        : "Player Home"
      : isArabic
        ? "الرئيسية"
        : "Home";
  const landingLabel = t("nav.landing");
  const dashboardLabel = isAdmin
    ? isArabic
      ? "لوحة الإدارة"
      : "Admin"
    : isRecruiter
      ? isArabic
        ? "لوحة العمليات"
        : "Ops Dashboard"
      : isArabic
        ? "لوحة التحكم"
        : "Dashboard";

  const getLinkClass = (isActive) =>
    `${isActive ? navTheme.activeButtonClass : navTheme.inactiveButtonClass} shrink-0 whitespace-nowrap`;
  const profilePath = user?.role === "PLAYER" ? `/players/${user.id}` : dashboardPath;

  const handleLogout = () => {
    logoutUser();
    navigate("/");
  };

  const desktopGuestLinks = [
    { to: "/", label: landingLabel },
    { to: "/login", label: t("nav.login") },
    { to: "/register", label: t("nav.register") },
  ];

  const mobileGuestLinks = desktopGuestLinks;

  const mobileAuthenticatedLinks = [
    { to: homePath, label: homeLabel },
    { to: dashboardPath, label: dashboardLabel },
  ];

  return (
    <div
      dir="ltr"
      className="fixed inset-x-0 top-3 z-50 mx-auto w-[calc(100%-1.25rem)] max-w-6xl px-1 transition-all duration-300 hover:scale-[1.005]"
    >
      <HeroNavbar
        isBlurred={false}
        position="sticky"
        maxWidth="xl"
        classNames={{
          base: `${navTheme.islandClasses} !border-0 md:!border h-auto`,
          wrapper:
            "!bg-transparent flex flex-wrap items-center gap-y-2 py-2 md:min-h-[8.5rem] md:gap-y-0 md:py-0 xl:min-h-[4.5rem]",
        }}
        height="auto"
      >
        <NavbarBrand className="ml-0">
          <Link className="flex items-center gap-1" to="/">
            <img
              className="h-10 w-auto shrink-0 md:h-8 xl:h-10"
              src={isDarkMode ? logoDarkMode : logoLightMode}
              alt="CertifiedCarry"
            />
            <span
              className={`block whitespace-nowrap font-display text-base font-bold tracking-tight md:text-xl xl:hidden ${
                navTheme.brandTextClass
              }`}
            >
              CertifiedCarry™
            </span>
            <span
              className={`hidden whitespace-nowrap font-display text-xl font-bold tracking-tight xl:block ${
                navTheme.brandTextClass
              }`}
            >
              CertifiedCarry™
            </span>
          </Link>
        </NavbarBrand>

        <NavbarContent justify="end" className="ml-auto gap-2 xl:hidden">
          {isAuthenticated ? (
            <NavbarItem>
              <NavbarProfileMenu
                handleLogout={handleLogout}
                isArabic={isArabic}
                isDarkMode={isDarkMode}
                language={language}
                navigate={navigate}
                profileImageSrc={profileImageSrc}
                profileMenuAriaLabel={profileMenuAriaLabel}
                profileMenuLogoutLabel={profileMenuLogoutLabel}
                profileMenuProfileLabel={profileMenuProfileLabel}
                profileMenuSettingsLabel={profileMenuSettingsLabel}
                profileMenuThemeLabel={profileMenuThemeLabel}
                profileMenuLanguageLabel={profileMenuLanguageLabel}
                profileMenuTriggerClass={profileMenuTriggerClass}
                profilePath={profilePath}
                switchLanguageLabel={switchLanguageLabel}
                switchThemeLabel={switchThemeLabel}
                toggleLanguage={toggleLanguage}
                toggleTheme={toggleTheme}
              />
            </NavbarItem>
          ) : (
            <NavbarUtilityButtons
              darkModeIcon={darkModeIcon}
              eToADarkMode={eToADarkMode}
              eToALightMode={eToALightMode}
              iconToggleButtonClass={iconToggleButtonClass}
              isDarkMode={isDarkMode}
              lightModeIcon={lightModeIcon}
              switchLanguageLabel={switchLanguageLabel}
              switchThemeLabel={switchThemeLabel}
              toggleLanguage={toggleLanguage}
              toggleTheme={toggleTheme}
            />
          )}
        </NavbarContent>

        <NavbarContent justify="end" className="hidden gap-2 xl:flex">
          {isAuthenticated ? (
            <>
              {!isAdmin ? (
                <NavbarItem>
                  <Button
                    as={Link}
                    to={homePath}
                    variant="light"
                    radius="sm"
                    className={getLinkClass(pathname === homePath)}
                  >
                    {homeLabel}
                  </Button>
                </NavbarItem>
              ) : null}

              <NavbarItem>
                <Button
                  as={Link}
                  to={dashboardPath}
                  variant="light"
                  radius="sm"
                  className={getLinkClass(pathname === dashboardPath)}
                >
                  {dashboardLabel}
                </Button>
              </NavbarItem>

              <NavbarItem>
                <NavbarProfileMenu
                  handleLogout={handleLogout}
                  isArabic={isArabic}
                  isDarkMode={isDarkMode}
                  language={language}
                  navigate={navigate}
                  profileImageSrc={profileImageSrc}
                  profileMenuAriaLabel={profileMenuAriaLabel}
                  profileMenuLogoutLabel={profileMenuLogoutLabel}
                  profileMenuProfileLabel={profileMenuProfileLabel}
                  profileMenuSettingsLabel={profileMenuSettingsLabel}
                  profileMenuThemeLabel={profileMenuThemeLabel}
                  profileMenuLanguageLabel={profileMenuLanguageLabel}
                  profileMenuTriggerClass={profileMenuTriggerClass}
                  profilePath={profilePath}
                  switchLanguageLabel={switchLanguageLabel}
                  switchThemeLabel={switchThemeLabel}
                  toggleLanguage={toggleLanguage}
                  toggleTheme={toggleTheme}
                />
              </NavbarItem>
            </>
          ) : (
            <>
              {desktopGuestLinks.map((link) => (
                <NavbarItem key={link.to}>
                  <Button
                    as={Link}
                    to={link.to}
                    variant="light"
                    radius="sm"
                    className={getLinkClass(pathname === link.to)}
                  >
                    {link.label}
                  </Button>
                </NavbarItem>
              ))}
            </>
          )}

          {!isAuthenticated ? (
            <NavbarUtilityButtons
              darkModeIcon={darkModeIcon}
              eToADarkMode={eToADarkMode}
              eToALightMode={eToALightMode}
              iconToggleButtonClass={iconToggleButtonClass}
              isDarkMode={isDarkMode}
              lightModeIcon={lightModeIcon}
              switchLanguageLabel={switchLanguageLabel}
              switchThemeLabel={switchThemeLabel}
              toggleLanguage={toggleLanguage}
              toggleTheme={toggleTheme}
            />
          ) : null}
        </NavbarContent>

        <div className="order-3 w-full flex-none px-2 pb-1 pt-1 xl:hidden">
          {isAuthenticated ? (
            <div className="grid grid-cols-2 gap-2">
              {mobileAuthenticatedLinks.map((link) => (
                <Button
                  key={link.to}
                  as={Link}
                  to={link.to}
                  variant="light"
                  radius="sm"
                  className={`${getLinkClass(pathname === link.to)} h-11 w-full min-w-0 !whitespace-normal px-2 text-xs leading-tight`}
                >
                  {link.label}
                </Button>
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-3 gap-2">
              {mobileGuestLinks.map((link) => (
                <Button
                  key={link.to}
                  as={Link}
                  to={link.to}
                  variant="light"
                  radius="sm"
                  className={`${getLinkClass(pathname === link.to)} h-11 w-full min-w-0 !whitespace-normal px-2 text-xs leading-tight`}
                >
                  {link.label}
                </Button>
              ))}
            </div>
          )}
        </div>
      </HeroNavbar>
    </div>
  );
};
export default Navbar;
