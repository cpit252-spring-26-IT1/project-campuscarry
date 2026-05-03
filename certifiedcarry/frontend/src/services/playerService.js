import {
  getProfileMissingFields,
  getRankWeight,
  getRatingLabel,
  isRoleDrivenGame,
  normalizeGameName,
  ROCKET_LEAGUE_MODES,
  toCanonicalGameName,
  requiresRatingValue,
} from "../constants/gameConfig";
import api from "./httpClient";
import { ensurePlayerProfile, getPlayerProfileByUserId } from "./playerProfileService";
import { normalizeRoleList, normalizeUserId } from "./serviceNormalizers";

const ROCKET_LEAGUE_GAME = "Rocket League";
const OPTIONAL_UPLOAD_ENDPOINT_STATUS_CODES = new Set([404, 405, 500, 501, 503]);
const MAX_IMAGE_UPLOAD_BYTES =
  Math.max(1, Number.parseInt(String(import.meta.env.VITE_IMAGE_UPLOAD_MAX_BYTES || ""), 10)) ||
  6 * 1024 * 1024;

const toNumericRating = (value) => {
  const numericValue = Number.parseFloat(String(value || "").trim());
  return Number.isFinite(numericValue) ? numericValue : null;
};

const normalizeRocketLeagueModes = (modes = []) => {
  const safeModes = Array.isArray(modes) ? modes : [];

  return safeModes
    .map((entry) => {
      const mode = String(entry?.mode || "").trim();

      if (!ROCKET_LEAGUE_MODES.includes(mode)) {
        return null;
      }

      return {
        mode,
        rank: String(entry?.rank || "").trim(),
        ratingValue: toNumericRating(entry?.ratingValue),
        ratingLabel: "MMR",
      };
    })
    .filter(Boolean)
    .sort((a, b) => ROCKET_LEAGUE_MODES.indexOf(a.mode) - ROCKET_LEAGUE_MODES.indexOf(b.mode));
};

const getRequestStatusCode = (error) => {
  const status = error?.response?.status;
  return Number.isInteger(status) ? status : null;
};

const isLikelyNetworkUploadError = (error) => {
  const errorName = String(error?.name || "").trim();
  const errorCode = String(error?.code || "")
    .trim()
    .toLowerCase();
  const message = String(error?.message || "")
    .trim()
    .toLowerCase();

  return (
    errorName === "TypeError" ||
    errorCode === "err_network" ||
    message.includes("failed to fetch") ||
    message.includes("network error") ||
    message.includes("load failed")
  );
};

const shouldFallbackToInlineUpload = (error) => {
  const statusCode = getRequestStatusCode(error);

  if (statusCode !== null) {
    return OPTIONAL_UPLOAD_ENDPOINT_STATUS_CODES.has(statusCode);
  }

  return isLikelyNetworkUploadError(error);
};

const readFileAsDataUrl = (file) =>
  new Promise((resolve, reject) => {
    const reader = new FileReader();

    reader.onload = () => {
      const result = reader.result;
      if (typeof result === "string") {
        resolve(result);
        return;
      }

      reject(new Error("Unable to read image file."));
    };

    reader.onerror = () => {
      reject(new Error("Unable to read image file."));
    };

    reader.readAsDataURL(file);
  });

const uploadToPresignedUrl = async ({ uploadUrl, requiredHeaders, file }) => {
  const headers = {
    ...requiredHeaders,
  };

  if (!headers["Content-Type"] && file.type) {
    headers["Content-Type"] = file.type;
  }

  const response = await fetch(uploadUrl, {
    method: "PUT",
    headers,
    body: file,
  });

  if (!response.ok) {
    throw new Error("Unable to upload image file.");
  }
};

const normalizeUploadAssetType = (assetType) => {
  const normalized = String(assetType || "")
    .trim()
    .toUpperCase();
  if (
    normalized === "PROFILE_IMAGE" ||
    normalized === "PROOF_IMAGE" ||
    normalized === "CHAT_IMAGE"
  ) {
    return normalized;
  }

  throw new Error("Unsupported upload asset type.");
};

