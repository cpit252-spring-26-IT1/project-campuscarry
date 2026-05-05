import { Button } from "@heroui/react";
import { Link } from "react-router-dom";
import Card from "./Card";

const NextActionCard = ({ isArabic, title, body, actionLabel, actionTo, onActionPress }) => {
  return (
    <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
      <h3 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">{title}</h3>
      <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">{body}</p>
      <Button
        as={Link}
        to={actionTo}
        className="cc-button-text mt-4 w-full bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
        onPress={onActionPress}
      >
        {actionLabel || (isArabic ? "المتابعة" : "Continue")}
      </Button>
    </Card>
  );
};

export default NextActionCard;
