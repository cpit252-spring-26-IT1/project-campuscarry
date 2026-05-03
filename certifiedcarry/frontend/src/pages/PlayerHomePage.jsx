import { useCallback, useEffect, useMemo, useState } from "react";
import { Badge, Button, Chip } from "@heroui/react";
import { Link, useNavigate } from "react-router-dom";
import Card from "../components/Card";
import PlayerChatPreviewCard from "../components/PlayerChatPreviewCard";
import PreferenceOnboardingPrompt from "../components/PreferenceOnboardingPrompt";
import RecruiterAvailabilityCard from "../components/RecruiterAvailabilityCard";
import ThemedLoader from "../components/ThemedLoader";
import TopPlayersCard from "../components/TopPlayersCard";
import { getProfileMissingFields } from "../constants/gameConfig";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";
import { getConversationsForUser, getUnreadChatNotifications } from "../services/chatService";
import { trackUiEvent } from "../services/analyticsService";
import { getLeaderboardEntries, getPlayerProfileDetails } from "../services/playerService";
import { getRecruiterDirectory } from "../services/recruiterService";

const PLAYER_PROFILE_UPDATED_EVENT = "cc:player-profile-updated";

const getVerificationLabel = ({ status, isArabic }) => {
  if (status === "APPROVED") {
    return isArabic ? "موثق" : "Verified";
  }

  if (status === "PENDING") {
    return isArabic ? "قيد المراجعة" : "Pending";
  }

  if (status === "DECLINED") {
    return isArabic ? "مرفوض" : "Declined";
  }

  return isArabic ? "غير مكتمل" : "Incomplete";
};

const getVerificationToneClass = (status) => {
  if (status === "APPROVED") {
    return "bg-emerald-500/20 text-emerald-700 dark:text-emerald-300";
  }

  if (status === "PENDING") {
    return "bg-amber-500/20 text-amber-700 dark:text-amber-300";
  }

  if (status === "DECLINED") {
    return "bg-red-500/20 text-red-700 dark:text-red-300";
  }

  return "bg-[#3c4748]/25 text-[#1d1d1d] dark:text-[#cae9ea]";
};

const HOME_EDITORIAL_CARD_CLASS = "cc-home-editorial-card shadow-xl backdrop-blur-sm";