const uploadPlayerProfileAsset = async ({ file, assetType, threadId = null }) => {
  if (!(file instanceof File)) {
    throw new Error("Image file is required.");
  }

  if (
    !String(file.type || "")
      .toLowerCase()
      .startsWith("image/")
  ) {
    throw new Error("Only image files are allowed.");
  }

  if (file.size <= 0) {
    throw new Error("Image file is empty.");
  }

  if (file.size > MAX_IMAGE_UPLOAD_BYTES) {
    throw new Error(
      `Image exceeds max size of ${Math.round(MAX_IMAGE_UPLOAD_BYTES / 1024 / 1024)} MB.`,
    );
  }

  const normalizedAssetType = normalizeUploadAssetType(assetType);
  const normalizedThreadId =
    threadId === null || threadId === undefined ? null : Number.parseInt(String(threadId), 10);

  if (
    normalizedAssetType === "CHAT_IMAGE" &&
    (!Number.isInteger(normalizedThreadId) || normalizedThreadId <= 0)
  ) {
    throw new Error("Chat thread id is required for chat image uploads.");
  }

  try {
    const { data } = await api.post("/media/uploads/presign", {
      assetType: normalizedAssetType,
      fileName: file.name,
      contentType: file.type,
      fileSizeBytes: file.size,
      threadId: normalizedAssetType === "CHAT_IMAGE" ? normalizedThreadId : null,
    });

    const uploadUrl = String(data?.uploadUrl || "").trim();
    const publicUrl = String(data?.publicUrl || "").trim();

    if (!uploadUrl || !publicUrl) {
      throw new Error("Upload URL is missing from the server response.");
    }

    await uploadToPresignedUrl({
      uploadUrl,
      requiredHeaders: data?.requiredHeaders || {},
      file,
    });

    return {
      url: publicUrl,
      storage: "object-storage",
    };
  } catch (error) {
    const shouldFallback = shouldFallbackToInlineUpload(error);

    if (shouldFallback) {
      const inlineUrl = await readFileAsDataUrl(file);
      return {
        url: inlineUrl,
        storage: "inline",
      };
    }

    if (error instanceof Error) {
      throw error;
    }

    throw new Error("Unable to upload image file.");
  }
};

const getFallbackRocketLeagueModes = (profile) => {
  const normalizedGame = normalizeGameName(profile?.game || "");

  if (normalizedGame !== ROCKET_LEAGUE_GAME) {
    return [];
  }

  const rank = profile?.rank || "";
  const ratingValue = toNumericRating(profile?.ratingValue);

  if (!rank || ratingValue === null) {
    return [];
  }

  return ROCKET_LEAGUE_MODES.map((mode) => ({
    mode,
    rank,
    ratingValue,
    ratingLabel: "MMR",
  }));
};

const getPrimaryRocketLeagueMode = (profile) => {
  const explicitMode = String(profile?.primaryRocketLeagueMode || "").trim();

  if (ROCKET_LEAGUE_MODES.includes(explicitMode)) {
    return explicitMode;
  }

  const normalizedModes = normalizeRocketLeagueModes(profile?.rocketLeagueModes || []);
  if (normalizedModes.length > 0) {
    return normalizedModes[0].mode;
  }

  return "2v2";
};

const getRocketLeagueModeSignature = (modes) =>
  JSON.stringify(
    normalizeRocketLeagueModes(modes).map((entry) => ({
      mode: entry.mode,
      rank: entry.rank,
      ratingValue: entry.ratingValue,
    })),
  );

const getComparableTimestamp = (record) =>
  new Date(
    record?.editedAt || record?.resolvedAt || record?.updatedAt || record?.submittedAt || 0,
  ).getTime();

