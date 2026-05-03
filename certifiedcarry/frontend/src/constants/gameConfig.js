export const GAME_OPTIONS = [
  "Valorant",
  "League of Legends",
  "EA FC",
  "Rocket League",
  "Overwatch 2",
];

export const ROCKET_LEAGUE_MODES = ["1v1", "2v2", "3v3"];

export const GAME_ROLE_OPTIONS = {
  Valorant: ["Duelist", "Initiator", "Controller", "Sentinel"],
  "League of Legends": ["Top Laner", "Jungler", "Mid Laner", "Bot Laner (ADC)", "Support"],
  "Overwatch 2": ["Tank", "Damage", "Support"],
};

export const GAME_RANK_OPTIONS = {
  Valorant: ["Immortal 1", "Immortal 2", "Immortal 3", "Radiant"],
  "League of Legends": ["Grandmaster", "Challenger"],
  "EA FC": ["Division 1", "Elite"],
  "Rocket League": [
    "Grand Champion 1",
    "Grand Champion 2",
    "Grand Champion 3",
    "Supersonic Legend",
  ],
  "Overwatch 2": ["Top 500", "Champion", "Grandmaster"],
};

const CUSTOM_RANK_WEIGHTS = {
  "Overwatch 2": {
    Grandmaster: 1,
    Champion: 2,
    "Top 500": 3,
  },
};

const GAME_DISPLAY_ALIASES = {
  LoL: "League of Legends",
};

const GAME_CANONICAL_ALIASES = {
  "League of Legends": "LoL",
};

export const normalizeGameName = (game) => GAME_DISPLAY_ALIASES[game] || game;

export const toCanonicalGameName = (game) => GAME_CANONICAL_ALIASES[game] || game;

export const isRoleDrivenGame = (game) => {
  const normalizedGame = normalizeGameName(game);
  return Boolean(GAME_ROLE_OPTIONS[normalizedGame]?.length);
};

export const getRatingLabel = (game, rank) => {
  const normalizedGame = normalizeGameName(game);

  if (normalizedGame === "Overwatch 2") {
    return "Rank";
  }

  if (normalizedGame === "EA FC" && rank === "Elite") {
    return "Skill Rating";
  }

  return "MMR";
};

export const requiresRatingValue = (game, rank) => {
  const normalizedGame = normalizeGameName(game);

  if (normalizedGame === "Overwatch 2") {
    return false;
  }

  if (normalizedGame === "EA FC") {
    return rank === "Elite";
  }

  return Boolean(normalizedGame);
};

export const getRankWeight = (game, rank) => {
  const normalizedGame = normalizeGameName(game);
  const customGameRankWeights = CUSTOM_RANK_WEIGHTS[normalizedGame];

  if (customGameRankWeights && customGameRankWeights[rank]) {
    return customGameRankWeights[rank];
  }

  const normalizedRanks = GAME_RANK_OPTIONS[normalizedGame] || [];
  const index = normalizedRanks.indexOf(rank);

  if (index === -1) {
    return 0;
  }

  return index + 1;
};

export const getProfileMissingFields = (profile) => {
  const missing = [];
  const normalizedGame = normalizeGameName(profile?.game || "");
  const verificationStatus = String(profile?.rankVerificationStatus || "")
    .trim()
    .toUpperCase();
  const hasCompletedVerificationFlow =
    profile?.isVerified === true ||
    verificationStatus === "PENDING" ||
    verificationStatus === "APPROVED" ||
    verificationStatus === "DECLINED";
  const roleList = Array.isArray(profile?.inGameRoles)
    ? profile.inGameRoles.filter(Boolean)
    : profile?.inGameRole
      ? [profile.inGameRole]
      : [];

  if (!profile?.game) {
    missing.push("game");
  }

  if (normalizedGame === "Rocket League") {
    const rocketLeagueModes = Array.isArray(profile?.rocketLeagueModes)
      ? profile.rocketLeagueModes
      : [];

    if (!rocketLeagueModes.length) {
      missing.push("rocketLeagueModes");
    }
  }

  if (!profile?.rank) {
    missing.push("rank");
  }

  const hasRatingValue =
    profile?.ratingValue !== null &&
    profile?.ratingValue !== undefined &&
    String(profile.ratingValue).trim() !== "";

  if (requiresRatingValue(profile?.game, profile?.rank) && !hasRatingValue) {
    missing.push("ratingValue");
  }

  if (isRoleDrivenGame(profile?.game) && roleList.length === 0) {
    missing.push("inGameRole");
  }

  if (!profile?.proofImage && !hasCompletedVerificationFlow) {
    missing.push("proofImage");
  }

  return missing;
};