const PlayerHomePage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { isArabic } = useLanguage();

  const [isLoading, setIsLoading] = useState(true);
  const [hasLoadedHome, setHasLoadedHome] = useState(false);
  const [playerProfile, setPlayerProfile] = useState(null);
  const [conversations, setConversations] = useState([]);
  const [chatNotifications, setChatNotifications] = useState({ unreadCount: 0, items: [] });
  const [openRecruiters, setOpenRecruiters] = useState([]);
  const [topPlayers, setTopPlayers] = useState([]);

  const loadPlayerHome = useCallback(async ({ showLoader = !hasLoadedHome } = {}) => {
    if (showLoader) {
      setIsLoading(true);
    }

    try {
      const profile = await getPlayerProfileDetails(user.id);
      const profileStatus = String(profile?.rankVerificationStatus || "NOT_SUBMITTED").toUpperCase();
      const isVerifiedPlayer = profileStatus === "APPROVED";
      const selectedMode =
        profile?.game === "Rocket League" ? profile?.primaryRocketLeagueMode || "2v2" : "";

      const [nextConversations, notifications, recruiters, leaderboardEntries] = await Promise.all([
        getConversationsForUser(user.id, { forceRefresh: true }),
        getUnreadChatNotifications(user.id, { forceRefresh: true }),
        getRecruiterDirectory({
          onlyEligibleForPlayer: true,
          isCurrentPlayerVerified: isVerifiedPlayer,
        }),
        profile?.game
          ? getLeaderboardEntries({ game: profile.game, mode: selectedMode })
          : Promise.resolve([]),
      ]);

      setPlayerProfile(profile);
      setConversations(nextConversations.slice(0, 3));
      setChatNotifications(notifications);
      setOpenRecruiters(recruiters);
      setTopPlayers(leaderboardEntries.slice(0, 5));
      setHasLoadedHome(true);
    } catch (error) {
      console.error("Unable to load player home:", error);
      if (!hasLoadedHome) {
        setPlayerProfile(null);
        setConversations([]);
        setChatNotifications({ unreadCount: 0, items: [] });
        setOpenRecruiters([]);
        setTopPlayers([]);
      }
    } finally {
      if (showLoader) {
        setIsLoading(false);
      }
    }
  }, [hasLoadedHome, user.id]);

  useEffect(() => {
    void loadPlayerHome();
  }, [loadPlayerHome]);

  useEffect(() => {
    const handleWindowFocus = () => {
      void loadPlayerHome({ showLoader: false });
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void loadPlayerHome({ showLoader: false });
      }
    };

    const handleProfileUpdated = (event) => {
      const eventUserId = String(event?.detail?.userId || "");
      if (!eventUserId || eventUserId === String(user.id)) {
        void loadPlayerHome({ showLoader: false });
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
  }, [loadPlayerHome, user.id]);

  const verificationStatus = String(
    playerProfile?.rankVerificationStatus || "NOT_SUBMITTED",
  ).toUpperCase();
  const profileMissingFields = useMemo(() => getProfileMissingFields(playerProfile), [playerProfile]);
  const isProfileComplete = profileMissingFields.length === 0;

  const nextAction = useMemo(() => {
    if (!playerProfile || profileMissingFields.length > 0) {
      return {
        label: isArabic ? "أكمل ملفك" : "Complete Your Profile",
        to: "/profile-setup",
      };
    }

    if (verificationStatus === "DECLINED") {
      return {
        label: isArabic ? "عدّل وأعد الإرسال" : "Edit and Resubmit",
        to: "/profile-setup",
      };
    }

    if (verificationStatus === "PENDING") {
      return {
        label: isArabic ? "تابع حالة المراجعة" : "Track Verification",
        to: "/leaderboard",
      };
    }

    return {
      label: isArabic ? "استكشف المتصدرين" : "Explore Leaderboard",
      to: "/leaderboard",
    };
  }, [isArabic, playerProfile, profileMissingFields.length, verificationStatus]);

  const leaderboardTitle = playerProfile?.game
    ? isArabic
      ? `أفضل اللاعبين في ${playerProfile.game}`
      : `Top Players in ${playerProfile.game}`
    : isArabic
      ? "أفضل اللاعبين"
      : "Top Players";

  const profileStatusLabel = isProfileComplete
    ? isArabic
      ? "الملف مكتمل"
      : "Profile Complete"
    : isArabic
      ? `${profileMissingFields.length} عناصر ناقصة`
      : `${profileMissingFields.length} items missing`;

  const todayRhythmItems = useMemo(
    () => [
      {
        key: "profile",
        label: isArabic ? "صحة الملف" : "Profile Health",
        value: profileStatusLabel,
      },
      {
        key: "verification",
        label: isArabic ? "مسار التوثيق" : "Verification Lane",
        value: getVerificationLabel({ status: verificationStatus, isArabic }),
      },
      {
        key: "chat",
        label: isArabic ? "نوافذ التواصل" : "Conversation Windows",
        value:
          chatNotifications.unreadCount > 0
            ? isArabic
              ? `${chatNotifications.unreadCount} رسائل جديدة`
              : `${chatNotifications.unreadCount} new messages`
            : isArabic
              ? "لا توجد رسائل جديدة"
              : "No new messages",
      },
    ],
    [chatNotifications.unreadCount, isArabic, profileStatusLabel, verificationStatus],
  );

  if (isLoading) {
    return (
      <section className="bg-transparent px-4 py-10">
        <div className="m-auto max-w-6xl py-20 text-center">
          <ThemedLoader />
        </div>
      </section>
    );
  }

  return (
    <section className="bg-transparent px-4 py-10">
      <div className="cc-home-shell m-auto max-w-6xl space-y-4">
        <PreferenceOnboardingPrompt />

        <Card
          bg="bg-[#f4fbfb]/90 dark:bg-[#273b40]/60"
          className={`${HOME_EDITORIAL_CARD_CLASS} relative overflow-hidden`}
        >
          <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_right,rgba(32,140,140,0.26),transparent_58%)]" />
          <div className="relative flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[#208c8c]">
                {isArabic ? "مركزك اليومي" : "Daily Home Base"}
              </p>
              <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">
                {isArabic ? "الرئيسية" : "Player Home"}
              </h1>
              <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
                {isArabic
                  ? "نظرة سريعة على أهم ما يجب فعله اليوم للوصول إلى فرص أفضل."
                  : "A quick pulse on what matters most today to get discovered faster."}
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <Chip variant="flat" className={getVerificationToneClass(verificationStatus)}>
                {getVerificationLabel({ status: verificationStatus, isArabic })}
              </Chip>
              <Chip
                variant="flat"
                className="bg-[#208c8c]/18 text-[#1d1d1d] dark:text-[#cae9ea]"
              >
                {profileStatusLabel}
              </Chip>
            </div>

            <div className="flex w-full flex-wrap gap-3">
              <Button
                as={Link}
                to={nextAction.to}
                className="cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                onPress={() =>
                  trackUiEvent("player_home_next_action_click", {
                    target: nextAction.to,
                    verificationStatus,
                  })
                }
              >
                {nextAction.label}
              </Button>

              <Button
                as={Link}
                to={`/players/${user.id}`}
                variant="bordered"
                className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
              >
                {isArabic ? "عرض ملفي العام" : "View Public Profile"}
              </Button>
            </div>
          </div>
        </Card>

        <Card
          bg="bg-[#fff3e4]/92 dark:bg-[#2f2a23]/74"
          className={`${HOME_EDITORIAL_CARD_CLASS} border-[#c9773b]/45`}
        >
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#f6e6d8]">
                {isArabic ? "إيقاع اليوم" : "Today Rhythm"}
              </h2>
              <p className="cc-body-muted mt-1 text-[#4a3426] dark:text-[#f6e6d8]/82">
                {isArabic
                  ? "موجز سريع يوضح أين يجب أن تركز الآن قبل الانتقال لإدارة التفاصيل."
                  : "A fast pulse check on where to focus now before switching to deep management."}
              </p>
            </div>

            <Button
              as={Link}
              to="/dashboard"
              variant="bordered"
              className="cc-button-text border-[#8b5a32]/55 bg-[#fff8f0]/65 text-[#4a3426] hover:bg-[#fbe7d2] dark:border-[#c9773b]/55 dark:bg-[#3a332b]/72 dark:text-[#f6e6d8] dark:hover:bg-[#4a4136]"
            >
              {isArabic ? "لوحة التحكم التفصيلية" : "Open Dashboard"}
            </Button>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
            {todayRhythmItems.map((item) => (
              <div
                key={item.key}
                className="rounded-xl border border-[#8b5a32]/25 bg-[#fff8f1]/88 px-4 py-3 dark:border-[#c9773b]/30 dark:bg-[#332b22]/70"
              >
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[#8b5a32] dark:text-[#f6d8be]">
                  {item.label}
                </p>
                <p className="mt-2 text-sm font-semibold text-[#1d1d1d] dark:text-[#f6e6d8]">
                  {item.value}
                </p>
              </div>
            ))}
          </div>
        </Card>

        <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <Card
            bg="bg-[#fff4e8]/92 dark:bg-[#2f2a23]/74"
            className={`${HOME_EDITORIAL_CARD_CLASS} border-[#c9773b]/40`}
          >
            <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic ? "المحادثات غير المقروءة" : "Unread Chats"}
            </p>
            <div className="mt-3 flex items-center justify-between">
              <p className="text-3xl font-bold text-[#208c8c]">{chatNotifications.unreadCount}</p>
              <Badge
                color="danger"
                content={chatNotifications.unreadCount}
                isInvisible={chatNotifications.unreadCount === 0}
              >
                <Button
                  as={Link}
                  to="/chats"
                  size="sm"
                  variant="bordered"
                  className="cc-button-text border-[#8b5a32]/45 text-[#4a3426] hover:bg-[#fbe7d2] dark:border-[#c9773b]/55 dark:text-[#f6e6d8] dark:hover:bg-[#4a4136]"
                >
                  {isArabic ? "فتح المحادثات" : "Open Chats"}
                </Button>
              </Badge>
            </div>
          </Card>

          <Card
            bg="bg-[#ecfaf6]/92 dark:bg-[#243735]/72"
            className={`${HOME_EDITORIAL_CARD_CLASS} border-[#208c8c]/38`}
          >
            <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic ? "مستقطبون متاحون الآن" : "Recruiters Open Now"}
            </p>
            <p className="mt-3 text-3xl font-bold text-[#208c8c]">{openRecruiters.length}</p>
            <p className="mt-2 text-xs text-[#273b40]/80 dark:text-[#cae9ea]/75">
              {isArabic
                ? "مبنية على إعدادات فتح الرسائل المباشرة لكل مستقطب."
                : "Based on each recruiter direct-message openness setting."}
            </p>
          </Card>

          <Card
            bg="bg-[#f2f9ff]/92 dark:bg-[#253342]/72"
            className={`${HOME_EDITORIAL_CARD_CLASS} border-[#4f86a3]/35`}
          >
            <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic ? "حالة الملف" : "Profile Status"}
            </p>
            <p className="mt-3 text-xl font-bold text-[#208c8c]">{profileStatusLabel}</p>
            <Button
              as={Link}
              to="/profile-setup"
              size="sm"
              className="cc-button-text mt-4 bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
            >
              {isArabic ? "إدارة الملف" : "Manage Profile"}
            </Button>
          </Card>
        </div>

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <RecruiterAvailabilityCard isArabic={isArabic} recruiters={openRecruiters} />

          <TopPlayersCard isArabic={isArabic} title={leaderboardTitle} entries={topPlayers} />

          <PlayerChatPreviewCard
            isArabic={isArabic}
            conversations={conversations}
            onOpenThread={(threadId) => {
              trackUiEvent("player_home_chat_preview_open", { threadId });
              navigate(`/chats?thread=${encodeURIComponent(String(threadId))}`);
            }}
          />
        </div>
      </div>
    </section>
  );
};

export default PlayerHomePage;