const upsertPendingRankSubmission = async ({ markLatestDeclinedAsEdited = false, ...payload }) => {
  const { data: allSubmissions } = await api.get("/pending_ranks");

  const submissionPayload = {
    ...payload,
    status: "PENDING",
    resolvedAt: null,
    editedAfterDecline: false,
    editedAt: null,
  };

  const pendingSubmissions = allSubmissions.filter(
    (submission) =>
      normalizeUserId(submission.userId) === submissionPayload.userId &&
      (!submission.status || submission.status === "PENDING"),
  );

  if (markLatestDeclinedAsEdited) {
    const declinedSubmissions = allSubmissions.filter(
      (submission) =>
        normalizeUserId(submission.userId) === submissionPayload.userId &&
        submission.status === "DECLINED",
    );

    if (declinedSubmissions.length > 0) {
      const [latestDeclined] = [...declinedSubmissions].sort(
        (a, b) => getComparableTimestamp(b) - getComparableTimestamp(a),
      );
      const editedAt = new Date().toISOString();

      await api.patch(`/pending_ranks/${latestDeclined.id}`, {
        ...submissionPayload,
        status: "DECLINED",
        declineReason: latestDeclined.declineReason || "",
        resolvedAt: latestDeclined.resolvedAt || latestDeclined.updatedAt || editedAt,
        editedAfterDecline: true,
        editedAt,
      });
    }
  }

  if (pendingSubmissions.length > 0) {
    const primarySubmission = pendingSubmissions[0];
    await api.patch(`/pending_ranks/${primarySubmission.id}`, submissionPayload);

    if (pendingSubmissions.length > 1) {
      await Promise.all(
        pendingSubmissions
          .slice(1)
          .map((submission) => api.delete(`/pending_ranks/${submission.id}`)),
      );
    }

    return;
  }

  await api.post("/pending_ranks", submissionPayload);
};

const removeLeaderboardEntryForUser = async (userId) => {
  const normalizedUserId = normalizeUserId(userId);
  const { data: allEntries } = await api.get("/leaderboard");
  const existingEntries = allEntries.filter(
    (entry) => normalizeUserId(entry.userId) === normalizedUserId,
  );

  if (!existingEntries.length) {
    return;
  }

  await Promise.all(existingEntries.map((entry) => api.delete(`/leaderboard/${entry.id}`)));
};

