import { useCallback, useEffect, useMemo, useState } from "react";
import { Navigate, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import { getAuthenticatedHomePath } from "../components/routeGuardUtils";
import DashboardHero from "../components/dashboard/DashboardHero";
import PlayerDashboardContent from "../components/dashboard/PlayerDashboardContent";
import RecruiterDashboardContent from "../components/dashboard/RecruiterDashboardContent";
import {
  GAME_OPTIONS,
  getProfileMissingFields,
  isRoleDrivenGame,
  requiresRatingValue,
} from "../constants/gameConfig";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";
import { logBackgroundAuthIssue } from "../services/auth/shared";
import {
  buildUnreadChatNotificationsFromConversations,
  getConversationsForUser,
  getUnreadChatNotifications,
} from "../services/chatService";
import {
  getBrowsePlayers,
  getPlayerProfileDetails,
  getPlayerRankPosition,
  updatePlayerChatPreference,
} from "../services/playerService";
import { getRecruiterDmOpenness, getRecruiterDmOpennessLabel } from "../services/recruiterService";

const PLAYER_PROFILE_UPDATED_EVENT = "cc:player-profile-updated";

const DashboardPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { isArabic } = useLanguage();

  const isPlayer = user.role === "PLAYER";
  const isRecruiter = user.role === "RECRUITER";
  const homePath = getAuthenticatedHomePath(user);

  const [isLoading, setIsLoading] = useState(true);
  const [hasLoadedPlayerDashboard, setHasLoadedPlayerDashboard] = useState(false);
  const [playerProfile, setPlayerProfile] = useState(null);
  const [playerRankPosition, setPlayerRankPosition] = useState(null);
  const [chatNotifications, setChatNotifications] = useState({ unreadCount: 0, items: [] });
  const [isUpdatingChatPreference, setIsUpdatingChatPreference] = useState(false);
  const [selectedGame, setSelectedGame] = useState(GAME_OPTIONS[0]);
  const [isRecruiterInsightsLoading, setIsRecruiterInsightsLoading] = useState(false);
  const [recruiterStats, setRecruiterStats] = useState({
    dmOpenness: "CLOSED",
    unreadCount: 0,
    threadCount: 0,
    candidateCount: 0,
  });

  const loadPlayerDashboard = useCallback(async () => {
    if (!isPlayer) {
      setIsLoading(false);
      return;
    }

    const showLoader = !hasLoadedPlayerDashboard;
    if (showLoader) {
      setIsLoading(true);
    }

    try {
      const profile = await getPlayerProfileDetails(user.id);
      setPlayerProfile(profile);

      const selectedMode =
        profile?.game === "Rocket League"
          ? profile.primaryRocketLeagueMode || profile.rocketLeagueModes?.[0]?.mode || "2v2"
          : "";

      const [notificationsResult, rankPositionResult] = await Promise.allSettled([
        getUnreadChatNotifications(user.id, { forceRefresh: true }),
        profile?.game
          ? getPlayerRankPosition({
              userId: user.id,
              game: profile.game,
              mode: selectedMode,
            })
          : Promise.resolve(null),
      ]);

      if (notificationsResult.status === "fulfilled") {
        setChatNotifications(notificationsResult.value);
      } else {
        logBackgroundAuthIssue(
          "Failed to load player dashboard chat notifications:",
          notificationsResult.reason,
          "error",
        );
        setChatNotifications((current) =>
          hasLoadedPlayerDashboard ? current : { unreadCount: 0, items: [] },
        );
      }

      if (rankPositionResult.status === "fulfilled") {
        setPlayerRankPosition(rankPositionResult.value);
      } else {
        logBackgroundAuthIssue(
          "Failed to load player dashboard rank position:",
          rankPositionResult.reason,
          "error",
        );
        setPlayerRankPosition((current) => (hasLoadedPlayerDashboard ? current : null));
      }

      setHasLoadedPlayerDashboard(true);
    } catch (error) {
      logBackgroundAuthIssue("Failed to load player dashboard:", error, "error");
      if (!hasLoadedPlayerDashboard) {
        setPlayerProfile(null);
        setPlayerRankPosition(null);
        setChatNotifications({ unreadCount: 0, items: [] });
      }
    } finally {
      if (showLoader) {
        setIsLoading(false);
      }
    }
  }, [hasLoadedPlayerDashboard, isPlayer, user.id]);

  useEffect(() => {
    void loadPlayerDashboard();
  }, [loadPlayerDashboard]);

  useEffect(() => {
    const handleWindowFocus = () => {
      void loadPlayerDashboard();
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void loadPlayerDashboard();
      }
    };

    const handleProfileUpdated = (event) => {
      const eventUserId = String(event?.detail?.userId || "");
      if (!eventUserId || eventUserId === String(user.id)) {
        void loadPlayerDashboard();
      }
    };

    globalThis.addEventListener("focus", handleWindowFocus);
    document.addEventListener("visibilitychange", handleVisibilityChange);
    globalThis.addEventListener(PLAYER_PROFILE_UPDATED_EVENT, handleProfileUpdated);

    return () => {
      globalThis.removeEventListener("focus", handleWindowFocus);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
      globalThis.removeEventListener(PLAYER_PROFILE_UPDATED_EVENT, handleProfileUpdated);
    };
  }, [loadPlayerDashboard, user.id]);

  const loadRecruiterDashboard = useCallback(async () => {
    if (!isRecruiter) {
      return;
    }

    setIsRecruiterInsightsLoading(true);

    try {
      const [dmOpenness, conversations, candidates] = await Promise.all([
        getRecruiterDmOpenness({ userId: user.id }),
        getConversationsForUser(user.id, { forceRefresh: true }),
        getBrowsePlayers({ game: selectedGame }),
      ]);
      const notifications = await buildUnreadChatNotificationsFromConversations(conversations, {
        forceRefresh: true,
      });

      setRecruiterStats({
        dmOpenness,
        unreadCount: notifications.unreadCount,
        threadCount: conversations.length,
        candidateCount: candidates.length,
      });
    } catch (error) {
      logBackgroundAuthIssue("Failed to load recruiter dashboard:", error, "error");
      setRecruiterStats({
        dmOpenness: "CLOSED",
        unreadCount: 0,
        threadCount: 0,
        candidateCount: 0,
      });
    } finally {
      setIsRecruiterInsightsLoading(false);
    }
  }, [isRecruiter, selectedGame, user.id]);

  useEffect(() => {
    if (!isRecruiter) {
      return;
    }

    void loadRecruiterDashboard();
  }, [isRecruiter, loadRecruiterDashboard]);

  useEffect(() => {
    if (!isRecruiter) {
      return undefined;
    }

    const handleWindowFocus = () => {
      void loadRecruiterDashboard();
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void loadRecruiterDashboard();
      }
    };

    globalThis.addEventListener("focus", handleWindowFocus);
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      globalThis.removeEventListener("focus", handleWindowFocus);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [isRecruiter, loadRecruiterDashboard]);

  const profileMissingFields = useMemo(() => getProfileMissingFields(playerProfile), [playerProfile]);

  const playerStatus = useMemo(() => {
    if (!playerProfile) {
      return "INCOMPLETE";
    }

    if (profileMissingFields.length > 0) {
      return "INCOMPLETE";
    }

    if (playerProfile.rankVerificationStatus === "PENDING") {
      return "PENDING";
    }

    if (playerProfile.rankVerificationStatus === "APPROVED") {
      return "VERIFIED";
    }

    if (playerProfile.rankVerificationStatus === "DECLINED") {
      return "DECLINED";
    }

    return "INCOMPLETE";
  }, [playerProfile, profileMissingFields.length]);

  const showRatingInPlayerPreview = playerProfile?.game !== "Overwatch 2";
  const dashboardDescription = isRecruiter
    ? isArabic
      ? "غرفة عمليات لإدارة تدفق الاستقطاب والمحادثات وسياسة الرسائل المباشرة."
      : "An operations room to manage scouting flow, conversations, and direct-message policy."
    : isArabic
      ? "مساحة الإدارة والمتابعة التفصيلية للملف والتحقق وخصوصية المحادثات."
      : "Your detailed control center for profile quality, verification, and chat privacy.";

  const completionProgress = useMemo(() => {
    if (!playerProfile) {
      return { value: 0, missing: [] };
    }

    const missing = profileMissingFields;
    const requiredCount =
      3 +
      (isRoleDrivenGame(playerProfile.game) ? 1 : 0) +
      (requiresRatingValue(playerProfile.game, playerProfile.rank) ? 1 : 0) +
      (playerProfile.game === "Rocket League" ? 1 : 0);

    const completion = Math.max(
      0,
      Math.round(((requiredCount - missing.length) / requiredCount) * 100),
    );

    return { value: completion, missing };
  }, [playerProfile, profileMissingFields]);

  const missingFieldLabels = {
    game: isArabic ? "اختيار اللعبة" : "Select game",
    rank: isArabic ? "اختيار الرتبة" : "Select rank",
    ratingValue: isArabic ? "إدخال MMR/Skill Rating" : "Provide MMR/Skill Rating",
    inGameRole: isArabic ? "اختيار الدور داخل اللعبة" : "Select in-game role",
    proofImage: isArabic ? "رفع صورة إثبات" : "Upload rank proof screenshot",
    rocketLeagueModes: isArabic ? "اختيار أنماط لعب Rocket League" : "Choose Rocket League modes",
  };

  const dashboardMetricCards = useMemo(
    () => [
      {
        key: "completion",
        label: isArabic ? "اكتمال الملف" : "Profile Completion",
        value: `${completionProgress.value}%`,
        helper:
          completionProgress.missing.length > 0
            ? isArabic
              ? `${completionProgress.missing.length} عناصر مطلوبة`
              : `${completionProgress.missing.length} required items`
            : isArabic
              ? "جاهز بالكامل"
              : "Fully ready",
        to: "/profile-setup",
        actionLabel: isArabic ? "إدارة الملف" : "Manage Profile",
      },
      {
        key: "rank",
        label: isArabic ? "الترتيب الحالي" : "Current Ranking",
        value: playerRankPosition ? `#${playerRankPosition}` : "-",
        helper: playerProfile?.game
          ? isArabic
            ? `في ${playerProfile.game}`
            : `In ${playerProfile.game}`
          : isArabic
            ? "اختر لعبتك أولا"
            : "Choose your game first",
        to: "/leaderboard",
        actionLabel: isArabic ? "فتح المتصدرين" : "Open Leaderboard",
      },
      {
        key: "chat",
        label: isArabic ? "رسائل غير مقروءة" : "Unread Messages",
        value: String(chatNotifications.unreadCount),
        helper:
          chatNotifications.unreadCount > 0
            ? isArabic
              ? "تحتاج متابعة"
              : "Needs follow-up"
            : isArabic
              ? "كل شيء محدث"
              : "All clear",
        to: "/chats",
        actionLabel: isArabic ? "إدارة المحادثات" : "Manage Chats",
      },
    ],
    [
      chatNotifications.unreadCount,
      completionProgress.missing.length,
      completionProgress.value,
      isArabic,
      playerProfile?.game,
      playerRankPosition,
    ],
  );

  const recruiterLeaderboardLink = `/leaderboard?game=${encodeURIComponent(selectedGame)}`;
  const recruiterDmStateLabel = getRecruiterDmOpennessLabel({
    recruiterDmOpenness: recruiterStats.dmOpenness,
    isArabic,
  });

  const recruiterMetricCards = useMemo(
    () => [
      {
        key: "candidate-count",
        label: isArabic ? "حجم المواهب" : "Candidate Pool",
        value: String(recruiterStats.candidateCount),
        helper: isArabic ? `في ${selectedGame}` : `In ${selectedGame}`,
        to: "/players",
        actionLabel: isArabic ? "فتح التصفح" : "Open Browse",
      },
      {
        key: "unread-messages",
        label: isArabic ? "رسائل غير مقروءة" : "Unread Messages",
        value: String(recruiterStats.unreadCount),
        helper:
          recruiterStats.unreadCount > 0
            ? isArabic
              ? "تحتاج متابعة"
              : "Needs follow-up"
            : isArabic
              ? "المحادثات محدثة"
              : "Conversations are clear",
        to: "/chats",
        actionLabel: isArabic ? "إدارة المحادثات" : "Manage Chats",
      },
      {
        key: "dm-state",
        label: isArabic ? "سياسة الرسائل" : "DM Openness",
        value: recruiterDmStateLabel,
        helper: isArabic
          ? `${recruiterStats.threadCount} محادثات نشطة`
          : `${recruiterStats.threadCount} active threads`,
        to: "/settings",
        actionLabel: isArabic ? "تعديل السياسة" : "Adjust Policy",
      },
    ],
    [
      isArabic,
      recruiterDmStateLabel,
      recruiterStats.candidateCount,
      recruiterStats.threadCount,
      recruiterStats.unreadCount,
      selectedGame,
    ],
  );

  const handleUpdatePlayerChatPreference = async (nextPreference) => {
    if (!isPlayer || !playerProfile) {
      return;
    }

    const currentPreference = playerProfile.allowPlayerChats !== false;
    if (nextPreference === currentPreference) {
      return;
    }

    setIsUpdatingChatPreference(true);

    try {
      const updatedPreference = await updatePlayerChatPreference({
        userId: user.id,
        allowPlayerChats: nextPreference,
      });

      setPlayerProfile((current) =>
        current
          ? {
              ...current,
              allowPlayerChats: updatedPreference,
            }
          : current,
      );

      toast.success(
        updatedPreference
          ? isArabic
            ? "تم تفعيل استقبال رسائل اللاعبين."
            : "You now accept chats from players."
          : isArabic
            ? "تم تقييد المحادثات لتكون من الكشّافين فقط."
            : "Only scouts can chat with you now.",
      );
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : isArabic
            ? "تعذر تحديث إعدادات المحادثة."
            : "Unable to update chat preference.";
      toast.error(message);
    } finally {
      setIsUpdatingChatPreference(false);
    }
  };

  if (user.role === "ADMIN") {
    return <Navigate to="/admin" replace />;
  }

  return (
    <section className="bg-transparent px-4 py-10">
      <div className="cc-dashboard-shell m-auto max-w-6xl">
        <DashboardHero
          dashboardDescription={dashboardDescription}
          homePath={homePath}
          isArabic={isArabic}
          isPlayer={isPlayer}
          user={user}
        />

        {isPlayer ? (
          <PlayerDashboardContent
            chatNotifications={chatNotifications}
            completionProgress={completionProgress}
            dashboardMetricCards={dashboardMetricCards}
            handleUpdatePlayerChatPreference={handleUpdatePlayerChatPreference}
            isArabic={isArabic}
            isLoading={isLoading}
            isUpdatingChatPreference={isUpdatingChatPreference}
            missingFieldLabels={missingFieldLabels}
            navigate={navigate}
            playerProfile={playerProfile}
            playerRankPosition={playerRankPosition}
            playerStatus={playerStatus}
            showRatingInPlayerPreview={showRatingInPlayerPreview}
            user={user}
          />
        ) : null}

        {isRecruiter ? (
          <RecruiterDashboardContent
            gameOptions={GAME_OPTIONS}
            isArabic={isArabic}
            isRecruiterInsightsLoading={isRecruiterInsightsLoading}
            recruiterDmStateLabel={recruiterDmStateLabel}
            recruiterLeaderboardLink={recruiterLeaderboardLink}
            recruiterMetricCards={recruiterMetricCards}
            recruiterStats={recruiterStats}
            selectedGame={selectedGame}
            setSelectedGame={setSelectedGame}
          />
        ) : null}
      </div>
    </section>
  );
};

export default DashboardPage;
