import { Button } from "@heroui/react";
import { Link } from "react-router-dom";
import Card from "../Card";
import {
  DASHBOARD_CARD_BG,
  DASHBOARD_PANEL_CLASS,
  OUTLINE_BUTTON_CLASS,
} from "./dashboardShared";

const DashboardHero = ({ dashboardDescription, homePath, isArabic, isPlayer, user }) => {
  return (
    <Card bg={DASHBOARD_CARD_BG} className={`mb-6 ${DASHBOARD_PANEL_CLASS}`}>
      <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">
        {isPlayer
          ? isArabic
            ? "لوحة اللاعب"
            : "Player Dashboard"
          : isArabic
            ? "لوحة عمليات المستقطب"
            : "Recruiter Ops Dashboard"}
      </h1>
      <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
        {isArabic ? "أهلا بعودتك" : "Welcome back"}, {user.fullName}
      </p>
      <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/85">
        {dashboardDescription}
      </p>
      <div className="mt-4 flex flex-wrap gap-3">
        <Button as={Link} to={homePath} variant="bordered" className={OUTLINE_BUTTON_CLASS}>
          {isArabic ? "العودة إلى الرئيسية" : "Back to Home"}
        </Button>
        <Button as={Link} to="/settings" variant="bordered" className={OUTLINE_BUTTON_CLASS}>
          {isArabic ? "إعدادات الحساب" : "Account Settings"}
        </Button>
      </div>
    </Card>
  );
};

export default DashboardHero;
