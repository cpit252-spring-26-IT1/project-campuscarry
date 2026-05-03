import api from "./httpClient";
import { normalizeRecruiterDmOpenness, canPlayerInitiateRecruiterChat } from "./recruiterService";
import { ROLE_ADMIN, ROLE_PLAYER, ROLE_RECRUITER } from "./chat/constants";
import {
  canInitiateConversation,
  findProfileByUserId,
  findThreadById,
  findThreadsByParticipants,
  getGroupedThreadIdsByPairKey,
  getHiddenConversationEntriesForUser,
  getInitiationErrorMessage,
  getLatestThread,
  getParticipantPairKey,
  getThreadParticipantIds,
  getThreadParticipantKey,
  hideConversationPairForUser,
  isApprovedPlayerProfile,
  normalizeText,
  normalizeUniqueUserIds,
  normalizeUserId,
  toTimestamp,
  unhideConversationPairForUser,
} from "./chat/helpers";
import { getProfilesByUserIds, getUsersAndProfiles, getUsersByIds, safeGetCollection } from "./chat/lookups";
import { invalidateChatCollections } from "./chat/state";

const ensureConversation = async ({ currentUserId, targetUserId }) => {
  const normalizedCurrentUserId = normalizeUserId(currentUserId);
  const normalizedTargetUserId = normalizeUserId(targetUserId);

  if (normalizedCurrentUserId === normalizedTargetUserId) {
    throw new Error("You cannot start a chat with yourself.");
  }

  const [threads, users] = await Promise.all([
    safeGetCollection("/chat_threads", undefined, { forceRefresh: true }),
    getUsersByIds([normalizedCurrentUserId, normalizedTargetUserId], { forceRefresh: true }),
  ]);

  const initiator = users.find((user) => normalizeUserId(user.id) === normalizedCurrentUserId);
  const target = users.find((user) => normalizeUserId(user.id) === normalizedTargetUserId);

  if (!initiator || !target) {
    throw new Error("Unable to start this chat right now.");
  }

  const existingThread = getLatestThread(
    findThreadsByParticipants(threads, normalizedCurrentUserId, normalizedTargetUserId),
  );
  const participantKey = getParticipantPairKey(normalizedCurrentUserId, normalizedTargetUserId);

  if (existingThread) {
    unhideConversationPairForUser(normalizedCurrentUserId, participantKey);
    return existingThread;
  }

  const [targetProfiles, initiatorProfiles] = await Promise.all([
    target.role === ROLE_PLAYER
      ? getProfilesByUserIds([normalizedTargetUserId])
      : Promise.resolve([]),
    initiator.role === ROLE_PLAYER
      ? getProfilesByUserIds([normalizedCurrentUserId])
      : Promise.resolve([]),
  ]);

  const targetProfile = findProfileByUserId(targetProfiles, normalizedTargetUserId);
  const targetAllowPlayerChats = targetProfile?.allowPlayerChats;
  const targetRecruiterDmOpenness = normalizeRecruiterDmOpenness(
    target?.recruiterDmOpenness || target?.dmOpenness,
  );
  const initiatorProfile = findProfileByUserId(initiatorProfiles, normalizedCurrentUserId);
  const initiatorIsVerifiedPlayer = isApprovedPlayerProfile(initiatorProfile);

  const normalizedInitiator = {
    ...initiator,
    isPlayerVerified: initiatorIsVerifiedPlayer,
  };

  const normalizedTarget = {
    ...target,
    recruiterDmOpenness: targetRecruiterDmOpenness,
  };

  if (
    !canInitiateConversation({
      initiator: normalizedInitiator,
      target: normalizedTarget,
      targetAllowPlayerChats,
    })
  ) {
    throw new Error(
      getInitiationErrorMessage({
        initiator,
        target,
        targetAllowPlayerChats,
        targetRecruiterDmOpenness,
        initiatorIsVerifiedPlayer,
      }),
    );
  }

  const now = new Date().toISOString();
  const participantIds = [normalizedCurrentUserId, normalizedTargetUserId].sort((a, b) =>
    a.localeCompare(b),
  );

  const { data: createdThread } = await api.post("/chat_threads", {
    participantIds,
    initiatedById: normalizedCurrentUserId,
    initiatedByRole: initiator.role,
    lastSenderId: null,
    lastMessageAt: null,
    lastMessagePreview: "",
    createdAt: now,
    updatedAt: now,
  });

  unhideConversationPairForUser(normalizedCurrentUserId, participantKey);
  invalidateChatCollections();

  return createdThread;
};

