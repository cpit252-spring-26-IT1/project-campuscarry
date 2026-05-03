import { useEffect, useMemo, useState } from "react";
import { Button, Chip } from "@heroui/react";
import { Link, useParams } from "react-router-dom";
import Card from "../components/Card";
import ThemedLoader from "../components/ThemedLoader";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";
import { getPlayerProfileDetails } from "../services/playerService";

const PlayerProfilePage = () => {
  const { playerId } = useParams();
  const { user } = useAuth();
  const { isArabic } = useLanguage();

  const [isLoading, setIsLoading] = useState(true);
  const [profile, setProfile] = useState(null);

  useEffect(() => {
    const loadProfile = async () => {
      setIsLoading(true);
      try {
        const playerProfile = await getPlayerProfileDetails(playerId);
        setProfile(playerProfile);
      } catch (error) {
        console.error("Unable to load player profile:", error);
        setProfile(null);
      } finally {
        setIsLoading(false);
      }
    };

    loadProfile();
  }, [playerId]);

  const verificationStatus = useMemo(() => {
    if (!profile) {
      return { label: "-", className: "bg-[#3c4748]/25 text-[#1d1d1d] dark:text-[#cae9ea]" };
    }

    if (profile.rankVerificationStatus === "APPROVED") {
      return {
        label: isArabic ? "موثق" : "Verified",
        className: "bg-emerald-500/20 text-emerald-700 dark:text-emerald-300",
      };
    }

    if (profile.rankVerificationStatus === "PENDING") {
      return {
        label: isArabic ? "قيد المراجعة" : "Pending Admin Approval",
        className: "bg-amber-500/20 text-amber-700 dark:text-amber-300",
      };
    }

    if (profile.rankVerificationStatus === "DECLINED") {
      return {
        label: isArabic ? "مرفوض" : "Declined",
        className: "bg-red-500/20 text-red-700 dark:text-red-300",
      };
    }

    return {
      label: isArabic ? "غير مكتمل" : "Incomplete",
      className: "bg-[#3c4748]/25 text-[#1d1d1d] dark:text-[#cae9ea]",
    };
  }, [isArabic, profile]);

  const isOwnProfile =
    profile && user?.role === "PLAYER" && String(user.id) === String(profile.userId);
  const canMessageProfile =
    Boolean(profile) &&
    Boolean(user) &&
    String(user.id) !== String(profile?.userId) &&
    (user.role === "RECRUITER" || user.role === "PLAYER" || user.role === "ADMIN");
  const isPlayerMessageBlocked =
    user?.role === "PLAYER" && profile?.allowPlayerChats === false && !isOwnProfile;
  const avatarInitial = (profile?.username || profile?.fullName || "?").trim().charAt(0);
  const rocketLeagueModeEntries =
    profile?.game === "Rocket League" && Array.isArray(profile?.rocketLeagueModes)
      ? profile.rocketLeagueModes
      : [];
  const profileRoles =
    Array.isArray(profile?.inGameRoles) && profile.inGameRoles.length
      ? profile.inGameRoles
      : profile?.inGameRole
        ? [profile.inGameRole]
        : [];

  if (isLoading) {
    return (
      <section className="bg-transparent px-4 py-12">
        <div className="m-auto flex max-w-4xl justify-center py-20">
          <ThemedLoader />
        </div>
      </section>
    );
  }

  if (!profile) {
    return (
      <section className="bg-transparent px-4 py-12">
        <div className="m-auto max-w-3xl">
          <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
            <h1 className="cc-title-section mb-2 text-[#1d1d1d] dark:text-[#cae9ea]">
              {isArabic ? "الملف غير موجود" : "Profile Not Found"}
            </h1>
            <Button
              as={Link}
              to="/leaderboard"
              className="cc-button-text mt-4 bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
            >
              {isArabic ? "العودة للوحة المتصدرين" : "Back to Leaderboard"}
            </Button>
          </Card>
        </div>
      </section>
    );
  }

  return (
    <section className="bg-transparent px-4 py-12">
      <div className="m-auto max-w-4xl">
        <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
          <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
            <div className="flex items-center gap-3">
              <div className="flex h-16 w-16 items-center justify-center overflow-hidden rounded-full border border-[#3c4748]/45 bg-[#cae9ea]/45 dark:bg-[#1d1d1d]/45">
                {profile.profileImage ? (
                  <img
                    src={profile.profileImage}
                    alt={isArabic ? "الصورة الشخصية" : "Profile picture"}
                    className="h-full w-full object-cover"
                  />
                ) : (
                  <span className="text-xl font-bold text-[#273b40] dark:text-[#cae9ea]">
                    {(avatarInitial || "?").toUpperCase()}
                  </span>
                )}
              </div>

              <div>
                <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">
                  {profile.username}
                </h1>
                <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/85">
                  {profile.fullName}
                </p>
              </div>
            </div>
            <Chip radius="sm" variant="flat" className={verificationStatus.className}>
              {verificationStatus.label}
            </Chip>
          </div>

          <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
            <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/90">
              <span className="font-semibold">{isArabic ? "اللعبة" : "Game"}:</span>{" "}
              {profile.game || "-"}
            </p>
            <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/90">
              <span className="font-semibold">{isArabic ? "الرتبة" : "Rank"}:</span>{" "}
              {profile.rank || "-"}
            </p>
            <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/90">
              <span className="font-semibold">{isArabic ? "الفريق" : "Team"}:</span>{" "}
              {profile.isWithTeam ? profile.teamName || "-" : isArabic ? "لا يوجد" : "No team"}
            </p>
            {profileRoles.length ? (
              <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/90">
                <span className="font-semibold">{isArabic ? "الدور" : "Role"}:</span>{" "}
                {profileRoles.join(", ")}
              </p>
            ) : null}
            {profile.game !== "Overwatch 2" ? (
              <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/90">
                <span className="font-semibold">
                  {profile.ratingLabel || (isArabic ? "MMR/تقييم المهارة" : "MMR/Skill")}:
                </span>{" "}
                {profile.ratingValue !== null && profile.ratingValue !== undefined
                  ? profile.ratingValue
                  : "-"}
              </p>
            ) : null}
          </div>

          {rocketLeagueModeEntries.length > 0 ? (
            <div className="mt-5">
              <p className="cc-body-lead font-semibold text-[#273b40] dark:text-[#cae9ea]/90">
                {isArabic ? "تفاصيل أنماط Rocket League" : "Rocket League Mode Details"}
              </p>
              <div className="mt-3 grid grid-cols-1 gap-2 md:grid-cols-3">
                {rocketLeagueModeEntries.map((modeEntry) => (
                  <div
                    key={modeEntry.mode}
                    className="rounded-md border border-[#3c4748]/30 bg-[#cae9ea]/25 px-3 py-2 dark:bg-[#1d1d1d]/35"
                  >
                    <p className="text-sm font-semibold text-[#1d1d1d] dark:text-[#cae9ea]">
                      {modeEntry.mode}
                    </p>
                    <p className="text-xs text-[#273b40] dark:text-[#cae9ea]/85">
                      {modeEntry.rank || "-"}
                    </p>
                    <p className="text-xs text-[#273b40] dark:text-[#cae9ea]/85">
                      MMR: {modeEntry.ratingValue ?? "-"}
                    </p>
                  </div>
                ))}
              </div>
            </div>
          ) : null}

          {profile.bio ? (
            <p className="cc-body-lead mt-5 text-[#273b40] dark:text-[#cae9ea]/90">{profile.bio}</p>
          ) : null}

          <div className="mt-6 flex flex-wrap gap-3">
            {profile.clipsUrl ? (
              <Button
                as="a"
                href={profile.clipsUrl}
                target="_blank"
                rel="noreferrer"
                className="cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
              >
                {isArabic ? "مقاطع اللاعب" : "View Clips"}
              </Button>
            ) : null}

            {canMessageProfile ? (
              <Button
                as={Link}
                to={`/chats?with=${encodeURIComponent(String(profile.userId))}`}
                isDisabled={isPlayerMessageBlocked}
                className="cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
              >
                {isArabic ? "إرسال رسالة" : "Send Message"}
              </Button>
            ) : null}

            {isOwnProfile ? (
              <Button
                as={Link}
                to="/profile-setup"
                variant="bordered"
                className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]"
              >
                {isArabic ? "تحديث الملف" : "Update Profile"}
              </Button>
            ) : null}
          </div>

          {isPlayerMessageBlocked ? (
            <p className="cc-body-muted mt-3 text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic
                ? "هذا اللاعب يستقبل الرسائل من الكشّافين فقط."
                : "This player only accepts chats from scouts."}
            </p>
          ) : null}
        </Card>
      </div>
    </section>
  );
};

export default PlayerProfilePage;
