import { Link } from "react-router-dom";
import Card from "../components/Card";
import useLanguage from "../hooks/useLanguage";
import {
  LEGAL_CONTACT_EMAIL,
  LEGAL_EFFECTIVE_DATE,
  LEGAL_PRIVACY_VERSION,
} from "../constants/legalConfig";

const cookieCopy = {
  en: {
    title: "Cookie Policy",
    intro:
      "This Cookie Policy explains how CertifiedCarry uses cookies and similar technologies on the platform.",
    sections: [
      {
        heading: "1. What We Use",
        points: [
          "Essential cookies and local storage used for authentication state, language preference, and security features.",
          "Technical storage used to maintain session consistency and improve core functionality.",
        ],
      },
      {
        heading: "2. Cookie Categories and Retention",
        points: [
          "Strictly necessary cookies: required for login sessions, security, and core platform operation.",
          "Preference cookies/local storage: used for language or interface preferences and may persist between sessions.",
          "Retention periods vary by purpose; session cookies expire when the browser closes, while preference items may persist until manually cleared or replaced.",
        ],
      },
      {
        heading: "3. Why We Use Them",
        points: [
          "To keep users signed in securely where applicable.",
          "To remember product preferences such as language and display settings.",
          "To protect platform integrity and prevent abuse.",
        ],
      },
      {
        heading: "4. Third-Party Technologies",
        points: [
          "Where third-party services are required to run the platform, they may set necessary technical cookies.",
          "CertifiedCarry does not use advertising trackers for selling user behavior data.",
        ],
      },
      {
        heading: "5. Consent and Choice",
        points: [
          "We use essential technologies necessary to provide the service.",
          "If we introduce non-essential cookies (such as analytics or marketing cookies), we will request consent where required by law before activation.",
        ],
      },
      {
        heading: "6. Managing Cookies",
        points: [
          "You can manage or delete cookies from your browser settings.",
          "Disabling essential cookies may impact account access or platform functionality.",
        ],
      },
      {
        heading: "7. Updates and Contact",
        points: [
          "We may update this Cookie Policy and publish the latest effective date and version.",
          `For cookie or privacy questions, contact: ${LEGAL_CONTACT_EMAIL}.`,
        ],
      },
    ],
    termsLabel: "Terms of Service",
    privacyLabel: "Privacy Policy",
  },
  ar: {
    title: "سياسة ملفات تعريف الارتباط",
    intro:
      "توضح هذه السياسة كيف يستخدم CertifiedCarry ملفات تعريف الارتباط والتقنيات المشابهة داخل المنصة.",
    sections: [
      {
        heading: "1. ما الذي نستخدمه",
        points: [
          "ملفات تعريف ارتباط أساسية وتخزين محلي لحالة تسجيل الدخول وتفضيلات اللغة ومتطلبات الأمان.",
          "تخزين تقني للحفاظ على استقرار الجلسة وتحسين وظائف المنصة الأساسية.",
        ],
      },
      {
        heading: "2. فئات ملفات الارتباط وفترات الاحتفاظ",
        points: [
          "ملفات ضرورية للغاية: لازمة لجلسة تسجيل الدخول والأمان وتشغيل المنصة الأساسية.",
          "ملفات التفضيلات أو التخزين المحلي: لتذكر اللغة وإعدادات الواجهة وقد تستمر بين الجلسات.",
          "تختلف مدة الاحتفاظ حسب الغرض؛ ملفات الجلسة تنتهي عادة عند إغلاق المتصفح، بينما قد تستمر عناصر التفضيلات حتى حذفها أو استبدالها.",
        ],
      },
      {
        heading: "3. لماذا نستخدمها",
        points: [
          "للمحافظة على تسجيل الدخول الآمن عند الحاجة.",
          "لتذكر التفضيلات مثل اللغة وإعدادات العرض.",
          "لحماية المنصة ومنع إساءة الاستخدام.",
        ],
      },
      {
        heading: "4. تقنيات الطرف الثالث",
        points: [
          "عند الحاجة إلى خدمات طرف ثالث لتشغيل المنصة، قد يتم استخدام ملفات تقنية ضرورية.",
          "لا يستخدم CertifiedCarry أدوات تتبع إعلانية لبيع بيانات سلوك المستخدم.",
        ],
      },
      {
        heading: "5. الموافقة وخيارات المستخدم",
        points: [
          "نستخدم التقنيات الأساسية اللازمة لتقديم الخدمة.",
          "إذا تم إدخال ملفات غير أساسية (مثل التحليلات أو التسويق)، سنطلب الموافقة عند وجوب ذلك نظاما قبل التفعيل.",
        ],
      },
      {
        heading: "6. إدارة ملفات الارتباط",
        points: [
          "يمكنك إدارة ملفات الارتباط أو حذفها من إعدادات المتصفح.",
          "تعطيل الملفات الأساسية قد يؤثر على الدخول أو وظائف المنصة.",
        ],
      },
      {
        heading: "7. التحديثات والتواصل",
        points: [
          "قد نقوم بتحديث هذه السياسة مع نشر أحدث تاريخ سريان وإصدار.",
          `للاستفسارات المتعلقة بملفات الارتباط أو الخصوصية: ${LEGAL_CONTACT_EMAIL}.`,
        ],
      },
    ],
    termsLabel: "شروط الخدمة",
    privacyLabel: "سياسة الخصوصية",
  },
};

const CookiePolicyPage = () => {
  const { isArabic, t } = useLanguage();
  const copy = isArabic ? cookieCopy.ar : cookieCopy.en;

  return (
    <section className="bg-transparent px-4 pb-16 pt-40 text-[#1d1d1d] dark:text-[#cae9ea] sm:px-6 lg:px-8">
      <div className="mx-auto max-w-5xl">
        <Card
          bg="bg-[#f4fbfb]/90 dark:bg-[#273b40]/65"
          className="rounded-2xl border border-[#3c4748]/45 p-6 shadow-xl backdrop-blur-sm md:p-8"
        >
          <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">{copy.title}</h1>
          <p className="cc-body-muted mt-2 text-[#3c4748] dark:text-[#cae9ea]/80">
            {t("legal.effectiveDateLabel", isArabic ? "تاريخ السريان" : "Effective Date")}:{" "}
            {LEGAL_EFFECTIVE_DATE}
          </p>
          <p className="cc-body-muted mt-1 text-[#3c4748] dark:text-[#cae9ea]/80">
            {t("legal.versionLabel", isArabic ? "الإصدار" : "Version")}: {LEGAL_PRIVACY_VERSION}
          </p>

          <p className="cc-body-lead mt-6 text-[#1d1d1d] dark:text-[#cae9ea]">{copy.intro}</p>

          <div className="mt-8 space-y-6">
            {copy.sections.map((section) => (
              <article key={section.heading}>
                <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                  {section.heading}
                </h2>
                <ul className="mt-3 list-disc space-y-2 ps-6 text-[#273b40] dark:text-[#cae9ea]/90">
                  {section.points.map((point) => (
                    <li key={point}>{point}</li>
                  ))}
                </ul>
              </article>
            ))}
          </div>

          <div className="mt-10 border-t border-[#3c4748]/25 pt-4">
            <div className="flex flex-wrap items-center gap-3">
              <Link
                to="/terms"
                className="cc-body-muted font-semibold text-[#208c8c] hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
              >
                {copy.termsLabel}
              </Link>
              <Link
                to="/privacy"
                className="cc-body-muted font-semibold text-[#208c8c] hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
              >
                {copy.privacyLabel}
              </Link>
            </div>
          </div>
        </Card>
      </div>
    </section>
  );
};

export default CookiePolicyPage;
