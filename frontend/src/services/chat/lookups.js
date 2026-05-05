import api from "../httpClient";
import { BULK_LOOKUP_THRESHOLD } from "./constants";
import {
  buildCollectionCacheKey,
  deduplicateProfiles,
  deduplicateUsers,
  normalizeUniqueUserIds,
  normalizeUserId,
} from "./helpers";
import { readCollectionCache, writeCollectionCache } from "./state";

const safeGetCollection = async (path, requestConfig = undefined, options = {}) => {
  const { forceRefresh = false } = options;
  const cacheKey = buildCollectionCacheKey(path, requestConfig);

  if (!forceRefresh) {
    const cachedCollection = readCollectionCache(cacheKey);
    if (cachedCollection) {
      return cachedCollection;
    }
  }

  try {
    const { data } = await api.get(path, requestConfig);
    const normalizedData = Array.isArray(data) ? data : [];
    writeCollectionCache(cacheKey, normalizedData);
    return normalizedData;
  } catch (error) {
    if (error?.response?.status === 404) {
      writeCollectionCache(cacheKey, []);
      return [];
    }

    throw error;
  }
};

const getUsersByIds = async (userIds, options = {}) => {
  const { forceRefresh = false } = options;
  const uniqueUserIds = normalizeUniqueUserIds(userIds);
  if (uniqueUserIds.length === 0) {
    return [];
  }

  if (uniqueUserIds.length <= BULK_LOOKUP_THRESHOLD) {
    const responses = await Promise.all(
      uniqueUserIds.map((userId) =>
        safeGetCollection("/users", { params: { id: userId } }, { forceRefresh }),
      ),
    );

    return deduplicateUsers(responses.flat());
  }

  const allUsers = await safeGetCollection("/users", undefined, { forceRefresh });
  const idSet = new Set(uniqueUserIds);
  return deduplicateUsers(allUsers.filter((user) => idSet.has(normalizeUserId(user?.id))));
};

const getProfilesByUserIds = async (userIds, options = {}) => {
  const { forceRefresh = false } = options;
  const uniqueUserIds = normalizeUniqueUserIds(userIds);
  if (uniqueUserIds.length === 0) {
    return [];
  }

  if (uniqueUserIds.length <= BULK_LOOKUP_THRESHOLD) {
    const responses = await Promise.all(
      uniqueUserIds.map((userId) =>
        safeGetCollection("/player_profiles", { params: { userId } }, { forceRefresh }),
      ),
    );

    return deduplicateProfiles(responses.flat());
  }

  const allProfiles = await safeGetCollection("/player_profiles", undefined, { forceRefresh });
  const idSet = new Set(uniqueUserIds);
  return deduplicateProfiles(
    allProfiles.filter((profile) => idSet.has(normalizeUserId(profile?.userId))),
  );
};

const getUsersAndProfiles = async (options = {}) => {
  const { userIds, profileUserIds, forceRefresh = false } = options;

  const usersPromise = Array.isArray(userIds)
    ? getUsersByIds(userIds, { forceRefresh })
    : safeGetCollection("/users", undefined, { forceRefresh });
  const profilesPromise = Array.isArray(profileUserIds)
    ? getProfilesByUserIds(profileUserIds, { forceRefresh })
    : safeGetCollection("/player_profiles", undefined, { forceRefresh });

  const [users, profiles] = await Promise.all([usersPromise, profilesPromise]);

  return { users, profiles };
};

export { getProfilesByUserIds, getUsersAndProfiles, getUsersByIds, safeGetCollection };
