import api from "./httpClient";
import { normalizeUserId } from "./serviceNormalizers";

const RECRUITER_DM_OPENNESS = {
  CLOSED: "CLOSED",
  OPEN_ALL_PLAYERS: "OPEN_ALL_PLAYERS",
  OPEN_VERIFIED_PLAYERS: "OPEN_VERIFIED_PLAYERS",
};

const normalizeRecruiterDmOpenness = (value) => {
  const normalizedValue = String(value || "")
    .trim()
    .toUpperCase();

  if (normalizedValue === RECRUITER_DM_OPENNESS.OPEN_ALL_PLAYERS) {
    return RECRUITER_DM_OPENNESS.OPEN_ALL_PLAYERS;
  }

  if (normalizedValue === RECRUITER_DM_OPENNESS.OPEN_VERIFIED_PLAYERS) {
    return RECRUITER_DM_OPENNESS.OPEN_VERIFIED_PLAYERS;
  }

  return RECRUITER_DM_OPENNESS.CLOSED;
};

const resolveRecruiterDmOpenness = ({ serverValue }) => {
  const candidateValue = String(serverValue || "").trim() || RECRUITER_DM_OPENNESS.OPEN_ALL_PLAYERS;
  return normalizeRecruiterDmOpenness(candidateValue);
};

const canPlayerInitiateRecruiterChat = ({ recruiterDmOpenness, isPlayerVerified }) => {
  const normalizedOpenness = normalizeRecruiterDmOpenness(recruiterDmOpenness);

  if (normalizedOpenness === RECRUITER_DM_OPENNESS.OPEN_ALL_PLAYERS) {
    return true;
  }

  if (normalizedOpenness === RECRUITER_DM_OPENNESS.OPEN_VERIFIED_PLAYERS) {
    return Boolean(isPlayerVerified);
  }

  return false;
};

const mapRecruiterRecord = (user) => {
  const normalizedUserId = normalizeUserId(user?.id);

  return {
    id: normalizedUserId,
    fullName: user?.fullName || user?.organizationName || "Unknown",
    username: user?.username || "",
    organizationName: user?.organizationName || "",
    role: String(user?.role || "").toUpperCase(),
    status: String(user?.status || "").toUpperCase(),
    recruiterDmOpenness: resolveRecruiterDmOpenness({
      serverValue: user?.recruiterDmOpenness || user?.dmOpenness,
    }),
  };
};

const getRecruiterDirectory = async ({
  onlyEligibleForPlayer = false,
  isCurrentPlayerVerified = false,
} = {}) => {
  const { data: users } = await api.get("/users");

  const recruiters = Array.isArray(users)
    ? users
        .map((user) => mapRecruiterRecord(user))
        .filter((recruiter) => recruiter.id && recruiter.role === "RECRUITER")
        .filter((recruiter) => recruiter.status === "APPROVED")
    : [];

  const filteredRecruiters = onlyEligibleForPlayer
    ? recruiters.filter((recruiter) =>
        canPlayerInitiateRecruiterChat({
          recruiterDmOpenness: recruiter.recruiterDmOpenness,
          isPlayerVerified: isCurrentPlayerVerified,
        }),
      )
    : recruiters;

  return filteredRecruiters.sort((leftRecruiter, rightRecruiter) =>
    leftRecruiter.fullName.localeCompare(rightRecruiter.fullName),
  );
};

const getRecruiterDmOpenness = async ({ userId }) => {
  normalizeUserId(userId);
  const { data } = await api.get("/users/me/dm-openness");
  return resolveRecruiterDmOpenness({
    serverValue: data?.recruiterDmOpenness || data?.dmOpenness,
  });
};

const updateRecruiterDmOpenness = async ({ userId, recruiterDmOpenness }) => {
  normalizeUserId(userId);
  const normalizedOpenness = normalizeRecruiterDmOpenness(recruiterDmOpenness);
  const { data } = await api.patch("/users/me/dm-openness", {
    recruiterDmOpenness: normalizedOpenness,
  });
  return resolveRecruiterDmOpenness({
    serverValue: data?.recruiterDmOpenness || data?.dmOpenness || normalizedOpenness,
  });
};

const getRecruiterDmOpennessLabel = ({ recruiterDmOpenness, isArabic }) => {
  const normalizedOpenness = normalizeRecruiterDmOpenness(recruiterDmOpenness);

  if (normalizedOpenness === RECRUITER_DM_OPENNESS.OPEN_ALL_PLAYERS) {
    return isArabic ? "مفتوح لكل اللاعبين" : "Open to all players";
  }

  if (normalizedOpenness === RECRUITER_DM_OPENNESS.OPEN_VERIFIED_PLAYERS) {
    return isArabic ? "مفتوح للاعبين الموثقين فقط" : "Verified players only";
  }

  return isArabic ? "مغلق حاليا" : "Closed";
};

export {
  RECRUITER_DM_OPENNESS,
  canPlayerInitiateRecruiterChat,
  getRecruiterDirectory,
  getRecruiterDmOpenness,
  getRecruiterDmOpennessLabel,
  normalizeRecruiterDmOpenness,
  updateRecruiterDmOpenness,
};
