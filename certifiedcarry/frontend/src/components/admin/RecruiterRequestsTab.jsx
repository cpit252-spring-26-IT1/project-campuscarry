import { Button } from "@heroui/react";
import Card from "../Card";
import {
  ADMIN_CARD_BG,
  ADMIN_CARD_CLASS,
  APPROVE_BUTTON_CLASS,
  DECLINE_BUTTON_CLASS,
} from "./adminShared";

const RecruiterRequestsTab = ({
  actionInProgress,
  isArabic,
  onApprove,
  onDecline,
  pendingRecruiters,
}) => {
  if (pendingRecruiters.length === 0) {
    return (
      <Card bg={ADMIN_CARD_BG} className={ADMIN_CARD_CLASS}>
        <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/85">
          {isArabic ? "لا توجد طلبات مستقطبين حاليا." : "No pending scout requests."}
        </p>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {pendingRecruiters.map((recruiter) => {
        const actionKey = `recruiter-${recruiter.id}`;
        const isProcessing = Boolean(actionInProgress[actionKey]);

        return (
          <Card key={recruiter.id} bg={ADMIN_CARD_BG} className={ADMIN_CARD_CLASS}>
            <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
              {recruiter.fullName}
            </h2>
            <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/85">
              {recruiter.organizationName}
            </p>
            <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
              {recruiter.email}
            </p>
            {recruiter.linkedinUrl ? (
              <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
                <a
                  href={recruiter.linkedinUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="text-[#208c8c] underline underline-offset-2 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
                >
                  {isArabic ? "رابط لينكدإن" : "LinkedIn Profile"}
                </a>
              </p>
            ) : null}

            <div className="mt-4 flex flex-wrap gap-2">
              <Button
                size="sm"
                className={APPROVE_BUTTON_CLASS}
                isDisabled={isProcessing}
                isLoading={isProcessing}
                onPress={() => onApprove(recruiter, actionKey)}
              >
                {isArabic ? "موافقة" : "Approve"}
              </Button>
              <Button
                size="sm"
                className={DECLINE_BUTTON_CLASS}
                isDisabled={isProcessing}
                isLoading={isProcessing}
                onPress={() => onDecline(recruiter, actionKey)}
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

export default RecruiterRequestsTab;