const getConversationsForUser = async (userId, options = {}) => {
  const normalizedUserId = normalizeUserId(userId);
  const includeAllConversations = Boolean(options.includeAllConversations);
  const forceRefresh = Boolean(options.forceRefresh);
  const hiddenConversationEntries = getHiddenConversationEntriesForUser(normalizedUserId);

  const [threads, messages] = await Promise.all([
    safeGetCollection("/chat_threads", undefined, { forceRefresh }),
    safeGetCollection("/chat_messages", undefined, { forceRefresh }),
  ]);

  const relevantUserIds = normalizeUniqueUserIds(
    threads.flatMap((thread) => getThreadParticipantIds(thread)),
  );
  const users = await getUsersByIds(relevantUserIds, { forceRefresh });
  const usersById = new Map(users.map((user) => [normalizeUserId(user.id), user]));

  const threadsForUser = includeAllConversations
    ? threads.filter((thread) => getThreadParticipantIds(thread).length === 2)
    : threads.filter((thread) => getThreadParticipantIds(thread).includes(normalizedUserId));

  const groupedByPairKey = new Map();
  threadsForUser.forEach((thread) => {
    const pairKey = getThreadParticipantKey(thread);
    if (!pairKey) {
      return;
    }

    const currentGroup = groupedByPairKey.get(pairKey) || [];
    currentGroup.push(thread);
    groupedByPairKey.set(pairKey, currentGroup);
  });

  return Array.from(groupedByPairKey.values())
    .map((groupedThreads) => {
      const canonicalThread = getLatestThread(groupedThreads);
      const conversationKey = getThreadParticipantKey(canonicalThread);
      const participantIds = getThreadParticipantIds(canonicalThread);
      const participantUsers = participantIds.map((participantId) => usersById.get(participantId));
      const participantNames = participantUsers.map(
        (participantUser) => participantUser?.fullName || participantUser?.username || "Unknown",
      );
      const participantRoles = participantUsers.map((participantUser) => participantUser?.role || "");
      const partnerId =
        participantIds.find((participantId) => participantId !== normalizedUserId) || "";
      const partner = usersById.get(partnerId);
      const groupedThreadIds = new Set(groupedThreads.map((thread) => String(thread.id)));
      const groupedMessages = messages.filter((message) =>
        groupedThreadIds.has(String(message.threadId)),
      );

      const unreadCount = groupedMessages.filter(
        (message) => normalizeUserId(message.recipientId) === normalizedUserId && !message.readAt,
      ).length;

      const latestMessage = [...groupedMessages].sort(
        (a, b) => toTimestamp(b.createdAt) - toTimestamp(a.createdAt),
      )[0];

      const displayName = includeAllConversations
        ? participantNames.join(" ↔ ")
        : partner?.fullName || partner?.username || "Unknown";
      const displayRole = includeAllConversations
        ? participantRoles.filter(Boolean).join(" / ")
        : partner?.role || "";

      return {
        id: canonicalThread.id,
        conversationKey,
        participantIds,
        participantNames,
        participantRoles,
        partnerId,
        partnerName: partner?.fullName || partner?.username || "Unknown",
        partnerRole: partner?.role || "",
        displayName,
        displayRole,
        isObserver: includeAllConversations && !participantIds.includes(normalizedUserId),
        lastMessagePreview: latestMessage?.body || canonicalThread.lastMessagePreview || "",
        lastMessageAt:
          latestMessage?.createdAt ||
          canonicalThread.lastMessageAt ||
          canonicalThread.updatedAt ||
          canonicalThread.createdAt,
        unreadCount,
      };
    })
    .filter((conversation) => {
      const hiddenAt = hiddenConversationEntries[conversation.conversationKey];
      return !hiddenAt || toTimestamp(conversation.lastMessageAt) > toTimestamp(hiddenAt);
    })
    .sort((a, b) => toTimestamp(b.lastMessageAt) - toTimestamp(a.lastMessageAt));
};

const getMessagesForThread = async ({ threadId, currentUserId, allowObserverAccess = false }) => {
  const normalizedCurrentUserId = normalizeUserId(currentUserId);

  const [threads, messages] = await Promise.all([
    safeGetCollection("/chat_threads"),
    safeGetCollection("/chat_messages"),
  ]);

  const thread = findThreadById(threads, threadId);
  if (!thread) {
    throw new Error("Conversation not found.");
  }

  const participantIds = getThreadParticipantIds(thread);

  if (!participantIds.includes(normalizedCurrentUserId) && !allowObserverAccess) {
    throw new Error("You do not have access to this conversation.");
  }

  const pairKey = getThreadParticipantKey(thread);
  const groupedThreadIds = getGroupedThreadIdsByPairKey(threads, pairKey);

  return messages
    .filter((message) => groupedThreadIds.has(String(message.threadId)))
    .sort((a, b) => toTimestamp(a.createdAt) - toTimestamp(b.createdAt));
};

