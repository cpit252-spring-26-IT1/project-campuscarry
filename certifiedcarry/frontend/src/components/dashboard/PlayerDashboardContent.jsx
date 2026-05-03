import { Badge, Button, Listbox, ListboxItem, Switch } from "@heroui/react";
import { Link } from "react-router-dom";
import Card from "../Card";
import ThemedLoader from "../ThemedLoader";
import DashboardMetricCards from "./DashboardMetricCards";
import {
  DASHBOARD_CARD_BG,
  DASHBOARD_PANEL_CLASS,
  OUTLINE_BUTTON_CLASS,
  PRIMARY_BUTTON_CLASS,
} from "./dashboardShared";

const PlayerDashboardContent = ({
  chatNotifications,
  completionProgress,
  dashboardMetricCards,
  handleUpdatePlayerChatPreference,
  isArabic,
  isLoading,
  isUpdatingChatPreference,
  missingFieldLabels,
  navigate,
  playerProfile,
  playerRankPosition,
  playerStatus,
  showRatingInPlayerPreview,
  user,
}) => {
  if (isLoading) {
    return (
      <Card bg={DASHBOARD_CARD_BG} className={`py-10 text-center ${DASHBOARD_PANEL_CLASS}`}>
        <ThemedLoader />
      </Card>
    );
  }

  return (
    <>
      <div className="mb-4">
        <DashboardMetricCards cards={dashboardMetricCards} />
      </div>
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
        <Card bg={DASHBOARD_CARD_BG} className={`${DASHBOARD_PANEL_CLASS} lg:col-span-2`}>
          {playerStatus === "INCOMPLETE" ? (
            <>
              <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                {isArabic
                  ? "أكمل ملفك للظهور في لوحة المتصدرين"
                  : "Complete your profile to appear on the leaderboard"}
              </h2>
              <div className="mt-4">
                <div className="mb-2 flex items-center justify-between text-sm text-[#273b40] dark:text-[#cae9ea]/85">
                  <span>{isArabic ? "تقدم الملف" : "Profile progress"}</span>
                  <span>{completionProgress.value}%</span>
                </div>
                <div className="h-2 w-full overflow-hidden rounded-full bg-[#3c4748]/20">
                  <div
                    className="h-full bg-[#208c8c] transition-all duration-300"
                    style={{ width: `${completionProgress.value}%` }}
                  />
                </div>
              </div>
              <ul className="mt-4 list-disc space-y-1 ps-5 text-sm text-[#273b40] dark:text-[#cae9ea]/85">
                {completionProgress.missing.map((field) => (
                  <li key={field}>{missingFieldLabels[field] || field}</li>
                ))}
              </ul>
              <Button as={Link} to="/profile-setup" className={`mt-5 ${PRIMARY_BUTTON_CLASS}`}>
                {isArabic ? "إكمال الملف" : "Complete Profile"}
              </Button>
            </>
          ) : null}

          {playerStatus === "PENDING" ? (
            <>
              <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                {isArabic ? "حالة التحقق: قيد المراجعة" : "Rank verification: Pending Admin Approval"}
              </h2>
              <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
                {isArabic
                  ? "يمكنك تصفح لوحة المتصدرين، لكن لن يظهر ملفك حتى تتم الموافقة."
                  : "You can browse the leaderboard, but your profile will appear only after approval."}
              </p>
              <div className="mt-5 flex flex-wrap gap-3">
                <Button as={Link} to="/leaderboard" className={PRIMARY_BUTTON_CLASS}>
                  {isArabic ? "عرض لوحة المتصدرين" : "Open Leaderboard"}
                </Button>
                <Button as={Link} to={`/players/${user.id}`} variant="bordered" className={OUTLINE_BUTTON_CLASS}>
                  {isArabic ? "عرض ملفي" : "View My Profile"}
                </Button>
              </div>
            </>
          ) : null}

          {playerStatus === "VERIFIED" ? (
            <>
              <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                {isArabic ? "تم التحقق من الرتبة" : "Rank Verified"}
              </h2>
              <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
                {isArabic
                  ? "ملفك ظاهر الآن في لوحة المتصدرين ويمكنك متابعة ترتيبك."
                  : "Your profile is now visible on the leaderboard."}
              </p>
              <p className="cc-body-lead mt-3 text-[#273b40] dark:text-[#cae9ea]/90">
                {isArabic ? "مركزك الحالي" : "Current rank position"}: {playerRankPosition || "-"}
              </p>
              <div className="mt-5 flex flex-wrap gap-3">
                <Button as={Link} to="/leaderboard" className={PRIMARY_BUTTON_CLASS}>
                  {isArabic ? "الذهاب للمتصدرين" : "Go to Leaderboard"}
                </Button>
                <Button as={Link} to="/profile-setup" variant="bordered" className={OUTLINE_BUTTON_CLASS}>
                  {isArabic ? "تحديث رتبتي" : "Update My Rank"}
                </Button>
              </div>
            </>
          ) : null}

          {playerStatus === "DECLINED" ? (
            <>
              <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                {isArabic ? "تم رفض إثبات الرتبة" : "Rank Verification Declined"}
              </h2>
              <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
                {isArabic
                  ? "يمكنك تعديل بيانات الرتبة ورفع صورة أوضح ثم إعادة الإرسال."
                  : "Update your rank details and upload a clearer proof screenshot to resubmit."}
              </p>
              {playerProfile?.declineReason ? (
                <p className="mt-3 rounded-md border border-red-300/50 bg-red-50/70 px-3 py-2 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-200">
                  {isArabic ? "سبب الرفض" : "Decline Reason"}: {playerProfile.declineReason}
                </p>
              ) : null}
              <div className="mt-5 flex flex-wrap gap-3">
                <Button as={Link} to="/profile-setup" className={PRIMARY_BUTTON_CLASS}>
                  {isArabic ? "تعديل الرتبة" : "Edit Rank Details"}
                </Button>
                <Button as={Link} to="/profile-setup" variant="bordered" className={OUTLINE_BUTTON_CLASS}>
                  {isArabic ? "تحديث صورة الإثبات" : "Update Proof Image"}
                </Button>
              </div>
            </>
          ) : null}
        </Card>

        <Card bg={DASHBOARD_CARD_BG} className={DASHBOARD_PANEL_CLASS}>
          <h3 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "معاينة الملف" : "Profile Preview"}
          </h3>
          <p className="cc-body-muted mt-3 text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic ? "Gamertag" : "Gamertag"}: {playerProfile?.username || "-"}
          </p>
          <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic ? "اللعبة" : "Game"}: {playerProfile?.game || "-"}
          </p>
          <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic ? "الرتبة" : "Rank"}: {playerProfile?.rank || "-"}
          </p>
          {showRatingInPlayerPreview ? (
            <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
              {playerProfile?.ratingLabel || "MMR"}: {playerProfile?.ratingValue ?? "-"}
            </p>
          ) : null}
          <Button as={Link} to={`/players/${user.id}`} className={`mt-4 w-full ${PRIMARY_BUTTON_CLASS}`}>
            {isArabic ? "فتح ملفي" : "Open My Profile"}
          </Button>
        </Card>

        <Card bg={DASHBOARD_CARD_BG} className={DASHBOARD_PANEL_CLASS}>
          <h3 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "المحادثات" : "Chats"}
          </h3>
          <div className="mt-3 flex items-center justify-between gap-2">
            <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic ? "الرسائل غير المقروءة" : "Unread messages"}
            </p>
            <Badge
              color="danger"
              content={chatNotifications.unreadCount}
              isInvisible={chatNotifications.unreadCount === 0}
            >
              <span className="h-2 w-2" />
            </Badge>
          </div>
          {chatNotifications.items.length === 0 ? (
            <p className="cc-body-muted mt-3 text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic ? "لا توجد إشعارات محادثات جديدة." : "No new chat notifications."}
            </p>
          ) : (
            <Listbox
              aria-label={isArabic ? "إشعارات المحادثات" : "Chat notifications"}
              className="mt-2"
              onAction={(key) => navigate(`/chats?thread=${encodeURIComponent(String(key))}`)}
            >
              {chatNotifications.items.slice(0, 3).map((item) => (
                <ListboxItem
                  key={String(item.threadId)}
                  textValue={`${item.senderName} ${item.preview}`}
                  description={item.preview}
                >
                  {item.senderName}
                </ListboxItem>
              ))}
            </Listbox>
          )}
          <Button as={Link} to="/chats" className={`mt-4 w-full ${PRIMARY_BUTTON_CLASS}`}>
            {isArabic ? "فتح المحادثات" : "Open Chats"}
          </Button>
          <div className="mt-5 border-t border-[#3c4748]/25 pt-4">
            <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic ? "خصوصية الرسائل" : "Chat Privacy"}
            </p>
            <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/85">
              {playerProfile?.allowPlayerChats === false
                ? isArabic
                  ? "تستقبل حاليا الرسائل من الكشّافين فقط."
                  : "You currently receive chats from scouts only."
                : isArabic
                  ? "تستقبل الرسائل من اللاعبين والكشّافين."
                  : "You currently receive chats from players and scouts."}
            </p>
            <Switch
              className="mt-3"
              isSelected={playerProfile?.allowPlayerChats !== false}
              isDisabled={isUpdatingChatPreference}
              onValueChange={handleUpdatePlayerChatPreference}
            >
              {isArabic ? "استقبال رسائل اللاعبين" : "Allow Player Chats"}
            </Switch>
          </div>
        </Card>
      </div>
    </>
  );
};

export default PlayerDashboardContent;
