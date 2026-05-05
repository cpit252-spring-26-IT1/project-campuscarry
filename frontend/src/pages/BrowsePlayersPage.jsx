import { useEffect, useMemo, useState } from "react";
import { Button, Select, SelectItem } from "@heroui/react";
import { Link } from "react-router-dom";
import Card from "../components/Card";
import ThemedLoader from "../components/ThemedLoader";
import { GAME_OPTIONS, GAME_RANK_OPTIONS, GAME_ROLE_OPTIONS } from "../constants/gameConfig";
import useLanguage from "../hooks/useLanguage";
import { getBrowsePlayers } from "../services/playerService";
import { getSelectValue } from "../utils/selectHelpers";

const BrowsePlayersPage = () => {
  const { isArabic } = useLanguage();
  const [game, setGame] = useState(GAME_OPTIONS[0]);
  const [role, setRole] = useState("");
  const [minimumRank, setMinimumRank] = useState("");
  const [players, setPlayers] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  const roleOptions = useMemo(() => GAME_ROLE_OPTIONS[game] || [], [game]);
  const rankOptions = useMemo(() => GAME_RANK_OPTIONS[game] || [], [game]);

  useEffect(() => {
    const loadPlayers = async () => {
      setIsLoading(true);
      try {
        const entries = await getBrowsePlayers({ game, role, minimumRank });
        setPlayers(entries);
      } catch (error) {
        console.error("Failed to load players for browse page:", error);
        setPlayers([]);
      } finally {
        setIsLoading(false);
      }
    };

    loadPlayers();
  }, [game, minimumRank, role]);

  return (
    <section className="bg-transparent px-4 py-10">
      <div className="m-auto max-w-7xl">
        <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="mb-6 shadow-xl backdrop-blur-sm">
          <h1 className="cc-title-section mb-2 text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "تصفح اللاعبين" : "Browse Players"}
          </h1>
          <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic
              ? "فلتر اللاعبين حسب اللعبة والدور والحد الأدنى للرتبة."
              : "Filter players by game, role, and minimum rank."}
          </p>
        </Card>

        <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="mb-6 shadow-xl backdrop-blur-sm">
          <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
            <Select
              label={isArabic ? "اللعبة" : "Game"}
              labelPlacement="outside"
              selectedKeys={game ? [game] : []}
              onSelectionChange={(keys) => {
                const selectedGame = getSelectValue(keys);
                setGame(selectedGame);
                setRole("");
                setMinimumRank("");
              }}
            >
              {GAME_OPTIONS.map((option) => (
                <SelectItem key={option}>{option}</SelectItem>
              ))}
            </Select>

            <Select
              label={isArabic ? "الدور" : "Role"}
              labelPlacement="outside"
              selectedKeys={role ? [role] : []}
              onSelectionChange={(keys) => setRole(getSelectValue(keys))}
              isDisabled={!roleOptions.length}
            >
              {roleOptions.map((option) => (
                <SelectItem key={option}>{option}</SelectItem>
              ))}
            </Select>

            <Select
              label={isArabic ? "الحد الأدنى للرتبة" : "Minimum Rank"}
              labelPlacement="outside"
              selectedKeys={minimumRank ? [minimumRank] : []}
              onSelectionChange={(keys) => setMinimumRank(getSelectValue(keys))}
            >
              {rankOptions.map((option) => (
                <SelectItem key={option}>{option}</SelectItem>
              ))}
            </Select>
          </div>
        </Card>

        {isLoading ? (
          <div className="py-20 text-center">
            <ThemedLoader />
          </div>
        ) : players.length === 0 ? (
          <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
            <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic
                ? "لا يوجد لاعبين مطابقين لهذه الفلاتر."
                : "No players match these filters."}
            </p>
          </Card>
        ) : (
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-3">
            {players.map((player) => (
              <Card
                key={`${player.userId}-${player.game}-${player.rank}`}
                bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58"
                className="shadow-xl backdrop-blur-sm"
              >
                <p className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                  {player.username}
                </p>
                <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/85">
                  {player.fullName}
                </p>
                <p className="cc-body-muted mt-3 text-[#273b40] dark:text-[#cae9ea]/90">
                  {player.game} • {player.rank}
                </p>
                {player.game !== "Overwatch 2" ? (
                  <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/90">
                    {player.ratingValue !== null && player.ratingValue !== undefined
                      ? `${player.ratingLabel}: ${player.ratingValue}`
                      : `${player.ratingLabel}: -`}
                  </p>
                ) : null}
                {(Array.isArray(player.inGameRoles) && player.inGameRoles.length) ||
                player.inGameRole ? (
                  <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/90">
                    {isArabic ? "الدور" : "Role"}:{" "}
                    {Array.isArray(player.inGameRoles) && player.inGameRoles.length
                      ? player.inGameRoles.join(", ")
                      : player.inGameRole}
                  </p>
                ) : null}

                <Button
                  as={Link}
                  to={`/players/${player.userId}`}
                  className="cc-button-text mt-4 bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                >
                  {isArabic ? "عرض الملف" : "View Profile"}
                </Button>

                <Button
                  as={Link}
                  to={`/chats?with=${encodeURIComponent(String(player.userId))}`}
                  variant="bordered"
                  className="cc-button-text mt-2 border-[#3c4748]/50 text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]"
                >
                  {isArabic ? "مراسلة اللاعب" : "Message Player"}
                </Button>
              </Card>
            ))}
          </div>
        )}
      </div>
    </section>
  );
};

export default BrowsePlayersPage;
