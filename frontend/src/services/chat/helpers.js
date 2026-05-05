import {
  RECRUITER_DM_OPENNESS,
  canPlayerInitiateRecruiterChat,
} from "../recruiterService";
import { ROLE_ADMIN, ROLE_PLAYER, ROLE_RECRUITER } from "./constants";
import { getHiddenConversationsStore, setHiddenConversationsStore } from "./state";

const normalizeUserId = (userId) => String(userId ?? "").trim();
const normalizeText = (value) => String(value || "").trim();
const isApprovedPlayerProfile = (profile) =>
  String(profile?.rankVerificationStatus || "").toUpperCase() === "APPROVED" ||
  profile?.isVerified === true;

const toTimestamp = (value) => new Date(value || 0).getTime();

const serializeRequestParams = (params) => {
  if (!params || typeof params !== "object") {
    return "";
  }

  return Object.entries(params)
    .sort(([leftKey], [rightKey]) => leftKey.localeCompare(rightKey))
    .map(([key, value]) => `${key}=${Array.isArray(value) ? value.join(",") : String(value)}`)
    .join("&");
};

const buildCollectionCacheKey = (path, requestConfig = undefined) => {
  const serializedParams = serializeRequestParams(requestConfig?.params);
  return serializedParams ? `${path}?${serializedParams}` : path;
};

const normalizeConversationKey = (value) => String(value || "").trim();

const getHiddenConversationEntriesForUser = (userId) => {
  const normalizedUserId = normalizeUserId(userId);
  const store = getHiddenConversationsStore();
  const rawEntries = store[normalizedUserId];

  if (!rawEntries) {
    return {};
  }

  if (Array.isArray(rawEntries)) {
    return rawEntries.reduce((entries, entry) => {
      const conversationKey = normalizeConversationKey(entry);
      if (!conversationKey) {
        return entries;
      }

      return {
        ...entries,
        [conversationKey]: new Date(0).toISOString(),
      };
    }, {});
  }

  if (typeof rawEntries !== "object") {
    return {};
  }

  return Object.entries(rawEntries).reduce((entries, [conversationKey, hiddenAt]) => {
    const normalizedConversationKey = normalizeConversationKey(conversationKey);

    if (!normalizedConversationKey) {
      return entries;
    }

    return {
      ...entries,
      [normalizedConversationKey]: String(hiddenAt || new Date(0).toISOString()),
    };
  }, {});
};

const setHiddenConversationEntriesForUser = (userId, entries) => {
  const normalizedUserId = normalizeUserId(userId);
  const store = getHiddenConversationsStore();
  const nextEntries = Object.entries(entries || {}).reduce(
    (accumulator, [conversationKey, hiddenAt]) => {
      const normalizedConversationKey = normalizeConversationKey(conversationKey);

      if (!normalizedConversationKey) {
        return accumulator;
      }

      return {
        ...accumulator,
        [normalizedConversationKey]: String(hiddenAt || new Date(0).toISOString()),
      };
    },
    {},
  );

  if (Object.keys(nextEntries).length === 0) {
    delete store[normalizedUserId];
  } else {
    store[normalizedUserId] = nextEntries;
  }

  setHiddenConversationsStore(store);
};

const hideConversationPairForUser = (userId, conversationKey) => {
  const normalizedConversationKey = normalizeConversationKey(conversationKey);
  if (!normalizedConversationKey) {
    return;
  }

  const currentEntries = getHiddenConversationEntriesForUser(userId);
  setHiddenConversationEntriesForUser(userId, {
    ...currentEntries,
    [normalizedConversationKey]: new Date().toISOString(),
  });
};

const unhideConversationPairForUser = (userId, conversationKey) => {
  const normalizedConversationKey = normalizeConversationKey(conversationKey);
  if (!normalizedConversationKey) {
    return;
  }

  const currentEntries = getHiddenConversationEntriesForUser(userId);
  if (!currentEntries[normalizedConversationKey]) {
    return;
  }

  const remainingEntries = { ...currentEntries };
  delete remainingEntries[normalizedConversationKey];
  setHiddenConversationEntriesForUser(userId, remainingEntries);
};

const sortParticipantIds = (userIdA, userIdB) =>
  [normalizeUserId(userIdA), normalizeUserId(userIdB)].sort((a, b) => a.localeCompare(b));

const getThreadParticipantIds = (thread) => {
  if (!Array.isArray(thread?.participantIds)) {
    return [];
  }

  const participantIds = thread.participantIds
    .map((participantId) => normalizeUserId(participantId))
    .filter(Boolean);

  const uniqueParticipantIds = Array.from(new Set(participantIds));
  if (uniqueParticipantIds.length !== 2) {
    return [];
  }

  return uniqueParticipantIds.sort((a, b) => a.localeCompare(b));
};

