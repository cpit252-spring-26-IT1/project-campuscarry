import { Button } from "@heroui/react";
import { Link } from "react-router-dom";
import Card from "./Card";
import { formatTime } from "../utils/chatFormatters";

const ChatPreviewCard = ({
  title,
  subtitle,
  conversations,
  isArabic,
  onOpenThread,
  openAllLabel,
}) => {
  const hasConversations = Array.isArray(conversations) && conversations.length > 0;

  return (
    <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
      <h3 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">{title}</h3>
      {subtitle ? (
        <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">{subtitle}</p>
      ) : null}

      {hasConversations ? (
        <div className="mt-4 space-y-2">
          {conversations.slice(0, 3).map((conversation) => (
            <button
              key={String(conversation.id)}
              type="button"
              onClick={() => onOpenThread(String(conversation.id))}
              className="w-full rounded-md border border-[#3c4748]/30 px-3 py-2 text-left transition-colors hover:bg-[#cae9ea]/35 dark:hover:bg-[#1d1d1d]/35"
            >
              <div className="flex items-center justify-between gap-2">
                <p className="text-sm font-semibold text-[#1d1d1d] dark:text-[#cae9ea]">
                  {conversation.displayName ||
                    conversation.partnerName ||
                    (isArabic ? "محادثة" : "Conversation")}
                </p>
                {conversation.unreadCount > 0 ? (
                  <span className="rounded-full bg-red-600 px-2 py-0.5 text-xs font-semibold text-white">
                    {conversation.unreadCount}
                  </span>
                ) : null}
              </div>
              {conversation.lastMessagePreview ? (
                <p className="mt-1 line-clamp-2 text-xs text-[#273b40]/80 dark:text-[#cae9ea]/75">
                  {conversation.lastMessagePreview}
                </p>
              ) : null}
              <p className="mt-1 text-[11px] text-[#273b40]/65 dark:text-[#cae9ea]/65">
                {formatTime(conversation.lastMessageAt)}
              </p>
            </button>
          ))}
        </div>
      ) : (
        <p className="cc-body-muted mt-4 text-[#273b40] dark:text-[#cae9ea]/85">
          {isArabic ? "لا توجد محادثات حديثة." : "No recent conversations yet."}
        </p>
      )}

      <Button
        as={Link}
        to="/chats"
        className="cc-button-text mt-4 w-full bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
      >
        {openAllLabel || (isArabic ? "فتح المحادثات" : "Open Chats")}
      </Button>
    </Card>
  );
};

export default ChatPreviewCard;