const submitPlayerProfile = async ({ user, profileInput }) => {
  const existingProfile = await ensurePlayerProfile({ userId: user.id, username: user.username });

  const game = normalizeGameName(profileInput.game);
  const canonicalGame = toCanonicalGameName(game);
  const isRocketLeague = game === ROCKET_LEAGUE_GAME;
  let rank = String(profileInput.rank || "").trim();
  let inGameRoles = normalizeRoleList(profileInput.inGameRoles || profileInput.inGameRole);
  let inGameRole = inGameRoles[0] || "";
  const isWithTeam = Boolean(profileInput.isWithTeam);
  const teamName = isWithTeam ? String(profileInput.teamName || "").trim() : "";
  let ratingLabel = getRatingLabel(game, rank);
  let ratingValue = requiresRatingValue(game, rank)
    ? toNumericRating(profileInput.ratingValue)
    : null;
  let rocketLeagueModes = [];
  let primaryRocketLeagueMode = "";

  if (!game) {
    throw new Error("Game is required.");
  }

  if (isWithTeam && !teamName) {
    throw new Error("Please provide your team name.");
  }

  if (isRocketLeague) {
    rocketLeagueModes = normalizeRocketLeagueModes(profileInput.rocketLeagueModes || []);

    if (!rocketLeagueModes.length) {
      throw new Error("Please choose at least one Rocket League game mode.");
    }

    const invalidMode = rocketLeagueModes.find(
      (modeEntry) => !modeEntry.rank || modeEntry.ratingValue === null,
    );

    if (invalidMode) {
      throw new Error(`Please complete rank and MMR for ${invalidMode.mode}.`);
    }

    const requestedPrimaryMode = String(profileInput.primaryRocketLeagueMode || "").trim();
    const availableModes = new Set(rocketLeagueModes.map((modeEntry) => modeEntry.mode));
    primaryRocketLeagueMode = availableModes.has(requestedPrimaryMode)
      ? requestedPrimaryMode
      : rocketLeagueModes[0].mode;

    const primaryModeEntry =
      rocketLeagueModes.find((modeEntry) => modeEntry.mode === primaryRocketLeagueMode) ||
      rocketLeagueModes[0];

    rank = primaryModeEntry.rank;
    ratingLabel = "MMR";
    ratingValue = primaryModeEntry.ratingValue;
    inGameRoles = [];
    inGameRole = "";
  } else {
    if (!rank) {
      throw new Error("Game and rank are required.");
    }

    if (requiresRatingValue(game, rank) && ratingValue === null) {
      throw new Error(`${ratingLabel} is required for this rank.`);
    }

    if (isRoleDrivenGame(game) && inGameRoles.length === 0) {
      throw new Error("Please select your in-game role.");
    }

    inGameRole = inGameRoles[0] || "";
  }

  const existingGame = normalizeGameName(existingProfile.game || "");
  const existingRank = existingProfile.rank || "";
  const existingRatingValue = toNumericRating(existingProfile.ratingValue);
  const existingProofImage = existingProfile.proofImage || "";
  const existingRocketLeagueModesSignature = getRocketLeagueModeSignature(
    existingProfile.rocketLeagueModes?.length
      ? existingProfile.rocketLeagueModes
      : getFallbackRocketLeagueModes(existingProfile),
  );
  const nextRocketLeagueModesSignature = getRocketLeagueModeSignature(rocketLeagueModes);
  const existingPrimaryMode = getPrimaryRocketLeagueMode(existingProfile);
  const nextProofImage = profileInput.proofImage || existingProofImage || "";

  const verificationSensitiveChanged =
    existingGame !== game ||
    existingRank !== rank ||
    existingRatingValue !== ratingValue ||
    (isRocketLeague &&
      (existingRocketLeagueModesSignature !== nextRocketLeagueModesSignature ||
        existingPrimaryMode !== primaryRocketLeagueMode)) ||
    existingProofImage !== nextProofImage;

  if (verificationSensitiveChanged && !nextProofImage) {
    throw new Error("Please upload proof showing your gamertag and rank details.");
  }

  const preservedVerificationStatus =
    existingProfile.rankVerificationStatus ||
    (existingProfile.isVerified ? "APPROVED" : "NOT_SUBMITTED");

  const profilePayload = {
    username: user.username,
    profileImage:
      typeof profileInput.profileImage === "string"
        ? profileInput.profileImage
        : existingProfile.profileImage || "",
    game: canonicalGame,
    rank,
    rocketLeagueModes: isRocketLeague ? rocketLeagueModes : [],
    primaryRocketLeagueMode: isRocketLeague ? primaryRocketLeagueMode : "",
    inGameRoles,
    inGameRole,
    ratingValue,
    ratingLabel,
    proofImage: nextProofImage,
    isWithTeam,
    teamName: isWithTeam ? teamName : null,
    bio: profileInput.bio || "",
    clipsUrl: profileInput.clipsUrl || "",
    rankVerificationStatus: verificationSensitiveChanged ? "PENDING" : preservedVerificationStatus,
    declineReason: verificationSensitiveChanged ? "" : existingProfile.declineReason || "",
    isVerified: verificationSensitiveChanged ? false : Boolean(existingProfile.isVerified),
    submittedAt: verificationSensitiveChanged
      ? new Date().toISOString()
      : existingProfile.submittedAt || null,
    updatedAt: new Date().toISOString(),
  };

  await api.patch(`/player_profiles/${existingProfile.id}`, profilePayload);

  if (verificationSensitiveChanged) {
    await upsertPendingRankSubmission({
      userId: normalizeUserId(user.id),
      username: user.username,
      fullName: user.fullName,
      game: canonicalGame,
      claimedRank: rank,
      inGameRoles,
      inGameRole,
      ratingValue,
      ratingLabel,
      rocketLeagueModes: isRocketLeague ? rocketLeagueModes : [],
      primaryRocketLeagueMode: isRocketLeague ? primaryRocketLeagueMode : "",
      proofImage: nextProofImage,
      submittedAt: new Date().toISOString(),
      markLatestDeclinedAsEdited: existingProfile.rankVerificationStatus === "DECLINED",
    });

    await removeLeaderboardEntryForUser(user.id);
  }

  return {
    requiresVerification: verificationSensitiveChanged,
    rankVerificationStatus: profilePayload.rankVerificationStatus,
  };
};

