import { Button } from "@heroui/react";
import { Link } from "react-router-dom";
import Card from "../Card";
import {
  DASHBOARD_CARD_BG,
  DASHBOARD_PANEL_CLASS,
  OUTLINE_BUTTON_CLASS,
} from "./dashboardShared";

const DashboardMetricCards = ({ cards, columnsClass = "md:grid-cols-3" }) => {
  return (
    <div className={`grid grid-cols-1 gap-4 ${columnsClass}`}>
      {cards.map((metricCard) => (
        <Card key={metricCard.key} bg={DASHBOARD_CARD_BG} className={DASHBOARD_PANEL_CLASS}>
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-[#3c4748] dark:text-[#cae9ea]/75">
            {metricCard.label}
          </p>
          <p className="mt-2 text-3xl font-bold text-[#208c8c]">{metricCard.value}</p>
          <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/85">
            {metricCard.helper}
          </p>
          <Button
            as={Link}
            to={metricCard.to}
            size="sm"
            variant="bordered"
            className={`mt-4 ${OUTLINE_BUTTON_CLASS}`}
          >
            {metricCard.actionLabel}
          </Button>
        </Card>
      ))}
    </div>
  );
};

export default DashboardMetricCards;
