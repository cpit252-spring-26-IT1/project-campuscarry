const getRoleLabel = (role, isArabic) => {
  if (role === "RECRUITER") {
    return isArabic ? "مستقطب" : "Scout";
  }

  if (role === "PLAYER") {
    return isArabic ? "لاعب" : "Player";
  }

  if (role === "ADMIN") {
    return isArabic ? "مشرف" : "Admin";
  }

  return isArabic ? "غير معروف" : "Unknown";
};

const CHAT_CARD_BG = "bg-[#f4fbfb]/88 dark:bg-[#273b40]/58";
const PRIMARY_BUTTON_CLASS = "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]";
const MUTED_TEXT_CLASS = "text-[#273b40]/80 dark:text-[#cae9ea]/75";

export { CHAT_CARD_BG, MUTED_TEXT_CLASS, PRIMARY_BUTTON_CLASS, getRoleLabel };