const getParticipantPairKey = (userIdA, userIdB) => sortParticipantIds(userIdA, userIdB).join("::");

const getThreadParticipantKey = (thread) => {
  const participantIds = getThreadParticipantIds(thread);
  return participantIds.length === 2 ? participantIds.join("::") : "";
};

const getLatestThread = (threads) =>
  [...threads].sort(
    (a, b) =>
      toTimestamp(b?.lastMessageAt || b?.updatedAt || b?.createdAt) -
      toTimestamp(a?.lastMessageAt || a?.updatedAt || a?.createdAt),
  )[0] || null;

const findThreadsByParticipants = (threads, userIdA, userIdB) => {
  const pairKey = getParticipantPairKey(userIdA, userIdB);
  return threads.filter((thread) => getThreadParticipantKey(thread) === pairKey);
};

const findThreadById = (threads, threadId) =>
  threads.find((candidate) => String(candidate.id) === String(threadId)) || null;

const getGroupedThreadIdsByPairKey = (threads, pairKey) =>
  new Set(
    threads
      .filter((candidate) => getThreadParticipantKey(candidate) === pairKey)
      .map((candidate) => String(candidate.id)),
  );

const normalizeUniqueUserIds = (userIds) =>
  Array.from(new Set((userIds || []).map((userId) => normalizeUserId(userId)).filter(Boolean)));

const deduplicateByNormalizedId = (items, idSelector) => {
  const uniqueItemsById = new Map();

  (items || []).forEach((item) => {
    const normalizedId = normalizeUserId(idSelector(item));
    if (!normalizedId || uniqueItemsById.has(normalizedId)) {
      return;
    }

    uniqueItemsById.set(normalizedId, item);
  });

  return Array.from(uniqueItemsById.values());
};

const deduplicateUsers = (users) => deduplicateByNormalizedId(users, (user) => user?.id);
const deduplicateProfiles = (profiles) =>
  deduplicateByNormalizedId(profiles, (profile) => profile?.userId);

const getInitiationErrorMessage = ({
  initiator,
  target,
  targetAllowPlayerChats,
  targetRecruiterDmOpenness,
  initiatorIsVerifiedPlayer,
}) => {
  if (initiator.role === ROLE_PLAYER && target.role === ROLE_RECRUITER) {
    if (targetRecruiterDmOpenness === RECRUITER_DM_OPENNESS.CLOSED) {
      return "This recruiter is not accepting player direct messages right now.";
    }

    if (
      targetRecruiterDmOpenness === RECRUITER_DM_OPENNESS.OPEN_VERIFIED_PLAYERS &&
      !initiatorIsVerifiedPlayer
    ) {
      return "This recruiter accepts direct messages from verified players only.";
    }

    return "Players cannot initiate chats with this recruiter right now.";
  }

  if (
    initiator.role === ROLE_PLAYER &&
    target.role === ROLE_PLAYER &&
    targetAllowPlayerChats === false
  ) {
    return "This player only accepts chats from scouts.";
  }

  return "This chat cannot be started due to role restrictions.";
};

const canInitiateConversation = ({ initiator, target, targetAllowPlayerChats }) => {
  if (!initiator || !target) {
    return false;
  }

  if (normalizeUserId(initiator.id) === normalizeUserId(target.id)) {
    return false;
  }

  if (initiator.role === ROLE_RECRUITER && target.role === ROLE_PLAYER) {
    return true;
  }

  if (initiator.role === ROLE_ADMIN) {
    return (
      target.role === ROLE_PLAYER || target.role === ROLE_RECRUITER || target.role === ROLE_ADMIN
    );
  }

  if (initiator.role === ROLE_PLAYER) {
    if (target.role === ROLE_PLAYER) {
      return targetAllowPlayerChats !== false;
    }

    if (target.role === ROLE_RECRUITER) {
      return canPlayerInitiateRecruiterChat({
        recruiterDmOpenness: target.recruiterDmOpenness,
        isPlayerVerified: Boolean(initiator.isPlayerVerified),
      });
    }
  }

  return false;
};

const findProfileByUserId = (profiles, userId) => {
  const normalizedUserId = normalizeUserId(userId);
  return profiles.find((profile) => normalizeUserId(profile.userId) === normalizedUserId) || null;
};

export {
  buildCollectionCacheKey,
  canInitiateConversation,
  deduplicateProfiles,
  deduplicateUsers,
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
};
