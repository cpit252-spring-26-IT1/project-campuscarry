import { useEffect, useMemo, useState } from "react";
import {
  Button,
  Input,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableColumn,
  TableHeader,
  TableRow,
} from "@heroui/react";
import { Link, useSearchParams } from "react-router-dom";
import Card from "../components/Card";
import ThemedLoader from "../components/ThemedLoader";
import {
  GAME_OPTIONS,
  GAME_ROLE_OPTIONS,
  normalizeGameName,
  ROCKET_LEAGUE_MODES,
  requiresRatingValue,
} from "../constants/gameConfig";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";
import { getLeaderboardEntries, getPlayerProfileDetails } from "../services/playerService";
import { getSelectValue } from "../utils/selectHelpers";

const LeaderboardPage = () => {
  const { user } = useAuth();
  const { isArabic } = useLanguage();
  const [searchParams, setSearchParams] = useSearchParams();
  const initialGame = normalizeGameName(searchParams.get("game") || GAME_OPTIONS[0]);
  const isPlayer = user?.role === "PLAYER";

  const [selectedGame, setSelectedGame] = useState(initialGame);
  const [lockedPlayerGame, setLockedPlayerGame] = useState("");
  const [isPlayerGameLoading, setIsPlayerGameLoading] = useState(isPlayer);
  const [selectedRocketLeagueMode, setSelectedRocketLeagueMode] = useState("2v2");
  const [selectedRole, setSelectedRole] = useState("");
  const [usernameSearch, setUsernameSearch] = useState("");
  const [entries, setEntries] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const effectiveGame = isPlayer ? lockedPlayerGame : selectedGame;
  const roleOptions = useMemo(() => GAME_ROLE_OPTIONS[effectiveGame] || [], [effectiveGame]);
  const showRatingColumn = effectiveGame !== "Overwatch 2";
  const hasRoleColumn = roleOptions.length > 0;
  const filteredEntries = useMemo(() => {
    const normalizedQuery = usernameSearch.trim().toLowerCase();

    if (!normalizedQuery) {
      return entries;
    }

    return entries.filter((entry) =>
      String(entry.username || "")
        .toLowerCase()
        .includes(normalizedQuery),
    );
  }, [entries, usernameSearch]);

  const columns = useMemo(() => {
    const baseColumns = [
      { key: "position", label: "#" },
      { key: "username", label: isArabic ? "اسم اللاعب" : "Gamertag" },
      { key: "rank", label: isArabic ? "الرتبة" : "Rank" },
    ];

    if (showRatingColumn) {
      baseColumns.push({ key: "rating", label: isArabic ? "MMR/تقييم المهارة" : "MMR/Skill" });
    }

    if (hasRoleColumn) {
      baseColumns.push({ key: "role", label: isArabic ? "الدور" : "Role" });
    }

    return baseColumns;
  }, [hasRoleColumn, isArabic, showRatingColumn]);

  const renderCell = (entry, columnKey) => {
    switch (String(columnKey)) {
      case "position":
        return (
          <span className="font-semibold text-[#1d1d1d] dark:text-[#cae9ea]">{entry.position}</span>
        );
      case "username":
        return (
          <Link
            to={`/players/${entry.userId}`}
            className="font-semibold text-[#208c8c] hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
          >
            {entry.username}
          </Link>
        );
      case "rank":
        return <span className="text-[#273b40] dark:text-[#cae9ea]/90">{entry.rank}</span>;
      case "rating":
        return (
          <span className="text-[#273b40] dark:text-[#cae9ea]/90">
            {entry.ratingValue !== null && entry.ratingValue !== undefined
              ? `${entry.ratingValue} ${entry.ratingLabel || ""}`.trim()
              : requiresRatingValue(entry.game, entry.rank)
                ? "-"
                : "N/A"}
          </span>
        );
      case "role":
        return (
          <span className="text-[#273b40] dark:text-[#cae9ea]/90">
            {Array.isArray(entry.inGameRoles) && entry.inGameRoles.length
              ? entry.inGameRoles.join(", ")
              : entry.inGameRole || "-"}
          </span>
        );
      default:
        return null;
    }
  };

  useEffect(() => {
    if (!isPlayer) {
      setIsPlayerGameLoading(false);
      return;
    }

    const loadPlayerGame = async () => {
      setIsPlayerGameLoading(true);

      try {
        const profile = await getPlayerProfileDetails(user.id);
        const profileGame = normalizeGameName(profile?.game || "");
        setLockedPlayerGame(profileGame);

        if (profileGame === "Rocket League") {
          const preferredMode =
            profile?.primaryRocketLeagueMode || profile?.rocketLeagueModes?.[0]?.mode;
          if (preferredMode && ROCKET_LEAGUE_MODES.includes(preferredMode)) {
            setSelectedRocketLeagueMode(preferredMode);
          }
        }

        if (profileGame) {
          setSelectedGame(profileGame);
        }
        setSelectedRole("");
      } catch (error) {
        console.error("Failed to load player game for leaderboard:", error);
        setLockedPlayerGame("");
      } finally {
        setIsPlayerGameLoading(false);
      }
    };

    loadPlayerGame();
  }, [isPlayer, user?.id]);

  useEffect(() => {
    if (isPlayer && isPlayerGameLoading) {
      return;
    }

    if (!effectiveGame) {
      setEntries([]);
      setIsLoading(false);
      return;
    }

    const loadEntries = async () => {
      setIsLoading(true);
      try {
        const data = await getLeaderboardEntries({
          game: effectiveGame,
          role: selectedRole,
          mode: effectiveGame === "Rocket League" ? selectedRocketLeagueMode : "",
        });

        setEntries(data);
      } catch (error) {
        console.error("Failed to load leaderboard:", error);
        setEntries([]);
      } finally {
        setIsLoading(false);
      }
    };

    loadEntries();
  }, [effectiveGame, isPlayer, isPlayerGameLoading, selectedRole, selectedRocketLeagueMode]);

  const handleGameChange = (game) => {
    if (isPlayer) {
      return;
    }

    setSelectedGame(game);
    setSelectedRole("");

    if (game === "Rocket League" && !ROCKET_LEAGUE_MODES.includes(selectedRocketLeagueMode)) {
      setSelectedRocketLeagueMode("2v2");
    }

    const nextSearchParams = new URLSearchParams(searchParams);
    nextSearchParams.set("game", game);
    setSearchParams(nextSearchParams);
  };

  return (
    <section className="bg-transparent px-4 py-10">
      <div className="m-auto max-w-6xl">
        <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="mb-6 shadow-xl backdrop-blur-sm">
          <h1 className="cc-title-section mb-2 text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "لوحة المتصدرين" : "Leaderboard"}
          </h1>
          <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic
              ? "تصفح اللاعبين الموثقين مرتبين حسب MMR/Skill Rating أو الرتبة عند عدم وجود تقييم."
              : "Browse verified players ordered by MMR/Skill Rating or by rank when no rating exists."}
          </p>
        </Card>

        {isPlayer && !isPlayerGameLoading && !lockedPlayerGame ? (
          <Card
            bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58"
            className="mb-6 shadow-xl backdrop-blur-sm"
          >
            <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic
                ? "أكمل إعداد ملفك واختيار لعبتك أولا لعرض لوحة المتصدرين الخاصة بك."
                : "Complete your profile setup and choose your game first to access your game leaderboard."}
            </p>
            <Button
              as={Link}
              to="/profile-setup"
              className="cc-button-text mt-4 bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
            >
              {isArabic ? "إكمال الملف" : "Complete Profile"}
            </Button>
          </Card>
        ) : null}

        <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
          {isPlayer ? (
            <p className="mb-5 text-sm font-semibold text-[#273b40] dark:text-[#cae9ea]/90">
              {isArabic
                ? `تعرض هذه الصفحة متصدرين لعبة ${effectiveGame || "-"}${
                    effectiveGame === "Rocket League" ? ` - ${selectedRocketLeagueMode}` : ""
                  }`
                : `Showing leaderboard for ${effectiveGame || "-"}${
                    effectiveGame === "Rocket League" ? ` - ${selectedRocketLeagueMode}` : ""
                  }`}
            </p>
          ) : (
            <div className="mb-5 flex flex-wrap gap-2">
              {GAME_OPTIONS.map((game) => {
                const isActive = selectedGame === game;
                return (
                  <Button
                    key={game}
                    size="sm"
                    radius="md"
                    className={
                      isActive
                        ? "cc-button-text bg-[#208c8c] text-[#cae9ea]"
                        : "cc-button-text border border-[#3c4748]/50 bg-transparent text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]"
                    }
                    onPress={() => handleGameChange(game)}
                  >
                    {game}
                  </Button>
                );
              })}
            </div>
          )}

          {effectiveGame === "Rocket League" ? (
            <div className="mb-5">
              <p className="mb-2 text-xs font-semibold uppercase tracking-wide text-[#273b40] dark:text-[#cae9ea]/80">
                {isArabic ? "نمط اللعب" : "Game Mode"}
              </p>
              <div className="flex flex-wrap gap-2">
                {ROCKET_LEAGUE_MODES.map((mode) => {
                  const isActive = selectedRocketLeagueMode === mode;

                  return (
                    <Button
                      key={mode}
                      size="sm"
                      radius="md"
                      className={
                        isActive
                          ? "cc-button-text bg-[#208c8c] text-[#cae9ea]"
                          : "cc-button-text border border-[#3c4748]/50 bg-transparent text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]"
                      }
                      onPress={() => setSelectedRocketLeagueMode(mode)}
                    >
                      {mode}
                    </Button>
                  );
                })}
              </div>
            </div>
          ) : null}

          {roleOptions.length ? (
            <div className="mb-6 max-w-sm">
              <Select
                label={isArabic ? "فلتر الدور" : "Role Filter"}
                labelPlacement="outside"
                selectedKeys={selectedRole ? [selectedRole] : []}
                onSelectionChange={(keys) => setSelectedRole(getSelectValue(keys))}
              >
                {roleOptions.map((role) => (
                  <SelectItem key={role}>{role}</SelectItem>
                ))}
              </Select>
            </div>
          ) : null}

          <div className="mb-6 max-w-md">
            <Input
              type="search"
              label={isArabic ? "بحث باسم اللاعب" : "Search by Gamertag"}
              labelPlacement="outside"
              placeholder={
                isArabic ? "ابحث عن لاعب في المتصدرين" : "Search for a player on this leaderboard"
              }
              value={usernameSearch}
              onValueChange={setUsernameSearch}
              isClearable
              onClear={() => setUsernameSearch("")}
            />
          </div>

          {isLoading ? (
            <div className="py-16 text-center">
              <ThemedLoader />
            </div>
          ) : entries.length === 0 ? (
            <p className="cc-body-lead py-10 text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic ? "لا توجد نتائج حاليا." : "No verified players found for this filter."}
            </p>
          ) : filteredEntries.length === 0 ? (
            <p className="cc-body-lead py-10 text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic
                ? "لا يوجد لاعب بهذا الاسم ضمن المتصدرين الحاليين."
                : "No leaderboard result found for that username in this view."}
            </p>
          ) : (
            <div className="overflow-x-auto">
              <Table removeWrapper aria-label={isArabic ? "لوحة المتصدرين" : "Leaderboard table"}>
                <TableHeader columns={columns}>
                  {(column) => <TableColumn key={column.key}>{column.label}</TableColumn>}
                </TableHeader>
                <TableBody items={filteredEntries}>
                  {(entry) => (
                    <TableRow key={`${entry.userId}-${entry.position}`}>
                      {(columnKey) => <TableCell>{renderCell(entry, columnKey)}</TableCell>}
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </div>
          )}
        </Card>
      </div>
    </section>
  );
};

export default LeaderboardPage;
