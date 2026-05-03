import { COLLECTION_CACHE_TTL_MS, HIDDEN_CONVERSATIONS_STORAGE_KEY } from "./constants";

const collectionCache = new Map();

const readCollectionCache = (cacheKey) => {
  const cachedEntry = collectionCache.get(cacheKey);
  if (!cachedEntry) {
    return null;
  }

  if (Date.now() > cachedEntry.expiresAt) {
    collectionCache.delete(cacheKey);
    return null;
  }

  return cachedEntry.value;
};

const writeCollectionCache = (cacheKey, value) => {
  collectionCache.set(cacheKey, {
    value,
    expiresAt: Date.now() + COLLECTION_CACHE_TTL_MS,
  });
};

const invalidateCollectionCacheByPrefix = (pathPrefixes) => {
  if (!Array.isArray(pathPrefixes) || pathPrefixes.length === 0) {
    collectionCache.clear();
    return;
  }

  for (const cacheKey of collectionCache.keys()) {
    if (pathPrefixes.some((prefix) => cacheKey === prefix || cacheKey.startsWith(`${prefix}?`))) {
      collectionCache.delete(cacheKey);
    }
  }
};

const invalidateChatCollections = () => {
  invalidateCollectionCacheByPrefix([
    "/chat_threads",
    "/chat_messages",
    "/users",
    "/player_profiles",
  ]);
};

const getHiddenConversationsStore = () => {
  if (globalThis.window === undefined) {
    return {};
  }

  try {
    const rawValue = globalThis.localStorage.getItem(HIDDEN_CONVERSATIONS_STORAGE_KEY);
    if (!rawValue) {
      return {};
    }

    const parsed = JSON.parse(rawValue);
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
};

const setHiddenConversationsStore = (store) => {
  if (globalThis.window === undefined) {
    return;
  }

  globalThis.localStorage.setItem(HIDDEN_CONVERSATIONS_STORAGE_KEY, JSON.stringify(store));
};

export {
  getHiddenConversationsStore,
  invalidateChatCollections,
  readCollectionCache,
  setHiddenConversationsStore,
  writeCollectionCache,
};
