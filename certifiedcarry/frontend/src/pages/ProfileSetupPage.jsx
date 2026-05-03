import { useEffect, useMemo, useState } from "react";
import { Button, Input, Select, SelectItem, Textarea } from "@heroui/react";
import { useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import Card from "../components/Card";
import FormErrorAlert from "../components/FormErrorAlert";
import GameAndRankSection from "../components/profileSetup/GameAndRankSection";
import ProfileDetailsSection from "../components/profileSetup/ProfileDetailsSection";
import ProfileMediaSection from "../components/profileSetup/ProfileMediaSection";
import {
  OUTLINE_BUTTON_CLASS,
  PRIMARY_BUTTON_CLASS,
  PROFILE_SETUP_CARD_BG,
} from "../components/profileSetup/profileSetupShared";
import ThemedLoader from "../components/ThemedLoader";
import { getAuthenticatedHomePath } from "../components/routeGuardUtils";
import {
  GAME_OPTIONS,
  ROCKET_LEAGUE_MODES,
  GAME_RANK_OPTIONS,
  GAME_ROLE_OPTIONS,
  getRatingLabel,
  isRoleDrivenGame,
  normalizeGameName,
  requiresRatingValue,
} from "../constants/gameConfig";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";
import { logBackgroundAuthIssue } from "../services/auth/shared";
import {
  getPlayerProfileDetails,
  submitPlayerProfile,
  uploadPlayerProfileAsset,
} from "../services/playerService";
import { getSelectValue } from "../utils/selectHelpers";

const buildEmptyRocketLeagueModeDetails = () =>
  ROCKET_LEAGUE_MODES.reduce((accumulator, mode) => {
    accumulator[mode] = { rank: "", ratingValue: "" };
    return accumulator;
  }, {});

const ProfileSetupPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { isArabic } = useLanguage();

  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isUploadingProfileImage, setIsUploadingProfileImage] = useState(false);
  const [isUploadingProofImage, setIsUploadingProofImage] = useState(false);
  const [submitError, setSubmitError] = useState("");

  const [game, setGame] = useState("");
  const [rank, setRank] = useState("");
  const [ratingValue, setRatingValue] = useState("");
  const [rocketLeagueModes, setRocketLeagueModes] = useState([]);
  const [rocketLeagueModeDetails, setRocketLeagueModeDetails] = useState(
    buildEmptyRocketLeagueModeDetails,
  );
  const [primaryRocketLeagueMode, setPrimaryRocketLeagueMode] = useState("");
  const [inGameRoles, setInGameRoles] = useState([]);
  const [profileImage, setProfileImage] = useState("");
  const [proofImage, setProofImage] = useState("");
  const [bio, setBio] = useState("");
  const [clipsUrl, setClipsUrl] = useState("");
  const [isWithTeam, setIsWithTeam] = useState(false);
  const [teamName, setTeamName] = useState("");

  useEffect(() => {
    const loadProfile = async () => {
      if (!user || user.role !== "PLAYER") {
        navigate(getAuthenticatedHomePath(user), { replace: true });
        return;
      }

      setIsLoading(true);
      try {
        const profile = await getPlayerProfileDetails(user.id);

        if (!profile) {
          return;
        }

        setGame(normalizeGameName(profile.game || ""));
        setRank(profile.rank || "");
        setRatingValue(
          profile.ratingValue !== null && profile.ratingValue !== undefined
            ? String(profile.ratingValue)
            : "",
        );
        const existingRoleList = Array.isArray(profile.inGameRoles)
          ? profile.inGameRoles.filter(Boolean)
          : profile.inGameRole
            ? [profile.inGameRole]
            : [];
        setInGameRoles(existingRoleList);
        setProfileImage(profile.profileImage || "");
        setProofImage(profile.proofImage || "");
        setBio(profile.bio || "");
        setClipsUrl(profile.clipsUrl || "");
        setIsWithTeam(Boolean(profile.isWithTeam));
        setTeamName(profile.teamName || "");

        const normalizedGame = normalizeGameName(profile.game || "");
        if (normalizedGame === "Rocket League") {
          const existingModes = Array.isArray(profile.rocketLeagueModes)
            ? profile.rocketLeagueModes
            : [];
          const selectedModes = existingModes
            .map((entry) => String(entry?.mode || ""))
            .filter((mode) => ROCKET_LEAGUE_MODES.includes(mode));

          const nextModeDetails = buildEmptyRocketLeagueModeDetails();
          existingModes.forEach((entry) => {
            const mode = String(entry?.mode || "");
            if (!ROCKET_LEAGUE_MODES.includes(mode)) {
              return;
            }

            nextModeDetails[mode] = {
              rank: String(entry?.rank || ""),
              ratingValue:
                entry?.ratingValue !== null && entry?.ratingValue !== undefined
                  ? String(entry.ratingValue)
                  : "",
            };
          });

          setRocketLeagueModes(selectedModes);
          setRocketLeagueModeDetails(nextModeDetails);
          setPrimaryRocketLeagueMode(
            selectedModes.includes(profile.primaryRocketLeagueMode)
              ? profile.primaryRocketLeagueMode
              : selectedModes[0] || "",
          );
        } else {
          setRocketLeagueModes([]);
          setRocketLeagueModeDetails(buildEmptyRocketLeagueModeDetails());
          setPrimaryRocketLeagueMode("");
        }
      } catch (error) {
        logBackgroundAuthIssue("Unable to load player profile setup:", error, "error");
        toast.error(isArabic ? "تعذر تحميل بيانات الملف" : "Unable to load profile setup.");
      } finally {
        setIsLoading(false);
      }
    };

    loadProfile();
  }, [isArabic, navigate, user]);

  const availableRanks = useMemo(() => GAME_RANK_OPTIONS[game] || [], [game]);
  const availableRoles = useMemo(() => GAME_ROLE_OPTIONS[game] || [], [game]);

  const isRocketLeague = game === "Rocket League";
  const needsRole = !isRocketLeague && isRoleDrivenGame(game);
  const needsRating = !isRocketLeague && requiresRatingValue(game, rank);
  const ratingLabel = getRatingLabel(game, rank);
  const labelClass = `mb-2 text-sm font-semibold text-[#1d1d1d] dark:text-[#cae9ea] ${isArabic ? "text-right" : "text-left"}`;
  const requiredAsteriskClass = "text-red-500";

  const handleGameChange = (keys) => {
    const selectedGame = getSelectValue(keys);
    setGame(selectedGame);
    setRank("");
    setInGameRoles([]);
    setRatingValue("");
    setRocketLeagueModes([]);
    setRocketLeagueModeDetails(buildEmptyRocketLeagueModeDetails());
    setPrimaryRocketLeagueMode("");
  };

  const handleRocketLeagueModeToggle = (mode) => {
    setRocketLeagueModes((currentModes) => {
      if (currentModes.includes(mode)) {
        const nextModes = currentModes.filter((item) => item !== mode);
        if (primaryRocketLeagueMode === mode) {
          setPrimaryRocketLeagueMode(nextModes[0] || "");
        }
        return nextModes;
      }

      const nextModes = [...currentModes, mode];
      if (!primaryRocketLeagueMode) {
        setPrimaryRocketLeagueMode(mode);
      }
      return nextModes;
    });
  };

  const handleRocketLeagueModeRankChange = (mode, keys) => {
    const selectedRank = getSelectValue(keys);
    setRocketLeagueModeDetails((currentDetails) => ({
      ...currentDetails,
      [mode]: {
        ...currentDetails[mode],
        rank: selectedRank,
      },
    }));
  };

  const handleRocketLeagueModeRatingChange = (mode, nextValue) => {
    setRocketLeagueModeDetails((currentDetails) => ({
      ...currentDetails,
      [mode]: {
        ...currentDetails[mode],
        ratingValue: nextValue,
      },
    }));
  };

  const handleFileUpload = async (event) => {
    const inputElement = event.target;
    const file = inputElement.files?.[0];
    if (!file) {
      return;
    }

    setIsUploadingProofImage(true);

    try {
      const uploadResult = await uploadPlayerProfileAsset({
        file,
        assetType: "PROOF_IMAGE",
      });

      setProofImage(uploadResult.url);

      if (uploadResult.storage === "inline") {
        toast.info(
          isArabic
            ? "تم حفظ الصورة محليا لهذا الإصدار المؤقت."
            : "Image stored with local fallback for this environment.",
        );
      }
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : isArabic
            ? "فشل رفع الصورة"
            : "Unable to upload this image.";
      toast.error(message);
    } finally {
      setIsUploadingProofImage(false);
      inputElement.value = "";
    }
  };

  const handleProfileImageUpload = async (event) => {
    const inputElement = event.target;
    const file = inputElement.files?.[0];
    if (!file) {
      return;
    }

    setIsUploadingProfileImage(true);

    try {
      const uploadResult = await uploadPlayerProfileAsset({
        file,
        assetType: "PROFILE_IMAGE",
      });

      setProfileImage(uploadResult.url);

      if (uploadResult.storage === "inline") {
        toast.info(
          isArabic
            ? "تم حفظ الصورة الشخصية محليا لهذا الإصدار المؤقت."
            : "Profile image stored with local fallback for this environment.",
        );
      }
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : isArabic
            ? "فشل رفع الصورة الشخصية"
            : "Unable to upload this profile image.";
      toast.error(message);
    } finally {
      setIsUploadingProfileImage(false);
      inputElement.value = "";
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitError("");

    if (isUploadingProfileImage || isUploadingProofImage) {
      const message =
        isArabic
          ? "يرجى انتظار اكتمال رفع الصور قبل الإرسال."
          : "Please wait for image uploads to finish before submitting.";
      setSubmitError(message);
      toast.error(message);
      return;
    }

    setIsSubmitting(true);

    try {
      let finalRank = rank;
      let finalRatingValue = ratingValue;
      let finalInGameRoles = [...inGameRoles];
      let finalInGameRole = finalInGameRoles[0] || "";
      let finalRocketLeagueModes = [];
      let finalPrimaryRocketLeagueMode = "";

      if (isRocketLeague) {
        if (!rocketLeagueModes.length) {
          throw new Error(
            isArabic
              ? "يرجى اختيار نمط لعب واحد على الأقل في Rocket League."
              : "Please choose at least one Rocket League game mode.",
          );
        }

        finalRocketLeagueModes = rocketLeagueModes.map((mode) => ({
          mode,
          rank: rocketLeagueModeDetails[mode]?.rank || "",
          ratingValue: rocketLeagueModeDetails[mode]?.ratingValue || "",
        }));

        const incompleteMode = finalRocketLeagueModes.find(
          (modeEntry) => !modeEntry.rank || !String(modeEntry.ratingValue || "").trim(),
        );

        if (incompleteMode) {
          throw new Error(
            isArabic
              ? `يرجى إدخال الرتبة وMMR لنمط ${incompleteMode.mode}.`
              : `Please provide rank and MMR for ${incompleteMode.mode}.`,
          );
        }

        finalPrimaryRocketLeagueMode = rocketLeagueModes.includes(primaryRocketLeagueMode)
          ? primaryRocketLeagueMode
          : rocketLeagueModes[0];

        const primaryModeEntry =
          finalRocketLeagueModes.find(
            (modeEntry) => modeEntry.mode === finalPrimaryRocketLeagueMode,
          ) || finalRocketLeagueModes[0];

        finalRank = primaryModeEntry.rank;
        finalRatingValue = primaryModeEntry.ratingValue;
        finalInGameRoles = [];
        finalInGameRole = "";
      } else if (isRoleDrivenGame(game) && finalInGameRoles.length === 0) {
        throw new Error(
          isArabic
            ? "يرجى اختيار دور واحد على الأقل داخل اللعبة."
            : "Please select at least one in-game role.",
        );
      }

      if (isWithTeam && !String(teamName || "").trim()) {
        throw new Error(isArabic ? "يرجى إدخال اسم الفريق." : "Please provide your team name.");
      }

      const submissionResult = await submitPlayerProfile({
        user,
        profileInput: {
          game,
          rank: finalRank,
          ratingValue: finalRatingValue,
          inGameRoles: finalInGameRoles,
          inGameRole: finalInGameRole,
          rocketLeagueModes: finalRocketLeagueModes,
          primaryRocketLeagueMode: finalPrimaryRocketLeagueMode,
          profileImage,
          proofImage,
          isWithTeam,
          teamName,
          bio,
          clipsUrl,
        },
      });

      toast.success(
        submissionResult?.requiresVerification
          ? isArabic
            ? "تم إرسال ملفك للتحقق من الرتبة"
            : "Profile submitted. Rank verification is now pending admin approval."
          : isArabic
            ? "تم تحديث الملف بنجاح دون الحاجة لإعادة التحقق من الرتبة"
            : "Profile updated successfully. Rank verification status is unchanged.",
      );

      globalThis.dispatchEvent(
        new CustomEvent("cc:player-profile-updated", {
          detail: {
            userId: String(user.id),
          },
        }),
      );

      navigate(getAuthenticatedHomePath(user), { replace: true });
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : isArabic
            ? "تعذر حفظ بيانات الملف"
            : "Unable to submit profile right now.";
      setSubmitError(message);
      toast.error(message);
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <section className="bg-transparent px-4 py-12">
        <div className="m-auto flex max-w-4xl justify-center py-20">
          <ThemedLoader />
        </div>
      </section>
    );
  }

  return (
    <section className="bg-transparent px-4 py-12">
      <div className="m-auto max-w-4xl">
        <Card bg={PROFILE_SETUP_CARD_BG} className="shadow-xl backdrop-blur-sm">
          <h1 className="cc-title-section mb-2 text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "إعداد الملف الشخصي" : "Profile Setup"}
          </h1>
          <p className="cc-body-muted mb-6 text-[#273b40] dark:text-[#cae9ea]/80">
            {isArabic
              ? "أكمل ملفك لتظهر في لوحة المتصدرين بعد الموافقة على إثبات الرتبة."
              : "Complete your profile to appear on the leaderboard after rank verification."}
          </p>

          <FormErrorAlert message={submitError} />

          <form onSubmit={handleSubmit}>
            <ProfileMediaSection
              handleProfileImageUpload={handleProfileImageUpload}
              isArabic={isArabic}
              isSubmitting={isSubmitting}
              isUploadingProfileImage={isUploadingProfileImage}
              labelClass={labelClass}
              onRemoveProfileImage={() => setProfileImage("")}
              profileImage={profileImage}
            />

            <GameAndRankSection
              availableRanks={availableRanks}
              game={game}
              handleGameChange={handleGameChange}
              handleRocketLeagueModeRankChange={handleRocketLeagueModeRankChange}
              handleRocketLeagueModeRatingChange={handleRocketLeagueModeRatingChange}
              handleRocketLeagueModeToggle={handleRocketLeagueModeToggle}
              isArabic={isArabic}
              isRocketLeague={isRocketLeague}
              labelClass={labelClass}
              needsRating={needsRating}
              primaryRocketLeagueMode={primaryRocketLeagueMode}
              rank={rank}
              ratingLabel={ratingLabel}
              ratingValue={ratingValue}
              requiredAsteriskClass={requiredAsteriskClass}
              rocketLeagueModeDetails={rocketLeagueModeDetails}
              rocketLeagueModes={rocketLeagueModes}
              setPrimaryRocketLeagueMode={setPrimaryRocketLeagueMode}
              setRank={setRank}
              setRatingValue={setRatingValue}
            />

            <ProfileDetailsSection
              availableRoles={availableRoles}
              bio={bio}
              clipsUrl={clipsUrl}
              game={game}
              handleFileUpload={handleFileUpload}
              inGameRoles={inGameRoles}
              isArabic={isArabic}
              isRoleDrivenGame={isRoleDrivenGame}
              isSubmitting={isSubmitting}
              isUploadingProofImage={isUploadingProofImage}
              isWithTeam={isWithTeam}
              labelClass={labelClass}
              needsRole={needsRole}
              proofImage={proofImage}
              requiredAsteriskClass={requiredAsteriskClass}
              setBio={setBio}
              setClipsUrl={setClipsUrl}
              setInGameRoles={setInGameRoles}
              setIsWithTeam={setIsWithTeam}
              setTeamName={setTeamName}
              teamName={teamName}
            />

            <div className="flex flex-wrap gap-3">
              <Button
                type="button"
                variant="bordered"
                className={OUTLINE_BUTTON_CLASS}
                onPress={() => navigate(getAuthenticatedHomePath(user))}
              >
                {isArabic ? "رجوع" : "Back"}
              </Button>
              <Button
                type="submit"
                className={PRIMARY_BUTTON_CLASS}
                isLoading={isSubmitting}
                isDisabled={isSubmitting || isUploadingProfileImage || isUploadingProofImage}
              >
                {isSubmitting
                  ? isArabic
                    ? "جار الإرسال..."
                    : "Submitting..."
                  : isArabic
                    ? "إرسال للتحقق"
                    : "Submit for Verification"}
              </Button>
            </div>
          </form>
        </Card>
      </div>
    </section>
  );
};

export default ProfileSetupPage;
