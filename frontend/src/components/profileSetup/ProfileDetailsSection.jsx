import { Button, Input, Select, SelectItem, Textarea } from "@heroui/react";
import { getSelectValues, getSelectValue } from "../../utils/selectHelpers";
import { OUTLINE_BUTTON_CLASS, PRIMARY_BUTTON_CLASS } from "./profileSetupShared";

const ProfileDetailsSection = ({
  availableRoles,
  bio,
  clipsUrl,
  game,
  handleFileUpload,
  inGameRoles,
  isArabic,
  isSubmitting,
  isUploadingProofImage,
  isWithTeam,
  labelClass,
  needsRole,
  proofImage,
  requiredAsteriskClass,
  setBio,
  setClipsUrl,
  setInGameRoles,
  setIsWithTeam,
  setTeamName,
  teamName,
}) => {
  return (
    <>
      {needsRole ? (
        <div className="mb-4">
          <p className={labelClass}>{isArabic ? "الأدوار داخل اللعبة" : "In-game Roles"}</p>
          <Select
            selectionMode="multiple"
            selectedKeys={inGameRoles}
            onSelectionChange={(keys) => setInGameRoles(getSelectValues(keys))}
          >
            {availableRoles.map((option) => (
              <SelectItem key={option}>{option}</SelectItem>
            ))}
          </Select>
        </div>
      ) : null}

      <div className="mb-4">
        <p className={labelClass}>
          {isArabic ? "هل أنت حاليا مع فريق؟" : "Are you currently with a team?"}
        </p>
        <Select
          selectedKeys={[isWithTeam ? "YES" : "NO"]}
          onSelectionChange={(keys) => {
            const selection = getSelectValue(keys) === "YES";
            setIsWithTeam(selection);
            if (!selection) {
              setTeamName("");
            }
          }}
        >
          <SelectItem key="NO">{isArabic ? "لا" : "No"}</SelectItem>
          <SelectItem key="YES">{isArabic ? "نعم" : "Yes"}</SelectItem>
        </Select>
      </div>

      {isWithTeam ? (
        <div className="mb-4">
          <p className={labelClass}>
            {isArabic ? "اسم الفريق" : "Team Name"} <span className={requiredAsteriskClass}>*</span>
          </p>
          <Input
            type="text"
            isRequired
            placeholder={isArabic ? "مثال: Falcons Esports" : "Example: Falcons Esports"}
            value={teamName}
            onValueChange={setTeamName}
          />
        </div>
      ) : null}

      <div className="mb-4">
        <p className={labelClass}>
          {isArabic
            ? game === "Overwatch 2"
              ? "صورة إثبات (اسم اللاعب + الرتبة)"
              : "صورة إثبات (اسم اللاعب + الرتبة + MMR/Skill Rating)"
            : game === "Overwatch 2"
              ? "Proof Screenshot (Gamertag + Rank)"
              : "Proof Screenshot (Gamertag + Rank + MMR/Skill Rating)"}{" "}
          <span className={requiredAsteriskClass}>*</span>
        </p>
        <Input
          type="file"
          accept="image/*"
          onChange={handleFileUpload}
          isDisabled={isUploadingProofImage || isSubmitting}
          className="w-full"
        />
        {isUploadingProofImage ? (
          <p className="mt-2 text-xs text-[#3c4748] dark:text-[#cae9ea]/75">
            {isArabic ? "جار رفع صورة الإثبات..." : "Uploading proof image..."}
          </p>
        ) : null}
        {proofImage ? (
          <img
            src={proofImage}
            alt={isArabic ? "معاينة الصورة" : "Proof preview"}
            className="mt-3 max-h-60 w-full rounded-md border border-[#3c4748]/40 object-contain"
          />
        ) : null}
      </div>

      <div className="mb-4">
        <p className={labelClass}>{isArabic ? "نبذة (اختياري)" : "Bio (optional)"}</p>
        <Textarea value={bio} onValueChange={setBio} minRows={4} />
      </div>

      <div className="mb-6">
        <p className={labelClass}>{isArabic ? "رابط المقاطع (اختياري)" : "Clips URL (optional)"}</p>
        <Input
          type="url"
          placeholder={isArabic ? "https://youtube.com/..." : "https://youtube.com/..."}
          value={clipsUrl}
          onValueChange={setClipsUrl}
        />
      </div>
    </>
  );
};

export default ProfileDetailsSection;
