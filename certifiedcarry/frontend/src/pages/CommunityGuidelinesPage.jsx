import { Link } from "react-router-dom";
import Card from "../components/Card";
import useLanguage from "../hooks/useLanguage";
import { LEGAL_CONTACT_EMAIL } from "../constants/legalConfig";

const guidelinesCopy = {
  en: {
    title: "Community Guidelines",
    intro:
      "CertifiedCarry is built for trusted, professional discovery of Saudi competitive talent. These guidelines apply to all users.",
    sections: [
      {
        heading: "1. Respect and Professional Conduct",
        points: [
          "Treat players, scouts, and admins with respect.",
          "Harassment, threats, hate speech, and discriminatory language are prohibited.",
          "Keep communication relevant to esports, scouting, and competition.",
        ],
      },
      {
        heading: "2. Authentic Profiles and Evidence",
        points: [
          "Use accurate identity and profile information.",
          "Do not submit fake rank screenshots or manipulated evidence.",
          "Do not impersonate organizations or community members.",
        ],
      },
      {
        heading: "3. Messaging and Outreach",
        points: [
          "No spam, mass unsolicited messages, or repeated contact after refusal.",
          "Recruiter outreach must be truthful and connected to legitimate opportunities.",
          "Do not request sensitive personal information unrelated to scouting.",
        ],
      },
      {
        heading: "4. Enforcement",
        points: [
          "Admins may warn, restrict, suspend, or ban accounts for violations.",
          "Severe violations, including fraud or repeated abuse, may result in permanent account removal.",
        ],
      },
      {
        heading: "5. Reporting and Appeals",
        points: [
          `You can report abuse, impersonation, or harmful behavior through support at ${LEGAL_CONTACT_EMAIL}.`,
          "Users subject to moderation action may request a review with relevant context or evidence.",
          "Emergency safety, legal, or fraud situations may trigger immediate restrictions without prior warning.",
        ],
      },
    ],
    termsLabel: "Terms of Service",
    privacyLabel: "Privacy Policy",
  },
  ar: {
    title: "إرشادات المجتمع",
    intro:
      "تم بناء CertifiedCarry لاكتشاف المواهب السعودية بشكل موثوق ومهني. تطبق هذه الإرشادات على جميع المستخدمين.",
    sections: [
      {
        heading: "1. الاحترام والسلوك المهني",
        points: [
          "تعامل باحترام مع اللاعبين والمستقطبين والمشرفين.",
          "يمنع التحرش أو التهديد أو خطاب الكراهية أو العبارات التمييزية.",
          "يجب أن تكون المراسلات مرتبطة بالرياضات الإلكترونية والاستقطاب والمنافسة.",
        ],
      },
      {
        heading: "2. صحة الملفات والأدلة",
        points: [
          "استخدم بيانات تعريف وملف شخصي صحيحة.",
          "يمنع إرسال صور تصنيف مزيفة أو أدلة معدلة.",
          "يمنع انتحال الجهات أو أعضاء المجتمع.",
        ],
      },
      {
        heading: "3. المراسلة والتواصل",
        points: [
          "يمنع الإزعاج أو الرسائل الجماعية غير المرغوبة أو تكرار التواصل بعد الرفض.",
          "يجب أن يكون تواصل المستقطبين صادقا ومرتبطا بفرص حقيقية.",
          "يمنع طلب بيانات شخصية حساسة لا علاقة لها بالاستقطاب.",
        ],
      },
      {
        heading: "4. الإنفاذ",
        points: [
          "يحق للمشرفين توجيه إنذار أو فرض قيود أو إيقاف الحساب أو حظره عند المخالفة.",
          "المخالفات الجسيمة، مثل الاحتيال أو الإساءة المتكررة، قد تؤدي إلى إزالة الحساب نهائيا.",
        ],
      },
      {
        heading: "5. الإبلاغ والتظلمات",
        points: [
          `يمكنك الإبلاغ عن الإساءة أو الانتحال أو السلوك الضار عبر الدعم على ${LEGAL_CONTACT_EMAIL}.`,
          "يمكن للمستخدم الخاضع لإجراء إشرافي طلب مراجعة القرار مع تقديم السياق أو الأدلة ذات الصلة.",
          "في الحالات الأمنية العاجلة أو المتطلبات النظامية أو الاشتباه بالاحتيال، قد يتم فرض قيود فورية دون إنذار مسبق.",
        ],
      },
    ],
    termsLabel: "شروط الخدمة",
    privacyLabel: "سياسة الخصوصية",
  },
};

const CommunityGuidelinesPage = () => {
  const { isArabic } = useLanguage();
  const copy = isArabic ? guidelinesCopy.ar : guidelinesCopy.en;

  return (
    <section className="bg-transparent px-4 pb-16 pt-40 text-[#1d1d1d] dark:text-[#cae9ea] sm:px-6 lg:px-8">
      <div className="mx-auto max-w-5xl">
        <Card
          bg="bg-[#f4fbfb]/90 dark:bg-[#273b40]/65"
          className="rounded-2xl border border-[#3c4748]/45 p-6 shadow-xl backdrop-blur-sm md:p-8"
        >
          <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">{copy.title}</h1>
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

export default CommunityGuidelinesPage;
