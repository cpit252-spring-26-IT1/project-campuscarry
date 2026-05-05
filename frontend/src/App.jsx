import { Suspense, createElement, lazy, useEffect, useState } from "react";
import {
  Route,
  RouterProvider,
  createBrowserRouter,
  createRoutesFromElements,
} from "react-router-dom";
import MainLayout from "./layouts/MainLayout";
import ProtectedRoute from "./components/ProtectedRoute";
import PublicOnlyRoute from "./components/PublicOnlyRoute";
import ThemedLoader from "./components/ThemedLoader";

const LandingPage = lazy(() => import("./pages/LandingPage"));
const NotFound = lazy(() => import("./pages/NotFound"));
const LoginPage = lazy(() => import("./pages/LoginPage"));
const RegisterPage = lazy(() => import("./pages/RegisterPage"));
const VerifyEmailSignupPage = lazy(() => import("./pages/VerifyEmailSignupPage"));
const DashboardPage = lazy(() => import("./pages/DashboardPage"));
const PlayerHomePage = lazy(() => import("./pages/PlayerHomePage"));
const RecruiterHomePage = lazy(() => import("./pages/RecruiterHomePage"));
const AdminPanelPage = lazy(() => import("./pages/AdminPanelPage"));
const AdminUsersPage = lazy(() => import("./pages/AdminUsersPage"));
const BrowsePlayersPage = lazy(() => import("./pages/BrowsePlayersPage"));
const LeaderboardPage = lazy(() => import("./pages/LeaderboardPage"));
const PendingApprovalPage = lazy(() => import("./pages/PendingApprovalPage"));
const PlayerProfilePage = lazy(() => import("./pages/PlayerProfilePage"));
const ProfileSetupPage = lazy(() => import("./pages/ProfileSetupPage"));
const ChatsPage = lazy(() => import("./pages/ChatsPage"));
const SettingsPage = lazy(() => import("./pages/SettingsPage"));
const TermsPage = lazy(() => import("./pages/TermsPage"));
const PrivacyPage = lazy(() => import("./pages/PrivacyPage"));
const CookiePolicyPage = lazy(() => import("./pages/CookiePolicyPage"));
const CommunityGuidelinesPage = lazy(() => import("./pages/CommunityGuidelinesPage"));
const AboutPage = lazy(() => import("./pages/AboutPage"));
const ContactPage = lazy(() => import("./pages/ContactPage"));

const RouteLoadingFallback = () => (
  <div className="flex min-h-[55vh] items-center justify-center">
    <ThemedLoader />
  </div>
);

const ROUTE_FALLBACK_DELAY_MS = 220;

const DelayedRouteLoadingFallback = () => {
  const [showLoader, setShowLoader] = useState(false);

  useEffect(() => {
    const timeoutId = globalThis.setTimeout(() => {
      setShowLoader(true);
    }, ROUTE_FALLBACK_DELAY_MS);

    return () => {
      globalThis.clearTimeout(timeoutId);
    };
  }, []);

  if (!showLoader) {
    return <div className="min-h-[55vh]" />;
  }

  return <RouteLoadingFallback />;
};

const withRouteSuspense = (element) => (
  <Suspense fallback={<DelayedRouteLoadingFallback />}>{element}</Suspense>
);

const withPublicRoute = (element) => (
  <PublicOnlyRoute>{withRouteSuspense(element)}</PublicOnlyRoute>
);

const withProtectedRoute = (element, allowedRoles) => (
  <ProtectedRoute allowedRoles={allowedRoles}>{withRouteSuspense(element)}</ProtectedRoute>
);

const PUBLIC_ONLY_ROUTES = [
  { path: "/login", component: LoginPage },
  { path: "/register", component: RegisterPage },
  { path: "/verify-email-signup", component: VerifyEmailSignupPage },
];

const PUBLIC_ROUTES = [
  { path: "/terms", component: TermsPage },
  { path: "/privacy", component: PrivacyPage },
  { path: "/cookies", component: CookiePolicyPage },
  { path: "/community-guidelines", component: CommunityGuidelinesPage },
  { path: "/about", component: AboutPage },
  { path: "/contact", component: ContactPage },
];

const PROTECTED_ROUTES = [
  { path: "/profile-setup", component: ProfileSetupPage, allowedRoles: ["PLAYER"] },
  {
    path: "/chats",
    component: ChatsPage,
    allowedRoles: ["PLAYER", "RECRUITER", "ADMIN"],
  },
  {
    path: "/leaderboard",
    component: LeaderboardPage,
    allowedRoles: ["PLAYER", "RECRUITER", "ADMIN"],
  },
  {
    path: "/settings",
    component: SettingsPage,
    allowedRoles: ["PLAYER", "RECRUITER", "ADMIN"],
  },
  { path: "/players", component: BrowsePlayersPage, allowedRoles: ["RECRUITER", "ADMIN"] },
  {
    path: "/players/:playerId",
    component: PlayerProfilePage,
    allowedRoles: ["PLAYER", "RECRUITER", "ADMIN"],
  },
  { path: "/admin", component: AdminPanelPage, allowedRoles: ["ADMIN"] },
  { path: "/admin/users", component: AdminUsersPage, allowedRoles: ["ADMIN"] },
];

const App = () => {
  const router = createBrowserRouter(
    createRoutesFromElements(
      <Route path="/" element={<MainLayout />}>
        <Route index element={withPublicRoute(<LandingPage />)} />
        {PUBLIC_ONLY_ROUTES.map(({ path, component }) => (
          <Route key={path} path={path} element={withPublicRoute(createElement(component))} />
        ))}
        {PUBLIC_ROUTES.map(({ path, component }) => (
          <Route key={path} path={path} element={withRouteSuspense(createElement(component))} />
        ))}
        <Route path="/player/home" element={withProtectedRoute(<PlayerHomePage />, ["PLAYER"])} />
        <Route
          path="/recruiter/home"
          element={withProtectedRoute(<RecruiterHomePage />, ["RECRUITER"])}
        />
        <Route
          path="/dashboard"
          element={withProtectedRoute(<DashboardPage />, ["PLAYER", "RECRUITER"])}
        />
        <Route path="/pending-approval" element={withPublicRoute(<PendingApprovalPage />)} />
        {PROTECTED_ROUTES.map((route) => {
          const Component = route.component;
          return (
            <Route
              key={route.path}
              path={route.path}
              element={withProtectedRoute(<Component />, route.allowedRoles)}
            />
          );
        })}
        <Route path="*" element={withRouteSuspense(<NotFound />)} />
      </Route>,
    ),
  );

  return <RouterProvider router={router} />;
};

export default App;
