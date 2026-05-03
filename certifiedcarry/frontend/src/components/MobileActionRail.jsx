import { Button } from "@heroui/react";
import { Link, useLocation } from "react-router-dom";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";
import { getAuthenticatedDashboardPath, getAuthenticatedHomePath } from "./routeGuardUtils";

const MobileActionRail = () => {
  const { pathname } = useLocation();
  const { isAuthenticated, user } = useAuth();
  const { isArabic } = useLanguage();

  if (!isAuthenticated || !user || user.role === "ADMIN") {
    return null;
  }

  const isRecruiter = user.role === "RECRUITER";
  const homePath = getAuthenticatedHomePath(user);
  const dashboardPath = getAuthenticatedDashboardPath(user);
  const profilePath = user.role === "PLAYER" ? `/players/${user.id}` : "/settings";
  const isProfileActive =
    user.role === "PLAYER"
      ? pathname === profilePath || pathname.startsWith(`/players/${user.id}`)
      : pathname.startsWith("/settings");

  const actions = [
    {
      key: "home",
      to: homePath,
      label: isRecruiter ? (isArabic ? "الاستقطاب" : "Scouting") : isArabic ? "الرئيسية" : "Home",
      isActive: pathname === homePath,
    },
    {
      key: "dashboard",
      to: dashboardPath,
      label: isRecruiter ? (isArabic ? "العمليات" : "Ops") : isArabic ? "التحكم" : "Dashboard",
      isActive: pathname.startsWith("/dashboard") || (user.role === "ADMIN" && pathname === "/admin"),
    },
    {
      key: "chats",
      to: "/chats",
      label: isArabic ? "المحادثات" : "Chats",
      isActive: pathname.startsWith("/chats"),
    },
    {
      key: "profile",
      to: profilePath,
      label: user.role === "PLAYER" ? (isArabic ? "الملف" : "Profile") : isArabic ? "الإعدادات" : "Settings",
      isActive: isProfileActive,
    },
  ];

  return (
    <nav className="fixed bottom-3 left-1/2 z-40 w-[calc(100%-1rem)] max-w-md -translate-x-1/2 rounded-2xl border border-[#3c4748]/35 bg-[#f4fbfb]/90 p-2 shadow-[0_10px_28px_rgba(39,59,64,0.25)] backdrop-blur-sm dark:border-[#3c4748]/65 dark:bg-[#1d1d1d]/85 md:hidden">
      <ul className="grid grid-cols-4 gap-1">
        {actions.map((action) => (
          <li key={action.key}>
            <Button
              as={Link}
              to={action.to}
              variant="light"
              radius="sm"
              className={`cc-button-text h-10 w-full min-w-0 px-1 text-[11px] leading-tight ${
                action.isActive
                  ? "bg-[#208c8c]/30 text-[#1d1d1d] dark:text-[#cae9ea]"
                  : "text-[#273b40] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
              }`}
            >
              {action.label}
            </Button>
          </li>
        ))}
      </ul>
    </nav>
  );
};

export default MobileActionRail;
