import { Button, Input } from "@heroui/react";
import { REMOVE_BUTTON_CLASS } from "./profileSetupShared";

const ProfileMediaSection = ({
  handleProfileImageUpload,
  isArabic,
  isSubmitting,
  isUploadingProfileImage,
  labelClass,
  onRemoveProfileImage,
  profileImage,
}) => {
  return (
    <div className="mb-4">
      <p className={labelClass}>
        {isArabic ? "الصورة الشخصية (اختياري)" : "Profile Picture (optional)"}
      </p>
      <Input
        type="file"
        accept="image/*"
        onChange={handleProfileImageUpload}
        isDisabled={isUploadingProfileImage || isSubmitting}
        className="w-full"
      />
      {isUploadingProfileImage ? (
        <p className="mt-2 text-xs text-[#3c4748] dark:text-[#cae9ea]/75">
          {isArabic ? "جار رفع الصورة الشخصية..." : "Uploading profile image..."}
        </p>
      ) : null}
      {profileImage ? (
        <div className="mt-3 flex items-center gap-3">
          <img
            src={profileImage}
            alt={isArabic ? "صورة شخصية" : "Profile preview"}
            className="h-28 w-28 rounded-full border border-[#3c4748]/40 object-cover"
          />
          <Button
            type="button"
            size="sm"
            variant="bordered"
            className={REMOVE_BUTTON_CLASS}
            onPress={onRemoveProfileImage}
          >
            {isArabic ? "إزالة الصورة" : "Remove photo"}
          </Button>
        </div>
      ) : null}
    </div>
  );
};

export default ProfileMediaSection;