const updatePlayerChatPreference = async ({ userId, allowPlayerChats }) => {
  const normalizedUserId = normalizeUserId(userId);
  const existingProfile = await getPlayerProfileByUserId(normalizedUserId);

  if (!existingProfile) {
    throw new Error("Player profile not found.");
  }

  const normalizedPreference = Boolean(allowPlayerChats);

  await api.patch(`/player_profiles/${existingProfile.id}`, {
    allowPlayerChats: normalizedPreference,
    updatedAt: new Date().toISOString(),
  });

  return normalizedPreference;
};

const mapProfileWithUser = (user, profile) => {
  const normalizedGame = normalizeGameName(profile?.game || "");
  const normalizedInGameRoles = normalizeRoleList(
    profile?.inGameRoles?.length ? profile.inGameRoles : profile?.inGameRole || profile?.role || "",
  );
  const normalizedRocketLeagueModes = normalizeRocketLeagueModes(profile?.rocketLeagueModes || []);
  const rocketLeagueModes =
    normalizedGame === ROCKET_LEAGUE_GAME
      ? normalizedRocketLeagueModes.length
        ? normalizedRocketLeagueModes
        : getFallbackRocketLeagueModes(profile)
      : [];

  const explicitPrimaryMode = String(profile?.primaryRocketLeagueMode || "").trim();
  const primaryRocketLeagueMode =
    normalizedGame === ROCKET_LEAGUE_GAME
      ? ROCKET_LEAGUE_MODES.includes(explicitPrimaryMode)
        ? explicitPrimaryMode
        : rocketLeagueModes[0]?.mode || "2v2"
      : "";

  return {
    userId: normalizeUserId(user.id),
    fullName: user.fullName,
    username: user.username,
    game: normalizedGame,
    rank: profile?.rank || "",
    allowPlayerChats: profile?.allowPlayerChats !== false,
    isWithTeam: Boolean(profile?.isWithTeam),
    teamName: profile?.teamName || "",
    rocketLeagueModes,
    primaryRocketLeagueMode,
    inGameRoles: normalizedInGameRoles,
    inGameRole: normalizedInGameRoles[0] || "",
    ratingValue: profile?.ratingValue ?? null,
    ratingLabel: profile?.ratingLabel || getRatingLabel(normalizedGame, profile?.rank),
    profileImage: profile?.profileImage || "",
    bio: profile?.bio || "",
    clipsUrl: profile?.clipsUrl || "",
    proofImage: profile?.proofImage || "",
    rankVerificationStatus:
      profile?.rankVerificationStatus || (profile?.isVerified ? "APPROVED" : "NOT_SUBMITTED"),
    declineReason: String(profile?.declineReason || "").trim(),
    declinedAt: profile?.declinedAt || null,
    isVerified: Boolean(profile?.isVerified),
    submittedAt: profile?.submittedAt || null,
    updatedAt: profile?.updatedAt || null,
  };
};

const getPlayerProfileDetails = async (userId) => {
  const normalizedUserId = normalizeUserId(userId);

  const [{ data: users }, profile] = await Promise.all([
    api.get("/users", {
      params: {
        id: normalizedUserId,
        role: "PLAYER",
        _ts: Date.now(),
      },
    }),
    getPlayerProfileByUserId(normalizedUserId, { bustCache: true }),
  ]);

  const user = users[0];
  if (!user) {
    return null;
  }

  const ensuredProfile =
    profile ||
    (await ensurePlayerProfile({
      userId: normalizedUserId,
      username: user.username,
      bustCache: true,
    }));
  const mappedProfile = mapProfileWithUser(user, ensuredProfile);

  return {
    ...mappedProfile,
    missingFields: getProfileMissingFields(mappedProfile),
  };
};

