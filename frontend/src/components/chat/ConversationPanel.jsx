import { Button, Textarea } from "@heroui/react";
import Card from "../Card";
import ThemedLoader from "../ThemedLoader";
import { formatTime, getMessageDisplayParts } from "../../utils/chatFormatters";
import {
  CHAT_CARD_BG,
  PRIMARY_BUTTON_CLASS,
  getRoleLabel,
} from "./chatPageShared";

const ConversationPanel = ({
  activeConversation,
  canSendInActiveConversation,
  isAdminViewingAllConversations,
  isArabic,
  isLoadingMessages,
  isUploadingAttachment,
  messageDraft,
  messageImageInputRef,
  messages,
  onComposerKeyDown,
  onImageFileChange,
  onMessageDraftChange,
  onOpenImagePicker,
  onRemoveSelectedImage,
  onStartDirectConversation,
  onSubmitMessage,
  selectedImageFile,
  user,
}) => {
  return (
    <Card bg={CHAT_CARD_BG} className="shadow-xl backdrop-blur-sm lg:col-span-2">
      {activeConversation ? (
        <>
          <div className="mb-4 border-b border-[#3c4748]/30 pb-3">
            <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
              {isAdminViewingAllConversations
                ? activeConversation.displayName || (isArabic ? "محادثة" : "Conversation")
                : activeConversation.partnerName}
            </h2>
            <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/80">
              {isAdminViewingAllConversations
                ? Array.isArray(activeConversation.participantRoles)
                  ? activeConversation.participantRoles
                      .map((role) => getRoleLabel(role, isArabic))
                      .join(" / ")
                  : ""
                : getRoleLabel(activeConversation.partnerRole, isArabic)}
            </p>
            {isAdminViewingAllConversations && !canSendInActiveConversation ? (
              <p className="mt-2 text-xs text-[#273b40]/80 dark:text-[#cae9ea]/75">
                {isArabic
                  ? "أنت في وضع المشاهدة لهذه المحادثة. ابدأ محادثة مباشرة مع أي طرف لإرسال رسالة."
                  : "You are viewing this conversation in observer mode. Start a direct chat with a participant to send messages."}
              </p>
            ) : null}
            {isAdminViewingAllConversations &&
            !canSendInActiveConversation &&
            Array.isArray(activeConversation.participantIds) ? (
              <div className="mt-3 flex flex-wrap gap-2">
                {activeConversation.participantIds.map((participantId, index) => (
                  <Button
                    key={String(participantId)}
                    size="sm"
                    variant="bordered"
                    className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]"
                    onPress={() => onStartDirectConversation(participantId)}
                  >
                    {isArabic
                      ? `مراسلة ${activeConversation.participantNames?.[index] || "المستخدم"}`
                      : `Message ${activeConversation.participantNames?.[index] || "User"}`}
                  </Button>
                ))}
              </div>
            ) : null}
          </div>

          <div className="max-h-[24rem] space-y-2 overflow-y-auto rounded-md border border-[#3c4748]/30 bg-white/55 p-3 dark:bg-[#1d1d1d]/35">
            {isLoadingMessages ? (
              <div className="py-8">
                <ThemedLoader />
              </div>
            ) : messages.length === 0 ? (
              <p className="text-sm text-[#273b40]/80 dark:text-[#cae9ea]/75">
                {isArabic
                  ? "ابدأ المحادثة بأول رسالة."
                  : "Start the conversation with your first message."}
              </p>
            ) : (
              messages.map((message) => {
                const isMine = String(message.senderId) === String(user.id);
                const { text: messageText, imageUrl } = getMessageDisplayParts(message.body);

                return (
                  <div key={message.id} className={`flex ${isMine ? "justify-end" : "justify-start"}`}>
                    <div
                      className={`max-w-[80%] rounded-md px-3 py-2 text-sm ${
                        isMine
                          ? "bg-[#208c8c] text-[#cae9ea]"
                          : "bg-[#e6f5f6] text-[#273b40] dark:bg-[#334a4f] dark:text-[#cae9ea]"
                      }`}
                    >
                      {messageText ? <p className="whitespace-pre-wrap">{messageText}</p> : null}
                      {imageUrl ? (
                        <a href={imageUrl} target="_blank" rel="noreferrer" className="mt-2 block">
                          <img
                            src={imageUrl}
                            alt={isArabic ? "صورة دردشة" : "Chat attachment"}
                            className="max-h-64 w-auto max-w-full rounded-md border border-[#3c4748]/25 object-contain"
                            loading="lazy"
                          />
                        </a>
                      ) : null}
                      {!messageText && !imageUrl ? <p>{String(message.body || "")}</p> : null}
                      <p
                        className={`mt-1 text-[10px] ${
                          isMine ? "text-[#cae9ea]/80" : "text-[#273b40]/65 dark:text-[#cae9ea]/65"
                        }`}
                      >
                        {formatTime(message.createdAt)}
                      </p>
                    </div>
                  </div>
                );
              })
            )}
          </div>

          <div className="mt-3 space-y-2">
            <input
              ref={messageImageInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onChange={onImageFileChange}
            />
            {selectedImageFile ? (
              <div className="flex items-center justify-between gap-3 rounded-md border border-[#3c4748]/25 bg-white/45 px-3 py-2 text-xs text-[#273b40] dark:bg-[#1d1d1d]/35 dark:text-[#cae9ea]/85">
                <span className="truncate">
                  {isArabic ? "صورة جاهزة للإرسال:" : "Image ready to send:"} {selectedImageFile.name}
                </span>
                <Button
                  type="button"
                  size="sm"
                  variant="light"
                  className="cc-button-text text-[#273b40] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
                  onPress={onRemoveSelectedImage}
                  isDisabled={isUploadingAttachment}
                >
                  {isArabic ? "إزالة" : "Remove"}
                </Button>
              </div>
            ) : null}
            <div className="flex gap-2">
              <Button
                type="button"
                variant="bordered"
                onPress={onOpenImagePicker}
                isDisabled={!canSendInActiveConversation || isUploadingAttachment}
                className="cc-button-text self-end border-[#3c4748]/45 text-[#273b40] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
              >
                {isArabic ? "إضافة صورة" : "Add Image"}
              </Button>
              <Textarea
                minRows={2}
                value={messageDraft}
                onValueChange={onMessageDraftChange}
                onKeyDown={onComposerKeyDown}
                placeholder={isArabic ? "اكتب رسالة..." : "Type a message..."}
                className="w-full"
                isDisabled={!canSendInActiveConversation || isUploadingAttachment}
              />
              <Button
                type="button"
                isLoading={isUploadingAttachment}
                isDisabled={
                  isUploadingAttachment ||
                  (!messageDraft.trim() && !selectedImageFile) ||
                  !canSendInActiveConversation
                }
                onPress={onSubmitMessage}
                className={`${PRIMARY_BUTTON_CLASS} self-end`}
              >
                {isArabic ? "إرسال" : "Send"}
              </Button>
            </div>
          </div>
        </>
      ) : (
        <div className="py-8">
          <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic
              ? "اختر محادثة من القائمة لعرض الرسائل."
              : "Select a conversation to view messages."}
          </p>
        </div>
      )}
    </Card>
  );
};

export default ConversationPanel;
