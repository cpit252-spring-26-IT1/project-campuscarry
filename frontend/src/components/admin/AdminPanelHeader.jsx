import { Button, Tab, Tabs } from "@heroui/react";
import { Link } from "react-router-dom";
import Card from "../Card";
import {
  ADMIN_CARD_BG,
  ADMIN_CARD_CLASS,
  OUTLINE_BUTTON_CLASS,
  PRIMARY_BUTTON_CLASS,
} from "./adminShared";

const AdminPanelHeader = ({
  activeTab,
  isArabic,
  isLoading,
  isRefreshing,
  loadQueues,
  onTabChange,
  tabButtons,
}) => {
  return (
    <Card bg={ADMIN_CARD_BG} className={`mb-6 ${ADMIN_CARD_CLASS}`}>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "لوحة الإدارة" : "Admin Panel"}
          </h1>
          <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic
              ? "إدارة طلبات المستقطبين وإثبات الرتب ومراجعة القرارات المرفوضة."
              : "Review scout accounts, rank submissions, and declined history."}
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <Button as={Link} to="/admin/users" variant="bordered" className={OUTLINE_BUTTON_CLASS}>
            {isArabic ? "حسابات المنصة" : "Platform Accounts"}
          </Button>
          <Button as={Link} to="/leaderboard" variant="bordered" className={OUTLINE_BUTTON_CLASS}>
            {isArabic ? "المتصدرين" : "Leaderboard"}
          </Button>
          <Button as={Link} to="/players" variant="bordered" className={OUTLINE_BUTTON_CLASS}>
            {isArabic ? "ملفات اللاعبين" : "Player Profiles"}
          </Button>
          <Button as={Link} to="/chats" variant="bordered" className={OUTLINE_BUTTON_CLASS}>
            {isArabic ? "المحادثات" : "Chats"}
          </Button>
          <Button
            className={PRIMARY_BUTTON_CLASS}
            isLoading={isRefreshing}
            isDisabled={isLoading || isRefreshing}
            onPress={() => loadQueues(true)}
          >
            {isArabic ? "تحديث" : "Refresh"}
          </Button>
        </div>
      </div>

      <Tabs
        selectedKey={activeTab}
        onSelectionChange={(key) => onTabChange(String(key))}
        variant="bordered"
        radius="sm"
      >
        {tabButtons.map((tab) => (
          <Tab key={tab.key} title={`${tab.label} (${tab.count})`} />
        ))}
      </Tabs>
    </Card>
  );
};

export default AdminPanelHeader;
