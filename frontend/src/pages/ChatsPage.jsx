import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { toast } from "react-toastify";
import Card from "../components/Card";
import ConversationPanel from "../components/chat/ConversationPanel";
import ConversationSidebar from "../components/chat/ConversationSidebar";
import { CHAT_CARD_BG } from "../components/chat/chatPageShared";
import ThemedLoader from "../components/ThemedLoader";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";
import {
  ensureConversation,
  getChatTargetsForUser,
  getConversationsForUser,
  getMessagesForThread,
  hideConversationForUser,
  markThreadAsRead,
  sendMessage,
} from "../services/chatService";
import { uploadPlayerProfileAsset } from "../services/playerService";

const ChatsPage = () => {
  const { user } = useAuth();
  const { isArabic } = useLanguage();
  const isAdmin = user?.role === "ADMIN";
  const [showAllAdminConversations, setShowAllAdminConversations] = useState(true);
  const [searchParams, setSearchParams] = useSearchParams();
  const isAdminViewingAllConversations = isAdmin && showAllAdminConversations;

  const requestedThreadId = searchParams.get("thread") || "";
  const requestedPartnerId = searchParams.get("with") || "";

  const [isLoadingConversations, setIsLoadingConversations] = useState(true);
  const [isLoadingMessages, setIsLoadingMessages] = useState(false);
  const [showConversationLoader, setShowConversationLoader] = useState(false);

  const [conversations, setConversations] = useState([]);
  const [activeThreadId, setActiveThreadId] = useState("");
  const [messages, setMessages] = useState([]);
  const [messageDraft, setMessageDraft] = useState("");
  const [selectedImageFile, setSelectedImageFile] = useState(null);
  const [isUploadingAttachment, setIsUploadingAttachment] = useState(false);
  const [isDeletingConversationById, setIsDeletingConversationById] = useState({});
  const [adminChatTargets, setAdminChatTargets] = useState([]);
  const [isLoadingAdminTargets, setIsLoadingAdminTargets] = useState(false);
  const [adminTargetSearch, setAdminTargetSearch] = useState("");
  const messageImageInputRef = useRef(null);

  const activeConversation = useMemo(
    () =>
      conversations.find((conversation) => String(conversation.id) === String(activeThreadId)) ||
      null,
    [activeThreadId, conversations],
  );

  const canSendInActiveConversation = useMemo(() => {
    if (!activeConversation || !Array.isArray(activeConversation.participantIds)) {
      return false;
    }

    return activeConversation.participantIds.some(
      (participantId) => String(participantId) === String(user.id),
    );
  }, [activeConversation, user.id]);

  const filteredAdminTargets = useMemo(() => {
    const normalizedQuery = adminTargetSearch.trim().toLowerCase();

    if (!normalizedQuery) {
      return adminChatTargets;
    }

    return adminChatTargets.filter((target) => {
      const searchableText = [target.fullName, target.username, target.organizationName]
        .filter(Boolean)
        .join(" ")
        .toLowerCase();

      return searchableText.includes(normalizedQuery);
    });
  }, [adminChatTargets, adminTargetSearch]);

  useEffect(() => {
    if (!isLoadingConversations) {
      setShowConversationLoader(false);
      return undefined;
    }

    const timeoutId = globalThis.setTimeout(() => {
      setShowConversationLoader(true);
    }, 220);

    return () => {
      globalThis.clearTimeout(timeoutId);
    };
  }, [isLoadingConversations]);

  const refreshConversations = useCallback(async () => {
    const nextConversations = await getConversationsForUser(user.id, {
      includeAllConversations: isAdminViewingAllConversations,
      forceRefresh: true,
    });
    setConversations(nextConversations);
    return nextConversations;
  }, [isAdminViewingAllConversations, user.id]);

  const loadMessages = useCallback(
    async (threadId) => {
      if (!threadId) {
        setMessages([]);
        return;
      }

      setIsLoadingMessages(true);

      try {
        const nextMessages = await getMessagesForThread({
          threadId,
          currentUserId: user.id,
          allowObserverAccess: isAdminViewingAllConversations,
        });
        setMessages(nextMessages);

        void (async () => {
          try {
            const readCount = await markThreadAsRead({ threadId, userId: user.id });
            if (readCount <= 0) {
              return;
            }

            setConversations((currentConversations) =>
              currentConversations.map((conversation) =>
                String(conversation.id) === String(threadId)
                  ? {
                      ...conversation,
                      unreadCount: 0,
                    }
                  : conversation,
              ),
            );

            const refreshedConversations = await refreshConversations();
            setConversations(refreshedConversations);
          } catch {
            // Keep conversation rendering fast even if read sync fails.
          }
        })();
      } catch (error) {
        const message =
          error instanceof Error
            ? error.message
            : isArabic
              ? "تعذر تحميل المحادثة"
              : "Unable to load conversation.";
        toast.error(message);
      } finally {
        setIsLoadingMessages(false);
      }
    },
    [isAdminViewingAllConversations, isArabic, refreshConversations, user.id],
  );

  useEffect(() => {
    let isCancelled = false;

    const loadAdminChatTargets = async () => {
      setIsLoadingAdminTargets(true);

      try {
        const targets = await getChatTargetsForUser({ currentUserId: user.id });
        if (!isCancelled) {
          setAdminChatTargets(targets);
        }
      } catch {
        if (!isCancelled) {
          setAdminChatTargets([]);
        }
      } finally {
        if (!isCancelled) {
          setIsLoadingAdminTargets(false);
        }
      }
    };

    loadAdminChatTargets();

    return () => {
      isCancelled = true;
    };
  }, [user.id]);

  useEffect(() => {
    let isCancelled = false;

    const initializeChat = async () => {
      setIsLoadingConversations(true);

      try {
        let selectedThreadId = requestedThreadId;

        if (requestedPartnerId && requestedPartnerId !== String(user.id)) {
          const thread = await ensureConversation({
            currentUserId: user.id,
            targetUserId: requestedPartnerId,
          });
          selectedThreadId = String(thread.id);
        }

        const nextConversations = await refreshConversations();
        if (isCancelled) {
          return;
        }

        if (!selectedThreadId && nextConversations.length > 0) {
          selectedThreadId = String(nextConversations[0].id);
        }

        setActiveThreadId(selectedThreadId || "");

        if (selectedThreadId) {
          await loadMessages(selectedThreadId);
        } else {
          setMessages([]);
        }

        if (selectedThreadId) {
          setSearchParams({ thread: selectedThreadId }, { replace: true });
        } else if (requestedThreadId || requestedPartnerId) {
          setSearchParams({}, { replace: true });
        }
      } catch (error) {
        const message =
          error instanceof Error
            ? error.message
            : isArabic
              ? "تعذر فتح المحادثة"
              : "Unable to open this conversation.";
        toast.error(message);
      } finally {
        if (!isCancelled) {
          setIsLoadingConversations(false);
        }
      }
    };

    initializeChat();

    return () => {
      isCancelled = true;
    };
  }, [
    isArabic,
    isAdmin,
    loadMessages,
    refreshConversations,
    requestedPartnerId,
    requestedThreadId,
    setSearchParams,
    user.id,
  ]);

  const openConversation = async (threadId) => {
    const normalizedThreadId = String(threadId);
    setActiveThreadId(normalizedThreadId);
    setSearchParams({ thread: normalizedThreadId }, { replace: true });
    await loadMessages(normalizedThreadId);
  };

  const onAdminScopeChange = (nextValue) => {
    setShowAllAdminConversations(nextValue);
    setActiveThreadId("");
    setMessages([]);
    setSearchParams({}, { replace: true });
  };

  const hideConversation = async (conversation) => {
    const threadId = String(conversation?.id || "");

    if (!threadId) {
      return;
    }

    setIsDeletingConversationById((current) => ({
      ...current,
      [threadId]: true,
    }));

    try {
      await hideConversationForUser({
        threadId,
        userId: user.id,
      });

      const refreshedConversations = await refreshConversations();
      const isRemovingActiveConversation = String(activeThreadId) === threadId;

      if (!isRemovingActiveConversation) {
        toast.success(isArabic ? "تم إخفاء المحادثة." : "Conversation hidden.");
        return;
      }

      const [nextConversation] = refreshedConversations;

      if (!nextConversation) {
        setActiveThreadId("");
        setMessages([]);
        setSearchParams({}, { replace: true });
        toast.success(isArabic ? "تم إخفاء المحادثة." : "Conversation hidden.");
        return;
      }

      const nextThreadId = String(nextConversation.id);
      setActiveThreadId(nextThreadId);
      setSearchParams({ thread: nextThreadId }, { replace: true });
      await loadMessages(nextThreadId);

      toast.success(isArabic ? "تم إخفاء المحادثة." : "Conversation hidden.");
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : isArabic
            ? "تعذر إخفاء المحادثة."
            : "Unable to hide conversation.";
      toast.error(message);
    } finally {
      setIsDeletingConversationById((current) => ({
        ...current,
        [threadId]: false,
      }));
    }
  };

  const startDirectConversation = (targetUserId) => {
    setSearchParams({ with: String(targetUserId) }, { replace: false });
  };

  const clearSelectedImage = useCallback(() => {
    setSelectedImageFile(null);

    if (messageImageInputRef.current) {
      messageImageInputRef.current.value = "";
    }
  }, []);

  const openImagePicker = () => {
    if (!canSendInActiveConversation) {
      return;
    }

    messageImageInputRef.current?.click();
  };

  const onImageFileChange = (event) => {
    const inputElement = event.target;
    const file = inputElement.files?.[0];

    if (!file) {
      setSelectedImageFile(null);
      return;
    }

    if (
      !String(file.type || "")
        .toLowerCase()
        .startsWith("image/")
    ) {
      toast.error(isArabic ? "يرجى اختيار صورة فقط." : "Please choose an image file.");
      inputElement.value = "";
      return;
    }

    setSelectedImageFile(file);
  };

  const submitMessage = async () => {
    const threadId = String(activeThreadId || "");
    const body = messageDraft.trim();

    if (!threadId || (!body && !selectedImageFile) || !canSendInActiveConversation) {
      return;
    }

    const optimisticMessageId = `temp-${Date.now()}`;

    try {
      let nextMessageBody = body;

      if (selectedImageFile) {
        setIsUploadingAttachment(true);

        const uploadResult = await uploadPlayerProfileAsset({
          file: selectedImageFile,
          assetType: "CHAT_IMAGE",
          threadId,
        });

        nextMessageBody = body ? `${body}\n${uploadResult.url}` : uploadResult.url;

        if (uploadResult.storage === "inline") {
          toast.info(
            isArabic
              ? "تم حفظ صورة الدردشة محليا لهذا الإصدار المؤقت."
              : "Chat image stored with local fallback for this environment.",
          );
        }
      }

      const optimisticCreatedAt = new Date().toISOString();
      const optimisticMessage = {
        id: optimisticMessageId,
        threadId,
        senderId: String(user.id),
        recipientId: "",
        body: nextMessageBody,
        createdAt: optimisticCreatedAt,
        readAt: null,
      };

      setMessages((currentMessages) => [...currentMessages, optimisticMessage]);
      setConversations((currentConversations) => {
        const updatedConversations = currentConversations.map((conversation) => {
          if (String(conversation.id) !== threadId) {
            return conversation;
          }

          return {
            ...conversation,
            lastMessagePreview: nextMessageBody.slice(0, 140),
            lastMessageAt: optimisticCreatedAt,
          };
        });

        return [...updatedConversations].sort(
          (left, right) =>
            new Date(right.lastMessageAt || 0).getTime() -
            new Date(left.lastMessageAt || 0).getTime(),
        );
      });

      setMessageDraft("");
      clearSelectedImage();

      void (async () => {
        try {
          const createdMessage = await sendMessage({
            threadId,
            senderId: user.id,
            body: nextMessageBody,
          });

          setMessages((currentMessages) =>
            currentMessages.map((message) =>
              String(message.id) === optimisticMessageId ? createdMessage : message,
            ),
          );

          void refreshConversations();
        } catch (error) {
          setMessages((currentMessages) =>
            currentMessages.filter((message) => String(message.id) !== optimisticMessageId),
          );
          void refreshConversations();

          const message =
            error instanceof Error
              ? error.message
              : isArabic
                ? "تعذر إرسال الرسالة"
                : "Unable to send message.";
          toast.error(message);
        }
      })();
    } catch (error) {
      setMessages((currentMessages) =>
        currentMessages.filter((message) => String(message.id) !== optimisticMessageId),
      );
      void refreshConversations();

      const message =
        error instanceof Error
          ? error.message
          : isArabic
            ? "تعذر إرسال الرسالة"
            : "Unable to send message.";
      toast.error(message);
    } finally {
      setIsUploadingAttachment(false);
    }
  };

  const onComposerKeyDown = async (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      await submitMessage();
    }
  };

  if (isLoadingConversations) {
    return (
      <section className="bg-transparent px-4 py-10">
        <div className="m-auto max-w-6xl py-16">
          {showConversationLoader ? <ThemedLoader /> : <div className="min-h-[4rem]" />}
        </div>
      </section>
    );
  }

  return (
    <section className="bg-transparent px-4 py-10">
      <div className="m-auto max-w-6xl">
        <Card bg={CHAT_CARD_BG} className="mb-6 shadow-xl backdrop-blur-sm">
          <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "الرسائل" : "Messages"}
          </h1>
          <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic
              ? "تواصل مباشرة مع اللاعبين والمستقطبين حسب صلاحيات الحساب."
              : "Directly chat with players and scouts based on account permissions."}
          </p>
        </Card>

        <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
          <ConversationSidebar
            activeThreadId={activeThreadId}
            adminTargetSearch={adminTargetSearch}
            conversations={conversations}
            filteredAdminTargets={filteredAdminTargets}
            isAdmin={isAdmin}
            isAdminViewingAllConversations={isAdminViewingAllConversations}
            isArabic={isArabic}
            isDeletingConversationById={isDeletingConversationById}
            isLoadingAdminTargets={isLoadingAdminTargets}
            onAdminScopeChange={onAdminScopeChange}
            onAdminTargetSearchChange={setAdminTargetSearch}
            onClearAdminTargetSearch={() => setAdminTargetSearch("")}
            onHideConversation={hideConversation}
            onOpenConversation={openConversation}
            onStartDirectConversation={startDirectConversation}
            showAllAdminConversations={showAllAdminConversations}
            user={user}
          />

          <ConversationPanel
            activeConversation={activeConversation}
            canSendInActiveConversation={canSendInActiveConversation}
            isAdminViewingAllConversations={isAdminViewingAllConversations}
            isArabic={isArabic}
            isLoadingMessages={isLoadingMessages}
            isUploadingAttachment={isUploadingAttachment}
            messageDraft={messageDraft}
            messageImageInputRef={messageImageInputRef}
            messages={messages}
            onComposerKeyDown={onComposerKeyDown}
            onImageFileChange={onImageFileChange}
            onMessageDraftChange={setMessageDraft}
            onOpenImagePicker={openImagePicker}
            onRemoveSelectedImage={clearSelectedImage}
            onStartDirectConversation={startDirectConversation}
            onSubmitMessage={submitMessage}
            selectedImageFile={selectedImageFile}
            user={user}
          />
        </div>
      </div>
    </section>
  );
};

export default ChatsPage;
