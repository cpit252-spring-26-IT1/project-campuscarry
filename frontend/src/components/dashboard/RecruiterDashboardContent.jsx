import { Button, Select, SelectItem } from "@heroui/react";
import { Link } from "react-router-dom";
import Card from "../Card";
import DashboardMetricCards from "./DashboardMetricCards";
import {
  DASHBOARD_CARD_BG,
  DASHBOARD_PANEL_CLASS,
  OUTLINE_BUTTON_CLASS,
  PRIMARY_BUTTON_CLASS,
} from "./dashboardShared";

const RecruiterDashboardContent = ({
  gameOptions,
  isArabic,
  isRecruiterInsightsLoading,
  recruiterDmStateLabel,
  recruiterLeaderboardLink,
  recruiterMetricCards,
  recruiterStats,
  selectedGame,
  setSelectedGame,
}) => {
  return (
    <div className="space-y-4">
      <DashboardMetricCards cards={recruiterMetricCards} />
      <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <Card bg={DASHBOARD_CARD_BG} className={DASHBOARD_PANEL_CLASS}>
          <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "مركز الاتصالات" : "Communication Control"}
          </h2>
          <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic
              ? "تابع حالة الرسائل المباشرة ونظّم محادثاتك المفتوحة."
              : "Track DM openness and manage your active conversation flow."}
          </p>
          <div className="mt-4 rounded-md border border-[#3c4748]/30 px-3 py-2">
            <p className="text-sm text-[#273b40] dark:text-[#cae9ea]/90">
              {isArabic ? "حالة استقبال الرسائل" : "Current DM State"}: {recruiterDmStateLabel}
            </p>
            <p className="mt-1 text-sm text-[#273b40] dark:text-[#cae9ea]/90">
              {isArabic
                ? `محادثات نشطة: ${recruiterStats.threadCount}`
                : `Active threads: ${recruiterStats.threadCount}`}
            </p>
          </div>
          <div className="mt-5 flex flex-wrap gap-3">
            <Button as={Link} to="/chats" className={PRIMARY_BUTTON_CLASS}>
              {isArabic ? "إدارة المحادثات" : "Manage Chats"}
            </Button>
            <Button as={Link} to="/settings" variant="bordered" className={OUTLINE_BUTTON_CLASS}>
              {isArabic ? "ضبط السياسة" : "Policy Settings"}
            </Button>
          </div>
        </Card>

        <Card bg={DASHBOARD_CARD_BG} className={DASHBOARD_PANEL_CLASS}>
          <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "تركيز الاستكشاف" : "Scouting Focus"}
          </h2>
          <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic
              ? "حدّد اللعبة لمعايرة البحث وفتح القائمة المناسبة بسرعة."
              : "Set a game focus to tune discovery and jump to the right talent list quickly."}
          </p>
          <div className="mt-4">
            <Select
              label={isArabic ? "اللعبة" : "Game"}
              labelPlacement="outside"
              selectedKeys={[selectedGame]}
              onSelectionChange={(keys) => {
                const selectedValue = Array.from(keys)[0];
                if (selectedValue) {
                  setSelectedGame(String(selectedValue));
                }
              }}
            >
              {gameOptions.map((game) => (
                <SelectItem key={game}>{game}</SelectItem>
              ))}
            </Select>
          </div>
          <p className="cc-body-muted mt-3 text-[#273b40] dark:text-[#cae9ea]/85">
            {isRecruiterInsightsLoading
              ? isArabic
                ? "جار تحديث مؤشرات اللعبة..."
                : "Refreshing game indicators..."
              : isArabic
                ? `مرشحون محتملون في ${selectedGame}: ${recruiterStats.candidateCount}`
                : `Potential candidates in ${selectedGame}: ${recruiterStats.candidateCount}`}
          </p>
          <div className="mt-5 flex flex-wrap gap-3">
            <Button as={Link} to="/players" className={PRIMARY_BUTTON_CLASS}>
              {isArabic ? "فتح التصفح" : "Open Browse"}
            </Button>
            <Button as={Link} to={recruiterLeaderboardLink} variant="bordered" className={OUTLINE_BUTTON_CLASS}>
              {isArabic ? "لوحة المتصدرين" : "Leaderboard"}
            </Button>
          </div>
        </Card>
      </div>
    </div>
  );
};

export default RecruiterDashboardContent;
