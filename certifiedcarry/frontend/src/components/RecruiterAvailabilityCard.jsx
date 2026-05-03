import { Button, Chip } from "@heroui/react";
import { Link } from "react-router-dom";
import Card from "./Card";
import { getRecruiterDmOpennessLabel } from "../services/recruiterService";

const RecruiterAvailabilityCard = ({ isArabic, recruiters }) => {
  const hasRecruiters = Array.isArray(recruiters) && recruiters.length > 0;

  return (
    <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
      <h3 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
        {isArabic ? "المستقطبون المتاحون للمراسلة" : "Recruiters Accepting DMs"}
      </h3>
      <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
        {isArabic
          ? "ابدأ تواصلا مباشرا مع المستقطبين المتاحين حاليا."
          : "Start direct conversations with recruiters who are currently open."}
      </p>

      {hasRecruiters ? (
        <div className="mt-4 space-y-2">
          {recruiters.slice(0, 4).map((recruiter) => (
            <div key={recruiter.id} className="rounded-md border border-[#3c4748]/30 px-3 py-2">
              <div className="flex items-center justify-between gap-2">
                <p className="text-sm font-semibold text-[#1d1d1d] dark:text-[#cae9ea]">
                  {recruiter.fullName}
                </p>
                <Chip
                  size="sm"
                  variant="flat"
                  className="bg-[#208c8c]/18 text-[#1d1d1d] dark:text-[#cae9ea]"
                >
                  {getRecruiterDmOpennessLabel({
                    recruiterDmOpenness: recruiter.recruiterDmOpenness,
                    isArabic,
                  })}
                </Chip>
              </div>
              {recruiter.organizationName ? (
                <p className="mt-1 text-xs text-[#273b40]/80 dark:text-[#cae9ea]/75">
                  {recruiter.organizationName}
                </p>
              ) : null}
              <Button
                as={Link}
                to={`/chats?with=${encodeURIComponent(String(recruiter.id))}`}
                size="sm"
                className="cc-button-text mt-3 bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
              >
                {isArabic ? "بدء محادثة" : "Start Chat"}
              </Button>
            </div>
          ))}
        </div>
      ) : (
        <p className="cc-body-muted mt-4 text-[#273b40] dark:text-[#cae9ea]/85">
          {isArabic
            ? "لا يوجد مستقطبون متاحون حاليا. تحقق لاحقا أو أكمل ملفك لزيادة فرص التواصل."
            : "No recruiters are open right now. Check back later or complete your profile to improve eligibility."}
        </p>
      )}
    </Card>
  );
};

export default RecruiterAvailabilityCard;
