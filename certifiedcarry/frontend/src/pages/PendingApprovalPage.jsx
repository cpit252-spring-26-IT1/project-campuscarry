import { Link } from "react-router-dom";
import { Button } from "@heroui/react";
import Card from "../components/Card";
import useLanguage from "../hooks/useLanguage";

const PendingApprovalPage = () => {
  const { isArabic } = useLanguage();

  const title = isArabic ? "طلبك قيد المراجعة" : "Scout Approval In Review";
  const body = isArabic
    ? "تم استلام طلب حساب المستقطب الخاص بك. سيتم تفعيل الوصول بمجرد موافقة المشرف."
    : "Your scout account is being reviewed. You will gain access once an admin approves your organization.";
  const note = isArabic
    ? "يمكنك تسجيل الدخول لاحقا للتحقق من حالة الحساب."
    : "You can sign in later to check your account status.";

  return (
    <section className="bg-transparent px-4 py-12">
      <div className="m-auto max-w-3xl">
        <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
          <h1 className="cc-title-section mb-3 text-[#1d1d1d] dark:text-[#cae9ea]">{title}</h1>
          <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/90">{body}</p>
          <p className="cc-body-muted mt-3 text-[#3c4748] dark:text-[#cae9ea]/75">{note}</p>

          <div className="mt-6 flex flex-wrap gap-3">
            <Button
              as={Link}
              to="/login"
              className="cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
            >
              {isArabic ? "الذهاب لتسجيل الدخول" : "Go to Login"}
            </Button>
            <Button
              as={Link}
              to="/"
              variant="bordered"
              className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]"
            >
              {isArabic ? "العودة للرئيسية" : "Back to Landing"}
            </Button>
          </div>
        </Card>
      </div>
    </section>
  );
};

export default PendingApprovalPage;
