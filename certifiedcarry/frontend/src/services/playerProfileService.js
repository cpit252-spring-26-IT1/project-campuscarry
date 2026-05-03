import api from "./httpClient";
import { normalizeUserId } from "./serviceNormalizers";

const DEFAULT_PLAYER_PROFILE_RATING_LABEL = "MMR";
const DEFAULT_PLAYER_PROFILE_STATUS = "NOT_SUBMITTED";

const buildEmptyPlayerProfilePayload = ({ userId, username }) => {
  const normalizedUserId = normalizeUserId(userId);

  return {
    userId: normalizedUserId,
    username,
    profileImage: "",
    game: "",
    rank: "",
    allowPlayerChats: true,
    rocketLeagueModes: [],
    primaryRocketLeagueMode: "",
    inGameRoles: [],
    inGameRole: "",
    ratingValue: null,
    ratingLabel: DEFAULT_PLAYER_PROFILE_RATING_LABEL,
    proofImage: "",
    bio: "",
    clipsUrl: "",
    rankVerificationStatus: DEFAULT_PLAYER_PROFILE_STATUS,
    declineReason: "",
    declinedAt: null,
    isWithTeam: false,
    teamName: null,
    isVerified: false,
    submittedAt: null,
    updatedAt: new Date().toISOString(),
  };
};

const getPlayerProfileByUserId = async (userId, { bustCache = false } = {}) => {
  const normalizedUserId = normalizeUserId(userId);
  const params = {
    userId: normalizedUserId,
  };

  if (bustCache) {
    params._ts = Date.now();
  }

  const { data: profiles } = await api.get("/player_profiles", { params });

  if (!Array.isArray(profiles) || profiles.length === 0) {
    return null;
  }

  return profiles.find((profile) => normalizeUserId(profile.userId) === normalizedUserId) || null;
};

const createPlayerProfile = async ({ userId, username }) => {
  const { data: createdProfile } = await api.post(
    "/player_profiles",
    buildEmptyPlayerProfilePayload({ userId, username }),
  );

  return createdProfile;
};

const ensurePlayerProfile = async ({ userId, username, bustCache = false }) => {
  const existingProfile = await getPlayerProfileByUserId(userId, { bustCache });

  if (existingProfile) {
    return existingProfile;
  }

  return createPlayerProfile({ userId, username });
};

export { createPlayerProfile, ensurePlayerProfile, getPlayerProfileByUserId };
