import ChatPreviewCard from "./ChatPreviewCard";

const PlayerChatPreviewCard = ({ isArabic, conversations, onOpenThread }) => {
  return (
    <ChatPreviewCard
      title={isArabic ? "آخر المحادثات" : "Latest Chats"}
      subtitle={
        isArabic
          ? "آخر 3 محادثات مع مؤشر الرسائل غير المقروءة."
          : "Your latest 3 conversations with unread indicators."
      }
      conversations={conversations}
      isArabic={isArabic}
      onOpenThread={onOpenThread}
      openAllLabel={isArabic ? "عرض كل المحادثات" : "View All Chats"}
    />
  );
};

export default PlayerChatPreviewCard;
