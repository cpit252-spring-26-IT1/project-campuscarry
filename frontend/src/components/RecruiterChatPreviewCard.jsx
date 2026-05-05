import ChatPreviewCard from "./ChatPreviewCard";

const RecruiterChatPreviewCard = ({ isArabic, conversations, onOpenThread }) => {
  return (
    <ChatPreviewCard
      title={isArabic ? "آخر المحادثات" : "Latest Chats"}
      subtitle={isArabic ? "آخر 3 محادثات مع اللاعبين." : "Your latest 3 player conversations."}
      conversations={conversations}
      isArabic={isArabic}
      onOpenThread={onOpenThread}
      openAllLabel={isArabic ? "عرض كل المحادثات" : "View All Chats"}
    />
  );
};

export default RecruiterChatPreviewCard;