const markThreadAsRead = async ({ threadId, userId }) => {
  const normalizedUserId = normalizeUserId(userId);
  const [threads, messages] = await Promise.all([
    safeGetCollection("/chat_threads"),
    safeGetCollection("/chat_messages"),
  ]);

  const thread = findThreadById(threads, threadId);
  if (!thread) {
    return 0;
  }

  const pairKey = getThreadParticipantKey(thread);
  const groupedThreadIds = getGroupedThreadIdsByPairKey(threads, pairKey);

  const unreadMessages = messages.filter(
    (message) =>
      groupedThreadIds.has(String(message.threadId)) &&
      normalizeUserId(message.recipientId) === normalizedUserId &&
      !message.readAt,
  );

  if (unreadMessages.length === 0) {
    return 0;
  }

  const readAt = new Date().toISOString();
  await Promise.all(
    unreadMessages.map((message) =>
      api.patch(`/chat_messages/${message.id}`, {
        readAt,
      }),
    ),
  );

  invalidateChatCollections();
  return unreadMessages.length;
};

const sendMessage = async ({ threadId, senderId, body }) => {
  const messageBody = normalizeText(body);
  if (!messageBody) {
    throw new Error("Message cannot be empty.");
  }

  const normalizedSenderId = normalizeUserId(senderId);
  const threads = await safeGetCollection("/chat_threads");
  const thread = findThreadById(threads, threadId);
  if (!thread) {
    throw new Error("Conversation not found.");
  }

  const participantIds = getThreadParticipantIds(thread);
  if (!participantIds.includes(normalizedSenderId)) {
    throw new Error("You do not have access to this conversation.");
  }

  const pairKey = getThreadParticipantKey(thread);
  if (pairKey) {
    unhideConversationPairForUser(normalizedSenderId, pairKey);
  }

  const canonicalThread =
    getLatestThread(
      threads.filter((candidate) => getThreadParticipantKey(candidate) === pairKey),
    ) || thread;

  const recipientId = participantIds.find((participantId) => participantId !== normalizedSenderId);
  if (!recipientId) {
    throw new Error("Conversation participant is invalid.");
  }

  const [users, profiles] = await Promise.all([
    getUsersByIds([normalizedSenderId, recipientId]),
    getProfilesByUserIds([recipientId]),
  ]);

  const sender = users.find((user) => normalizeUserId(user.id) === normalizedSenderId);
  const recipient = users.find((user) => normalizeUserId(user.id) === recipientId);
  if (!sender || !recipient) {
    throw new Error("Unable to send message right now.");
  }

  if (sender.role === ROLE_PLAYER && recipient.role === ROLE_PLAYER) {
    const recipientProfile = findProfileByUserId(profiles, recipientId);
    if (recipientProfile?.allowPlayerChats === false) {
      throw new Error("This player only accepts chats from scouts.");
    }
  }

  const now = new Date().toISOString();
  const { data: createdMessage } = await api.post("/chat_messages", {
    threadId: canonicalThread.id,
    senderId: normalizedSenderId,
    recipientId,
    body: messageBody,
    createdAt: now,
    readAt: null,
  });

  invalidateChatCollections();
  return createdMessage;
};

const hideConversationForUser = async ({ threadId, userId }) => {
  const normalizedUserId = normalizeUserId(userId);

  const [users, threads] = await Promise.all([
    getUsersByIds([normalizedUserId]),
    safeGetCollection("/chat_threads"),
  ]);

  const thread = findThreadById(threads, threadId);
  if (!thread) {
    throw new Error("Conversation not found.");
  }

  const conversationKey = getThreadParticipantKey(thread);
  if (!conversationKey) {
    throw new Error("Conversation is invalid.");
  }

  const actor = users.find((candidate) => normalizeUserId(candidate.id) === normalizedUserId);
  if (!actor) {
    throw new Error("Unable to hide this conversation right now.");
  }

  const participantIds = getThreadParticipantIds(thread);
  const canHide = actor.role === ROLE_ADMIN || participantIds.includes(normalizedUserId);
  if (!canHide) {
    throw new Error("You do not have access to this conversation.");
  }

  hideConversationPairForUser(normalizedUserId, conversationKey);
  return {
    threadId: String(thread.id),
    conversationKey,
  };
};

