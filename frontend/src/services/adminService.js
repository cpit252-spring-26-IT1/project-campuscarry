import { getRatingLabel, normalizeGameName, toCanonicalGameName } from "../constants/gameConfig";
import api from "./httpClient";
import { getPlayerProfileByUserId } from "./playerProfileService";
import { normalizeRoleList, normalizeUserId } from "./serviceNormalizers";

const normalizeDeclineReason = (value) => String(value || "").trim();

const getRecordByUserId = (records) => {
  const recordMap = new Map();

  records.forEach((record) => {
    recordMap.set(String(record.userId), record);
  });

  return recordMap;
};

const getPendingRecruiters = async () => {
  const [{ data: pendingUsers }, { data: pendingRecruiterRecords }] = await Promise.all([
    api.get("/users?role=RECRUITER&status=PENDING"),
    api.get("/pending_recruiters"),
  ]);

  const pendingRecordByUserId = getRecordByUserId(pendingRecruiterRecords);

  return pendingUsers.map((user) => {
    const pendingRecord = pendingRecordByUserId.get(String(user.id));

    return {
      id: user.id,
      userId: user.id,
      fullName: user.fullName,
      email: user.email,
      linkedinUrl: pendingRecord?.linkedinUrl || "",
      organizationName: user.organizationName,
      submittedAt: pendingRecord?.submittedAt || null,
      pendingId: pendingRecord?.id || null,
    };
  });
};

const getPendingRanks = async () => {
  const [{ data: pendingRankSubmissions }, { data: users }] = await Promise.all([
    api.get("/pending_ranks"),
    api.get("/users?role=PLAYER"),
  ]);

  const usersById = new Map(users.map((user) => [normalizeUserId(user.id), user]));

  return pendingRankSubmissions
    .filter((submission) => !submission.status || submission.status === "PENDING")
    .map((submission) => {
      const user = usersById.get(normalizeUserId(submission.userId));
      const inGameRoles = normalizeRoleList(
        submission.inGameRoles?.length ? submission.inGameRoles : submission.inGameRole,
      );

      return {
        id: submission.id,
        userId: normalizeUserId(submission.userId),
        fullName: submission.fullName || user?.fullName || "Unknown Player",
        username: submission.username || user?.username || "Unknown",
        game: normalizeGameName(submission.game),
        claimedRank: submission.claimedRank,
        inGameRoles,
        inGameRole: inGameRoles[0] || "",
        ratingValue: submission.ratingValue ?? null,
        ratingLabel:
          submission.ratingLabel || getRatingLabel(submission.game, submission.claimedRank),
        proofImage: submission.proofImage || "",
        submittedAt: submission.submittedAt || null,
      };
    })
    .sort((a, b) => new Date(b.submittedAt || 0) - new Date(a.submittedAt || 0));
};

const updateUserStatus = async (userId, status, extraFields = {}) => {
  await api.patch(`/users/${userId}`, {
    status,
    updatedAt: new Date().toISOString(),
    ...extraFields,
  });
};

const updateProfileStatus = async ({
  userId,
  rankVerificationStatus,
  isVerified,
  profileOverrides = {},
}) => {
  const existingProfile = await getPlayerProfileByUserId(userId);

  if (!existingProfile) {
    throw new Error("Player profile not found.");
  }

  await api.patch(`/player_profiles/${existingProfile.id}`, {
    rankVerificationStatus,
    isVerified,
    updatedAt: new Date().toISOString(),
    ...profileOverrides,
  });

  return existingProfile;
};

