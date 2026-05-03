import { Button, Input, Switch } from "@heroui/react";
import { Link } from "react-router-dom";
import Card from "../Card";
import ThemedLoader from "../ThemedLoader";
import {
  CHAT_CARD_BG,
  MUTED_TEXT_CLASS,
  PRIMARY_BUTTON_CLASS,
  getRoleLabel,
} from "./chatPageShared";
import { getRecruiterDmOpennessLabel } from "../../services/recruiterService";

const ConversationSidebar = ({
  activeThreadId,
  adminTargetSearch,
  conversations,
  filteredAdminTargets,
  isAdmin,
  isAdminViewingAllConversations,
  isArabic,
  isDeletingConversationById,
  isLoadingAdminTargets,
  onAdminScopeChange,
  onAdminTargetSearchChange,
  onClearAdminTargetSearch,
  onHideConversation,
  onOpenConversation,
  onStartDirectConversation,
  showAllAdminConversations,
  user,
}) => {
  const emptyConversationLink =
    user.role === "RECRUITER"
      ? {
          to: "/players",
          label: isArabic ? "تصفح اللاعبين وابدأ المحادثة" : "Browse Players to Start Chat",
        }
      : {
          to: "/leaderboard",
          label: isArabic
            ? user.role === "ADMIN"
              ? "عرض المتصدرين"
              : "تصفح اللاعبين"
            : user.role === "ADMIN"
              ? "Open Leaderboard"
              : "Browse Players",
        };

  const searchPlaceholder = isAdmin
    ? isArabic
      ? "ابحث عن لاعب أو مستقطب"
      : "Search for a player or scout"
    : user.role === "PLAYER"
      ? isArabic
        ? "ابحث عن مستقطب متاح"
        : "Search open recruiters"
      : isArabic
        ? "ابحث عن لاعب"
        : "Search players";

  const emptyTargetMessage = isAdmin
    ? isArabic
      ? "لا توجد نتائج."
      : "No matching users found."
    : user.role === "PLAYER"
      ? isArabic
        ? "لا يوجد مستقطبون مؤهلون حاليا."
        : "No eligible recruiters available right now."
      : isArabic
        ? "لا يوجد لاعبون مطابقون حاليا."
        : "No matching players found right now.";

  return (
    <Card bg={CHAT_CARD_BG} className="shadow-xl backdrop-blur-sm lg:col-span-1">
      <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
        {isArabic ? "المحادثات" : "Conversations"}
      </h2>

      {isAdmin ? (
        <div className="mt-3 border-b border-[#3c4748]/25 pb-3">
          <Switch isSelected={showAllAdminConversations} onValueChange={onAdminScopeChange}>
            {isArabic ? "عرض كل المحادثات" : "Show All Conversations"}
          </Switch>
          <p className={`mt-1 text-xs ${MUTED_TEXT_CLASS}`}>
            {showAllAdminConversations
              ? isArabic
                ? "وضع الإشراف: عرض جميع محادثات اللاعبين والمستقطبين."
                : "Moderation mode: view all player and scout conversations."
              : isArabic
                ? "وضع المحادثات الخاصة بك فقط."
                : "Personal mode: only your conversations."}
          </p>
        </div>
      ) : null}

      {conversations.length === 0 ? (
        <div className="mt-3 space-y-3">
          <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic ? "لا توجد محادثات حتى الآن." : "No conversations yet."}
          </p>
          <Button as={Link} to={emptyConversationLink.to} className={PRIMARY_BUTTON_CLASS}>
            {emptyConversationLink.label}
          </Button>
        </div>
      ) : (
        <div className="mt-3 space-y-2">
          {conversations.map((conversation) => {
            const isActive = String(conversation.id) === String(activeThreadId);
            const conversationTitle = isAdminViewingAllConversations
              ? conversation.displayName || (isArabic ? "محادثة" : "Conversation")
              : conversation.partnerName;
            const roleLabel = isAdminViewingAllConversations
              ? Array.isArray(conversation.participantRoles)
                ? conversation.participantRoles.map((role) => getRoleLabel(role, isArabic)).join(" / ")
                : ""
              : getRoleLabel(conversation.partnerRole, isArabic);
            const isDeletingConversation = Boolean(
              isDeletingConversationById[String(conversation.id)],
            );

            return (
              <div
                key={conversation.id}
                className={`w-full rounded-md border px-3 py-2 text-left transition-colors ${
                  isActive
                    ? "border-[#208c8c]/70 bg-[#208c8c]/15"
                    : "border-[#3c4748]/35 bg-transparent hover:bg-[#cae9ea]/35 dark:hover:bg-[#1d1d1d]/35"
                }`}
              >
                <button
                  type="button"
                  onClick={() => onOpenConversation(conversation.id)}
                  className="w-full text-left"
                >
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-sm font-semibold text-[#1d1d1d] dark:text-[#cae9ea]">
                      {conversationTitle}
                    </p>
                    {conversation.unreadCount > 0 ? (
                      <span className="rounded-full bg-red-600 px-2 py-0.5 text-xs font-semibold text-white">
                        {conversation.unreadCount}
                      </span>
                    ) : null}
                  </div>
                  <p className={`text-xs ${MUTED_TEXT_CLASS}`}>{roleLabel}</p>
                  {conversation.lastMessagePreview ? (
                    <p className="mt-1 line-clamp-2 text-xs text-[#273b40]/75 dark:text-[#cae9ea]/70">
                      {conversation.lastMessagePreview}
                    </p>
                  ) : null}
                </button>

                <div className="mt-2 flex justify-end border-t border-[#3c4748]/20 pt-2">
                  <Button
                    size="sm"
                    variant="light"
                    isLoading={isDeletingConversation}
                    isDisabled={isDeletingConversation}
                    onPress={() => onHideConversation(conversation)}
                    className="cc-button-text text-[#273b40] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
                  >
                    {isArabic ? "إخفاء المحادثة" : "Hide Conversation"}
                  </Button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {isAdmin || user.role === "PLAYER" || user.role === "RECRUITER" ? (
        <div className="mt-4 border-t border-[#3c4748]/25 pt-4">
          <h3 className="text-sm font-semibold text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "ابدأ محادثة مباشرة" : "Start Direct Chat"}
          </h3>
          <Input
            type="search"
            className="mt-3"
            value={adminTargetSearch}
            onValueChange={onAdminTargetSearchChange}
            placeholder={searchPlaceholder}
            isClearable
            onClear={onClearAdminTargetSearch}
          />

          {isLoadingAdminTargets ? (
            <div className="py-4">
              <ThemedLoader size={24} showLabel={false} />
            </div>
          ) : filteredAdminTargets.length === 0 ? (
            <p className={`mt-3 text-xs ${MUTED_TEXT_CLASS}`}>{emptyTargetMessage}</p>
          ) : (
            <div className="mt-3 max-h-52 space-y-2 overflow-y-auto">
              {filteredAdminTargets.map((target) => (
                <button
                  key={target.id}
                  type="button"
                  className="w-full rounded-md border border-[#3c4748]/35 px-3 py-2 text-left transition-colors hover:bg-[#cae9ea]/35 dark:hover:bg-[#1d1d1d]/35"
                  onClick={() => onStartDirectConversation(target.id)}
                >
                  <p className="text-sm font-semibold text-[#1d1d1d] dark:text-[#cae9ea]">
                    {target.fullName}
                  </p>
                  <p className={`text-xs ${MUTED_TEXT_CLASS}`}>
                    {getRoleLabel(target.role, isArabic)}
                    {target.role === "RECRUITER" && target.organizationName
                      ? ` • ${target.organizationName}`
                      : ""}
                  </p>
                  {target.role === "RECRUITER" ? (
                    <p className="mt-1 text-[11px] text-[#273b40]/70 dark:text-[#cae9ea]/70">
                      {getRecruiterDmOpennessLabel({
                        recruiterDmOpenness: target.recruiterDmOpenness,
                        isArabic,
                      })}
                    </p>
                  ) : null}
                </button>
              ))}
            </div>
          )}
        </div>
      ) : null}
    </Card>
  );
};

export default ConversationSidebar;