const getChatTargetsForUser = async ({ currentUserId }) => {
  const normalizedCurrentUserId = normalizeUserId(currentUserId);
  const { users, profiles } = await getUsersAndProfiles();

  const currentUser = users.find(
    (candidate) => normalizeUserId(candidate.id) === normalizedCurrentUserId,
  );
  if (!currentUser) {
    return [];
  }

  const currentUserProfile = findProfileByUserId(profiles, normalizedCurrentUserId);
  const isCurrentPlayerVerified = isApprovedPlayerProfile(currentUserProfile);

  const targets = users.filter((candidate) => {
    const candidateId = normalizeUserId(candidate.id);
    if (!candidateId || candidateId === normalizedCurrentUserId) {
      return false;
    }

    if (currentUser.role === ROLE_ADMIN) {
      return candidate.role === ROLE_PLAYER || candidate.role === ROLE_RECRUITER;
    }

    if (currentUser.role === ROLE_RECRUITER) {
      return candidate.role === ROLE_PLAYER;
    }

    if (currentUser.role === ROLE_PLAYER) {
      if (candidate.role !== ROLE_RECRUITER) {
        return false;
      }

      if (String(candidate.status || "").toUpperCase() !== "APPROVED") {
        return false;
      }

      return canPlayerInitiateRecruiterChat({
        recruiterDmOpenness: normalizeRecruiterDmOpenness(
          candidate.recruiterDmOpenness || candidate.dmOpenness,
        ),
        isPlayerVerified: isCurrentPlayerVerified,
      });
    }

    return false;
  });

  return targets
    .map((candidate) => ({
      id: normalizeUserId(candidate.id),
      fullName: candidate.fullName || candidate.username || "Unknown",
      username: candidate.username || "",
      role: candidate.role || "",
      organizationName: candidate.organizationName || "",
      recruiterDmOpenness: normalizeRecruiterDmOpenness(
        candidate.recruiterDmOpenness || candidate.dmOpenness,
      ),
    }))
    .sort((a, b) => a.fullName.localeCompare(b.fullName));
};

const getUnreadChatNotifications = async (userId, options = {}) => {
  const normalizedUserId = normalizeUserId(userId);
  const forceRefresh = Boolean(options.forceRefresh);

  const [messages, threads] = await Promise.all([
    safeGetCollection("/chat_messages", undefined, { forceRefresh }),
    safeGetCollection("/chat_threads", undefined, { forceRefresh }),
  ]);

  const unreadMessages = messages.filter(
    (message) => normalizeUserId(message.recipientId) === normalizedUserId && !message.readAt,
  );

  if (unreadMessages.length === 0) {
    return {
      unreadCount: 0,
      items: [],
    };
  }

  const senderIds = normalizeUniqueUserIds(unreadMessages.map((message) => message.senderId));
  const users = await getUsersByIds(senderIds, { forceRefresh });
  const usersById = new Map(users.map((user) => [normalizeUserId(user.id), user]));
  const threadsById = new Map(threads.map((thread) => [String(thread.id), thread]));
  const latestUnreadByConversation = new Map();

  unreadMessages.forEach((message) => {
    const thread = threadsById.get(String(message.threadId));
    const pairKey = getThreadParticipantKey(thread);
    const key = pairKey || `thread:${String(message.threadId)}`;
    const currentLatest = latestUnreadByConversation.get(key);

    if (!currentLatest || toTimestamp(message.createdAt) > toTimestamp(currentLatest.createdAt)) {
      latestUnreadByConversation.set(key, message);
    }
  });

  const items = Array.from(latestUnreadByConversation.values())
    .map((message) => {
      const sender = usersById.get(normalizeUserId(message.senderId));
      const thread = threadsById.get(String(message.threadId));

      return {
        threadId: message.threadId,
        senderId: normalizeUserId(message.senderId),
        senderName: sender?.fullName || sender?.username || "Unknown",
        senderRole: sender?.role || "",
        preview: message.body || "",
        createdAt: message.createdAt,
        participantIds: Array.isArray(thread?.participantIds)
          ? thread.participantIds.map((participantId) => normalizeUserId(participantId))
          : [],
      };
    })
    .sort((a, b) => toTimestamp(b.createdAt) - toTimestamp(a.createdAt));

  return {
    unreadCount: unreadMessages.length,
    items,
  };
};

export {
  ensureConversation,
  getChatTargetsForUser,
  getConversationsForUser,
  getMessagesForThread,
  getUnreadChatNotifications,
  hideConversationForUser,
  markThreadAsRead,
  sendMessage,
};
