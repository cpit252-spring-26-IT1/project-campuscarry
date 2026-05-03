import { Button } from "@heroui/react";
import { Link } from "react-router-dom";
import Card from "./Card";

const TopPlayersCard = ({ isArabic, title, entries, leaderboardLink }) => {
  const hasEntries = Array.isArray(entries) && entries.length > 0;

  return (
    <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
      <h3 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">{title}</h3>

      {hasEntries ? (
        <div className="mt-4 space-y-2">
          {entries.slice(0, 5).map((entry) => (
            <div
              key={`${entry.userId}-${entry.position}`}
              className="flex items-center justify-between rounded-md border border-[#3c4748]/30 px-3 py-2"
            >
              <p className="text-sm text-[#273b40] dark:text-[#cae9ea]/90">
                #{entry.position} {entry.username}
              </p>
              <p className="text-xs text-[#273b40] dark:text-[#cae9ea]/90">
                {entry.game === "Overwatch 2"
                  ? `${isArabic ? "الرتبة" : "Rank"}: ${entry.rank || "-"}`
                  : `${entry.ratingLabel || "MMR"}: ${entry.ratingValue ?? "-"}`}
              </p>
            </div>
          ))}
        </div>
      ) : (
        <p className="cc-body-muted mt-4 text-[#273b40] dark:text-[#cae9ea]/85">
          {isArabic ? "لا توجد بيانات متاحة حاليا." : "No leaderboard entries available yet."}
        </p>
      )}

      <Button
        as={Link}
        to={leaderboardLink || "/leaderboard"}
        className="cc-button-text mt-4 w-full bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
      >
        {isArabic ? "عرض المتصدرين" : "Open Leaderboard"}
      </Button>
    </Card>
  );
};

export default TopPlayersCard;