const buildLeaderboardEntries = async () => {
  const [{ data: users }, { data: profiles }] = await Promise.all([
    api.get("/users?role=PLAYER"),
    api.get("/player_profiles"),
  ]);

  const usersById = new Map(users.map((user) => [normalizeUserId(user.id), user]));

  return profiles
    .map((profile) => {
      const user = usersById.get(normalizeUserId(profile.userId));
      if (!user) {
        return null;
      }

      const mapped = mapProfileWithUser(user, profile);
      const isApproved = mapped.rankVerificationStatus === "APPROVED" || mapped.isVerified;

      if (!isApproved) {
        return null;
      }

      return mapped;
    })
    .filter(Boolean);
};

const getLeaderboardEntries = async ({ game = "", role = "", mode = "" } = {}) => {
  const normalizedGameFilter = normalizeGameName(game);
  const normalizedMode = String(mode || "").trim();
  const effectiveRocketLeagueMode =
    normalizedGameFilter === ROCKET_LEAGUE_GAME
      ? ROCKET_LEAGUE_MODES.includes(normalizedMode)
        ? normalizedMode
        : "2v2"
      : "";
  const allEntries = await buildLeaderboardEntries();

  const filteredEntries = allEntries
    .flatMap((entry) => {
      const matchGame = !normalizedGameFilter || entry.game === normalizedGameFilter;
      if (!matchGame) {
        return [];
      }

      if (entry.game === ROCKET_LEAGUE_GAME) {
        const modeEntries = entry.rocketLeagueModes?.length
          ? entry.rocketLeagueModes
          : getFallbackRocketLeagueModes(entry);

        const selectedModeEntry = modeEntries.find(
          (modeEntry) => modeEntry.mode === effectiveRocketLeagueMode,
        );

        if (!selectedModeEntry) {
          return [];
        }

        return [
          {
            ...entry,
            rocketLeagueMode: selectedModeEntry.mode,
            rank: selectedModeEntry.rank || entry.rank,
            ratingValue:
              selectedModeEntry.ratingValue !== null && selectedModeEntry.ratingValue !== undefined
                ? selectedModeEntry.ratingValue
                : entry.ratingValue,
            ratingLabel: "MMR",
          },
        ];
      }

      const roleList = normalizeRoleList(
        entry.inGameRoles?.length ? entry.inGameRoles : entry.inGameRole,
      );
      const matchRole = !role || roleList.includes(role);
      if (!matchRole) {
        return [];
      }

      return [entry];
    })
    .filter(Boolean);

  filteredEntries.sort((a, b) => {
    const aRating = toNumericRating(a.ratingValue);
    const bRating = toNumericRating(b.ratingValue);

    if (aRating !== null || bRating !== null) {
      if (aRating === null) {
        return 1;
      }

      if (bRating === null) {
        return -1;
      }

      if (bRating !== aRating) {
        return bRating - aRating;
      }
    }

    const rankDiff = getRankWeight(b.game, b.rank) - getRankWeight(a.game, a.rank);
    if (rankDiff !== 0) {
      return rankDiff;
    }

    return a.username.localeCompare(b.username);
  });

  return filteredEntries.map((entry, index) => ({
    ...entry,
    position: index + 1,
  }));
};

const getPlayerRankPosition = async ({ userId, game, mode = "" }) => {
  if (!game) {
    return null;
  }

  const entries = await getLeaderboardEntries({ game, mode });
  const normalizedUserId = normalizeUserId(userId);
  const foundEntry = entries.find((entry) => entry.userId === normalizedUserId);
  return foundEntry?.position || null;
};

const getBrowsePlayers = async ({ game = "", role = "", minimumRank = "" } = {}) => {
  const entries = await getLeaderboardEntries({ game, role });

  if (!minimumRank) {
    return entries;
  }

  return entries.filter(
    (entry) => getRankWeight(entry.game, entry.rank) >= getRankWeight(entry.game, minimumRank),
  );
};

export {
  getBrowsePlayers,
  getLeaderboardEntries,
  getPlayerProfileDetails,
  getPlayerRankPosition,
  submitPlayerProfile,
  uploadPlayerProfileAsset,
  updatePlayerChatPreference,
};
