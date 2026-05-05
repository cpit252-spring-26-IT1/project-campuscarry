import { useEffect, useMemo, useState } from "react";
import { Badge, Button, Chip, Select, SelectItem } from "@heroui/react";
import { Link, useNavigate } from "react-router-dom";
import Card from "../components/Card";
import PreferenceOnboardingPrompt from "../components/PreferenceOnboardingPrompt";
import RecruiterChatPreviewCard from "../components/RecruiterChatPreviewCard";
import ThemedLoader from "../components/ThemedLoader";
import TopPlayersCard from "../components/TopPlayersCard";
import { GAME_OPTIONS, ROCKET_LEAGUE_MODES } from "../constants/gameConfig";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";
import {
  buildUnreadChatNotificationsFromConversations,
  getConversationsForUser,
} from "../services/chatService";
import { trackUiEvent } from "../services/analyticsService";
import { getBrowsePlayers, getLeaderboardEntries } from "../services/playerService";
import { getRecruiterDmOpenness, getRecruiterDmOpennessLabel } from "../services/recruiterService";
import { getSelectValue } from "../utils/selectHelpers";

const HOME_EDITORIAL_CARD_CLASS = "cc-home-editorial-card shadow-xl backdrop-blur-sm";

const RecruiterHomePage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { isArabic } = useLanguage();

  const [isLoading, setIsLoading] = useState(true);
  const [hasLoadedHome, setHasLoadedHome] = useState(false);
  const [selectedGame, setSelectedGame] = useState(GAME_OPTIONS[0]);
  const [selectedMode, setSelectedMode] = useState(ROCKET_LEAGUE_MODES[1]);
  const [recruiterDmOpenness, setRecruiterDmOpenness] = useState("CLOSED");
  const [candidateCount, setCandidateCount] = useState(0);
  const [topPlayers, setTopPlayers] = useState([]);
  const [conversations, setConversations] = useState([]);
  const [chatNotifications, setChatNotifications] = useState({ unreadCount: 0, items: [] });

  const leaderboardLink = useMemo(() => {
    if (selectedGame === "Rocket League") {
      return `/leaderboard?game=${encodeURIComponent(selectedGame)}&mode=${encodeURIComponent(selectedMode)}`;
    }

    return `/leaderboard?game=${encodeURIComponent(selectedGame)}`;
  }, [selectedGame, selectedMode]);

  const leaderboardTitle =
    selectedGame === "Rocket League"
      ? isArabic
        ? `أفضل اللاعبين في ${selectedGame} (${selectedMode})`
        : `Top Players in ${selectedGame} (${selectedMode})`
      : isArabic
        ? `أفضل اللاعبين في ${selectedGame}`
        : `Top Players in ${selectedGame}`;

  const recruiterDmStateLabel = getRecruiterDmOpennessLabel({ recruiterDmOpenness, isArabic });
  const scoutingPulseItems = useMemo(
    () => [
      {
        key: "pipeline",
        label: isArabic ? "حجم المواهب" : "Talent Pool",
        value: String(candidateCount),
      },
      {
        key: "messages",
        label: isArabic ? "رسائل جديدة" : "New Messages",
        value: String(chatNotifications.unreadCount),
      },
      {
        key: "dm",
        label: isArabic ? "سياسة الرسائل" : "DM Policy",
        value: recruiterDmStateLabel,
      },
    ],
    [candidateCount, chatNotifications.unreadCount, isArabic, recruiterDmStateLabel],
  );

  useEffect(() => {
    let isCancelled = false;

    const loadRecruiterHome = async () => {
      const showLoader = !hasLoadedHome;
      if (showLoader) {
        setIsLoading(true);
      }

      try {
        const [dmOpenness, nextConversations] = await Promise.all([
          getRecruiterDmOpenness({ userId: user.id }),
          getConversationsForUser(user.id, { forceRefresh: true }),
        ]);
        const notifications = await buildUnreadChatNotificationsFromConversations(
          nextConversations,
          { forceRefresh: true },
        );

        if (!isCancelled) {
          setRecruiterDmOpenness(dmOpenness);
          setConversations(nextConversations.slice(0, 3));
          setChatNotifications(notifications);
          setHasLoadedHome(true);
        }
      } catch (error) {
        console.error("Unable to load recruiter home:", error);

        if (!isCancelled && !hasLoadedHome) {
          setRecruiterDmOpenness("CLOSED");
          setConversations([]);
          setChatNotifications({ unreadCount: 0, items: [] });
        }
      } finally {
        if (!isCancelled && showLoader) {
          setIsLoading(false);
        }
      }
    };

    void loadRecruiterHome();

    const handleWindowFocus = () => {
      void loadRecruiterHome();
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === "visible") {
        void loadRecruiterHome();
      }
    };

    globalThis.addEventListener("focus", handleWindowFocus);
    document.addEventListener("visibilitychange", handleVisibilityChange);

    return () => {
      isCancelled = true;
      globalThis.removeEventListener("focus", handleWindowFocus);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };
  }, [hasLoadedHome, user.id]);

  useEffect(() => {
    let isCancelled = false;

    const loadTalentData = async () => {
      try {
        const [entries, candidates] = await Promise.all([
          getLeaderboardEntries({
            game: selectedGame,
            mode: selectedGame === "Rocket League" ? selectedMode : "",
          }),
          getBrowsePlayers({ game: selectedGame }),
        ]);

        if (isCancelled) {
          return;
        }

        setTopPlayers(entries.slice(0, 5));
        setCandidateCount(candidates.length);
      } catch (error) {
        console.error("Unable to load recruiter leaderboard data:", error);

        if (!isCancelled) {
          setTopPlayers([]);
          setCandidateCount(0);
        }
      }
    };

    void loadTalentData();

    return () => {
      isCancelled = true;
    };
  }, [selectedGame, selectedMode]);

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
          <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_top_left,rgba(32,140,140,0.25),transparent_56%)]" />
          <div className="relative flex flex-wrap items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-[#208c8c]">
                {isArabic ? "نبض الاستقطاب" : "Scouting Pulse"}
              </p>
              <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">
                {isArabic ? "رئيسية الاستقطاب" : "Scouting Home"}
              </h1>
              <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
                {isArabic
                  ? "واجهة سريعة لالتقاط أهم الإشارات اليومية قبل الانتقال للإدارة التفصيلية."
                  : "A fast, signal-first home to scan today before moving into deep operations."}
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <Chip variant="flat" className="bg-[#208c8c]/18 text-[#1d1d1d] dark:text-[#cae9ea]">
                {isArabic ? "حالة الرسائل:" : "DM state:"}{" "}
                {recruiterDmStateLabel}
              </Chip>

              <Chip variant="flat" className="bg-[#c9773b]/18 text-[#1d1d1d] dark:text-[#f8dfcb]">
                {isArabic ? "محادثات نشطة:" : "Active threads:"} {conversations.length}
              </Chip>
            </div>

            <div className="flex w-full flex-wrap gap-3">
              <Button
                as={Link}
                to="/players"
                className="cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                onPress={() =>
                  trackUiEvent("recruiter_home_browse_players_click", {
                    source: "hero",
                  })
                }
              >
                {isArabic ? "تصفح اللاعبين" : "Browse Players"}
              </Button>

              <Badge
                color="danger"
                content={chatNotifications.unreadCount}
                isInvisible={chatNotifications.unreadCount === 0}
              >
                <Button
                  as={Link}
                  to="/chats"
                  variant="bordered"
                  className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
                >
                  {isArabic ? "المحادثات" : "Chats"}
                </Button>
              </Badge>

              <Button
                as={Link}
                to="/dashboard"
                variant="bordered"
                className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
              >
                {isArabic ? "لوحة العمليات" : "Ops Dashboard"}
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
                {isArabic ? "إيقاع الاستقطاب" : "Scouting Rhythm"}
              </h2>
              <p className="cc-body-muted mt-1 text-[#4a3426] dark:text-[#f6e6d8]/82">
                {isArabic
                  ? "قياسات فورية لتوجيه قرارات التواصل والبحث اليوم."
                  : "Instant metrics to steer today outreach and discovery decisions."}
              </p>
            </div>

            <Button
              as={Link}
              to="/settings"
              variant="bordered"
              className="cc-button-text border-[#8b5a32]/55 bg-[#fff8f0]/65 text-[#4a3426] hover:bg-[#fbe7d2] dark:border-[#c9773b]/55 dark:bg-[#3a332b]/72 dark:text-[#f6e6d8] dark:hover:bg-[#4a4136]"
            >
              {isArabic ? "ضبط سياسة الرسائل" : "Refine DM Policy"}
            </Button>
          </div>

          <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
            {scoutingPulseItems.map((item) => (
              <div
                key={item.key}
                className="rounded-xl border border-[#8b5a32]/25 bg-[#fff8f1]/88 px-4 py-3 dark:border-[#c9773b]/30 dark:bg-[#332b22]/70"
              >
                <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[#8b5a32] dark:text-[#f6d8be]">
                  {item.label}
                </p>
                <p className="mt-2 text-lg font-semibold text-[#1d1d1d] dark:text-[#f6e6d8]">
                  {item.value}
                </p>
              </div>
            ))}
          </div>
        </Card>

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          <Card
            bg="bg-[#ecfaf6]/92 dark:bg-[#243735]/72"
            className={`${HOME_EDITORIAL_CARD_CLASS} border-[#208c8c]/38`}
          >
            <h3 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
              {isArabic ? "ملخص المرشحين" : "Candidate Summary"}
            </h3>
            <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic
                ? "عدد المرشحين في اللعبة المختارة حاليا."
                : "Current candidate pool size for the selected game."}
            </p>
            <p className="mt-4 text-3xl font-bold text-[#208c8c]">{candidateCount}</p>
            <Button
              as={Link}
              to="/players"
              className="cc-button-text mt-4 w-full bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
            >
              {isArabic ? "الذهاب إلى التصفح" : "Go to Browse"}
            </Button>
          </Card>

          <Card
            bg="bg-[#f2f9ff]/92 dark:bg-[#253342]/72"
            className={`${HOME_EDITORIAL_CARD_CLASS} border-[#4f86a3]/35 lg:col-span-2`}
          >
            <h3 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
              {isArabic ? "الانتقال السريع حسب اللعبة" : "Game Quick Jump"}
            </h3>

            <div className="mt-4 grid grid-cols-1 gap-3 md:grid-cols-3">
              <Select
                label={isArabic ? "اللعبة" : "Game"}
                labelPlacement="outside"
                selectedKeys={[selectedGame]}
                onSelectionChange={(keys) => {
                  const nextGame = getSelectValue(keys);
                  if (nextGame) {
                    setSelectedGame(String(nextGame));
                  }
                }}
              >
                {GAME_OPTIONS.map((game) => (
                  <SelectItem key={game}>{game}</SelectItem>
                ))}
              </Select>

              {selectedGame === "Rocket League" ? (
                <Select
                  label={isArabic ? "النمط" : "Mode"}
                  labelPlacement="outside"
                  selectedKeys={[selectedMode]}
                  onSelectionChange={(keys) => {
                    const nextMode = getSelectValue(keys);
                    if (nextMode) {
                      setSelectedMode(String(nextMode));
                    }
                  }}
                >
                  {ROCKET_LEAGUE_MODES.map((mode) => (
                    <SelectItem key={mode}>{mode}</SelectItem>
                  ))}
                </Select>
              ) : (
                <div className="md:col-span-1" />
              )}

              <Button
                as={Link}
                to={leaderboardLink}
                className="cc-button-text self-end bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
              >
                {isArabic ? "فتح المتصدرين" : "Open Leaderboard"}
              </Button>
            </div>
          </Card>

          <TopPlayersCard
            isArabic={isArabic}
            title={leaderboardTitle}
            entries={topPlayers}
            leaderboardLink={leaderboardLink}
          />

          <RecruiterChatPreviewCard
            isArabic={isArabic}
            conversations={conversations}
            onOpenThread={(threadId) => {
              trackUiEvent("recruiter_home_chat_preview_open", { threadId });
              navigate(`/chats?thread=${encodeURIComponent(String(threadId))}`);
            }}
          />

          <Card
            bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58"
            className={`${HOME_EDITORIAL_CARD_CLASS}`}
          >
            <h3 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
              {isArabic ? "إدارة الإعدادات" : "Settings Quick Access"}
            </h3>
            <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic
                ? "تحكم بحالة فتح الرسائل المباشرة من صفحة الإعدادات."
                : "Manage your direct-message openness in Settings."}
            </p>
            <Button
              as={Link}
              to="/settings"
              className="cc-button-text mt-4 w-full bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
            >
              {isArabic ? "فتح الإعدادات" : "Open Settings"}
            </Button>
          </Card>
        </div>
      </div>
    </section>
  );
};

export default RecruiterHomePage;
