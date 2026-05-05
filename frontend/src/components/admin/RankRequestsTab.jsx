import { Button, Chip } from "@heroui/react";
import Card from "../Card";
import {
  ADMIN_CARD_BG,
  ADMIN_CARD_CLASS,
  APPROVE_BUTTON_CLASS,
  DECLINE_BUTTON_CLASS,
} from "./adminShared";

const RankRequestsTab = ({
  actionInProgress,
  isArabic,
  onApprove,
  onDecline,
  onOpenImageViewer,
  pendingRanks,
}) => {
  if (pendingRanks.length === 0) {
    return (
      <Card bg={ADMIN_CARD_BG} className={ADMIN_CARD_CLASS}>
        <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/85">
          {isArabic ? "لا توجد طلبات رتب حاليا." : "No pending rank submissions."}
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {pendingRanks.map((submission) => {
        const actionKey = `rank-${submission.id}`;
        const isProcessing = Boolean(actionInProgress[actionKey]);

        return (
          <Card key={submission.id} bg={ADMIN_CARD_BG} className={ADMIN_CARD_CLASS}>
            <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
              <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                {submission.fullName}
              </h2>
              <Chip variant="flat" className="bg-[#208c8c]/25 text-[#1d1d1d] dark:text-[#cae9ea]">
                {submission.game}
              </Chip>
            </div>

            <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic ? "اسم اللاعب" : "Gamertag"}: {submission.username}
            </p>
            <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic ? "الرتبة" : "Rank"}: {submission.claimedRank}
            </p>
            {submission.game !== "Overwatch 2" ? (
              <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
                {submission.ratingLabel}: {submission.ratingValue ?? "-"}
              </p>
            ) : null}
            {(Array.isArray(submission.inGameRoles) && submission.inGameRoles.length) ||
            submission.inGameRole ? (
              <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
                {isArabic ? "الدور" : "Role"}:{" "}
                {Array.isArray(submission.inGameRoles) && submission.inGameRoles.length
                  ? submission.inGameRoles.join(", ")
                  : submission.inGameRole}
              </p>
            ) : null}

            {submission.proofImage ? (
              <>
                <button
                  type="button"
                  className="mt-3 block w-full"
                  onClick={() => onOpenImageViewer(submission.proofImage, submission.username)}
                >
                  <img
                    src={submission.proofImage}
                    alt={isArabic ? "إثبات الرتبة" : "Rank proof"}
                    className="max-h-56 w-full cursor-zoom-in rounded-md border border-[#3c4748]/35 object-contain transition hover:border-[#208c8c]/70"
                  />
                </button>
                <p className="mt-1 text-xs text-[#273b40]/75 dark:text-[#cae9ea]/70">
                  {isArabic ? "انقر الصورة للتكبير والتحريك" : "Click image to zoom and pan"}
                </p>
              </>
            ) : null}

            <div className="mt-4 flex flex-wrap gap-2">
              <Button
                size="sm"
                className={APPROVE_BUTTON_CLASS}
                isDisabled={isProcessing}
                isLoading={isProcessing}
                onPress={() => onApprove(submission, actionKey)}
              >
                {isArabic ? "موافقة" : "Approve"}
              </Button>
              <Button
                size="sm"
                className={DECLINE_BUTTON_CLASS}
                isDisabled={isProcessing}
                isLoading={isProcessing}
                onPress={() => onDecline(submission, actionKey)}
              >
                {isArabic ? "رفض" : "Decline"}
              </Button>
            </div>
          </Card>
        );
      })}
    </div>
  );
};

export default RankRequestsTab;
