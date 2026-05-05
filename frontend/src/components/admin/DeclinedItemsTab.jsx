import { Chip } from "@heroui/react";
import Card from "../Card";
import { ADMIN_CARD_BG, ADMIN_CARD_CLASS } from "./adminShared";

const DeclinedItemsTab = ({ declinedItems, isArabic, onOpenImageViewer }) => {
  if (declinedItems.length === 0) {
    return (
      <Card bg={ADMIN_CARD_BG} className={ADMIN_CARD_CLASS}>
        <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/85">
          {isArabic ? "لا يوجد عناصر مرفوضة حاليا." : "No declined records yet."}
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {declinedItems.map((item) => (
        <Card key={item.id} bg={ADMIN_CARD_BG} className={ADMIN_CARD_CLASS}>
          <div className="mb-2 flex items-center justify-between gap-2">
            <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
              {item.fullName}
            </h2>
            <div className="flex items-center gap-2">
              <Chip
                variant="flat"
                className={
                  item.type === "RECRUITER"
                    ? "bg-[#208c8c]/25 text-[#1d1d1d] dark:text-[#cae9ea]"
                    : "bg-red-500/20 text-red-700 dark:text-red-300"
                }
              >
                {item.type === "RECRUITER"
                  ? isArabic
                    ? "مستقطب"
                    : "Scout"
                  : isArabic
                    ? "رتبة"
                    : "Rank"}
              </Chip>
              {item.type === "RANK" && item.editedAfterDecline ? (
                <Chip variant="flat" className="bg-amber-500/20 text-amber-700 dark:text-amber-300">
                  {isArabic ? "تم التعديل" : "Edited"}
                </Chip>
              ) : null}
            </div>
          </div>

          {item.type === "RECRUITER" ? (
            <>
              <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
                {item.organizationName}
              </p>
              <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
                {item.email}
              </p>
            </>
          ) : (
            <>
              <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
                {isArabic ? "اسم اللاعب" : "Gamertag"}: {item.username}
              </p>
              <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
                {item.game} • {item.claimedRank}
              </p>
              {item.game !== "Overwatch 2" ? (
                <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
                  {item.ratingLabel}: {item.ratingValue ?? "-"}
                </p>
              ) : null}
              {item.proofImage ? (
                <>
                  <button
                    type="button"
                    className="mt-3 block w-full"
                    onClick={() => onOpenImageViewer(item.proofImage, item.username)}
                  >
                    <img
                      src={item.proofImage}
                      alt={isArabic ? "إثبات الرتبة" : "Rank proof"}
                      className="max-h-56 w-full cursor-zoom-in rounded-md border border-[#3c4748]/35 object-contain transition hover:border-[#208c8c]/70"
                    />
                  </button>
                  <p className="mt-1 text-xs text-[#273b40]/75 dark:text-[#cae9ea]/70">
                    {isArabic ? "انقر الصورة للتكبير والتحريك" : "Click image to zoom and pan"}
                  </p>
                </>
              ) : null}
            </>
          )}

          {item.declineReason ? (
            <p className="mt-3 rounded-md border border-red-300/50 bg-red-50/70 px-3 py-2 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-200">
              {isArabic ? "سبب الرفض" : "Decline Reason"}: {item.declineReason}
            </p>
          ) : null}
        </Card>
      ))}
    </div>
  );
};

export default DeclinedItemsTab;