const upsertLeaderboardEntry = async ({
  userId,
  username,
  game,
  claimedRank,
  inGameRoles,
  inGameRole,
  ratingValue,
  ratingLabel,
}) => {
  const normalizedUserId = normalizeUserId(userId);
  const { data: allEntries } = await api.get("/leaderboard");
  const existingEntries = allEntries.filter(
    (entry) => normalizeUserId(entry.userId) === normalizedUserId,
  );

  const entryPayload = {
    userId: normalizedUserId,
    username,
    game: toCanonicalGameName(normalizeGameName(game)),
    rank: claimedRank,
    role: normalizeRoleList(inGameRoles?.length ? inGameRoles : inGameRole)[0] || "",
    ratingValue: ratingValue ?? null,
    ratingLabel: ratingLabel || getRatingLabel(game, claimedRank),
    updatedAt: new Date().toISOString(),
  };

  if (!existingEntries.length) {
    await api.post("/leaderboard", entryPayload);
    return;
  }

  await api.patch(`/leaderboard/${existingEntries[0].id}`, entryPayload);

  if (existingEntries.length > 1) {
    await Promise.all(
      existingEntries.slice(1).map((entry) => api.delete(`/leaderboard/${entry.id}`)),
    );
  }
};

const removeLeaderboardEntry = async (userId) => {
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

const removePendingRecord = async (path) => {
  try {
    await api.delete(path);
  } catch (error) {
    if (error.response?.status !== 404) {
      throw error;
    }
  }
};

const removePendingRecruiter = async (pendingRecruiterId) => {
  await removePendingRecord(`/pending_recruiters/${pendingRecruiterId}`);
};

const approveRecruiter = async (pendingRecruiter) => {
  await updateUserStatus(pendingRecruiter.userId, "APPROVED", {
    declineReason: "",
    declinedAt: null,
  });

  if (pendingRecruiter.pendingId !== null) {
    await removePendingRecruiter(pendingRecruiter.pendingId);
  }
};

const declineRecruiter = async (pendingRecruiter, declineReason) => {
  const reason = normalizeDeclineReason(declineReason);
  if (!reason) {
    throw new Error("Decline reason is required.");
  }

  await updateUserStatus(pendingRecruiter.userId, "DECLINED", {
    declineReason: reason,
    declinedAt: new Date().toISOString(),
  });

  if (pendingRecruiter.pendingId !== null) {
    await removePendingRecruiter(pendingRecruiter.pendingId);
  }
};

const approveRankSubmission = async (submission) => {
  await updateProfileStatus({
    userId: submission.userId,
    rankVerificationStatus: "APPROVED",
    isVerified: true,
    profileOverrides: {
      game: toCanonicalGameName(normalizeGameName(submission.game)),
      rank: submission.claimedRank,
      inGameRoles: normalizeRoleList(
        submission.inGameRoles?.length ? submission.inGameRoles : submission.inGameRole,
      ),
      inGameRole:
        normalizeRoleList(
          submission.inGameRoles?.length ? submission.inGameRoles : submission.inGameRole,
        )[0] || "",
      ratingValue: submission.ratingValue ?? null,
      ratingLabel:
        submission.ratingLabel || getRatingLabel(submission.game, submission.claimedRank),
      proofImage: submission.proofImage || "",
      declineReason: "",
      declinedAt: null,
    },
  });

  await api.patch(`/pending_ranks/${submission.id}`, {
    status: "APPROVED",
    resolvedAt: new Date().toISOString(),
    declineReason: "",
    editedAfterDecline: false,
    editedAt: null,
  });

  await upsertLeaderboardEntry(submission);
};

const declineRankSubmission = async (submission, declineReason) => {
  const reason = normalizeDeclineReason(declineReason);
  if (!reason) {
    throw new Error("Decline reason is required.");
  }

  const declinedAt = new Date().toISOString();

  await updateProfileStatus({
    userId: submission.userId,
    rankVerificationStatus: "DECLINED",
    isVerified: false,
    profileOverrides: {
      declineReason: reason,
      declinedAt,
    },
  });

  await api.patch(`/pending_ranks/${submission.id}`, {
    status: "DECLINED",
    resolvedAt: declinedAt,
    declineReason: reason,
    editedAfterDecline: false,
    editedAt: null,
  });

  await removeLeaderboardEntry(submission.userId);
};

const getDeclinedItems = async () => {
  const [{ data: declinedRecruiters }, { data: rankSubmissions }] = await Promise.all([
    api.get("/users?role=RECRUITER&status=DECLINED"),
    api.get("/pending_ranks?status=DECLINED"),
  ]);

  const declinedRecruiterItems = declinedRecruiters.map((recruiter) => ({
    id: `recruiter-${recruiter.id}`,
    type: "RECRUITER",
    fullName: recruiter.fullName,
    organizationName: recruiter.organizationName,
    email: recruiter.email,
    declineReason: normalizeDeclineReason(recruiter.declineReason),
    declinedAt: recruiter.declinedAt || recruiter.updatedAt || null,
    editedAfterDecline: false,
    editedAt: null,
  }));

  const declinedRankItems = rankSubmissions.map((submission) => ({
    id: `rank-${submission.id}`,
    type: "RANK",
    fullName: submission.fullName || submission.username,
    username: submission.username,
    game: normalizeGameName(submission.game),
    claimedRank: submission.claimedRank,
    ratingValue: submission.ratingValue ?? null,
    ratingLabel: submission.ratingLabel || getRatingLabel(submission.game, submission.claimedRank),
    proofImage: submission.proofImage || "",
    declineReason: normalizeDeclineReason(submission.declineReason),
    declinedAt: submission.resolvedAt || null,
    editedAfterDecline: Boolean(submission.editedAfterDecline),
    editedAt: submission.editedAt || null,
  }));

  return [...declinedRecruiterItems, ...declinedRankItems].sort((a, b) => {
    const aEditedRank = a.type === "RANK" && Boolean(a.editedAfterDecline);
    const bEditedRank = b.type === "RANK" && Boolean(b.editedAfterDecline);

    if (aEditedRank !== bEditedRank) {
      return aEditedRank ? -1 : 1;
    }

    const aTime = aEditedRank ? a.editedAt || a.declinedAt : a.declinedAt;
    const bTime = bEditedRank ? b.editedAt || b.declinedAt : b.declinedAt;

    return new Date(bTime || 0) - new Date(aTime || 0);
  });
};

const toComparableName = (user) =>
  String(user?.fullName || user?.username || user?.organizationName || "").toLowerCase();

const getRegisteredScouts = async () => {
  const { data: scouts } = await api.get("/users?role=RECRUITER");

  return scouts
    .map((scout) => ({
      id: normalizeUserId(scout.id),
      fullName: scout.fullName || "",
      email: scout.email || scout.personalEmail || "",
      organizationName: scout.organizationName || "",
      status: scout.status || "PENDING",
      declineReason: normalizeDeclineReason(scout.declineReason),
      updatedAt: scout.updatedAt || null,
    }))
    .sort((left, right) => toComparableName(left).localeCompare(toComparableName(right)));
};

const getRegisteredPlayers = async () => {
  const { data: players } = await api.get("/users?role=PLAYER");

  return players
    .map((player) => ({
      id: normalizeUserId(player.id),
      fullName: player.fullName || "",
      username: player.username || "",
      email: player.personalEmail || player.email || "",
      status: player.status || "APPROVED",
      updatedAt: player.updatedAt || null,
    }))
    .sort((left, right) => toComparableName(left).localeCompare(toComparableName(right)));
};

const deleteUserAccount = async (userId) => {
  const normalizedUserId = normalizeUserId(userId);

  if (!normalizedUserId) {
    throw new Error("User id is required.");
  }

  await api.delete(`/users/${normalizedUserId}`);
};

export {
  approveRankSubmission,
  approveRecruiter,
  deleteUserAccount,
  declineRankSubmission,
  declineRecruiter,
  getDeclinedItems,
  getPendingRanks,
  getPendingRecruiters,
  getRegisteredPlayers,
  getRegisteredScouts,
};
