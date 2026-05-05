import { Link } from "react-router-dom";
import Card from "../components/Card";
import useLanguage from "../hooks/useLanguage";
import {
  LEGAL_CONTACT_EMAIL,
  LEGAL_EFFECTIVE_DATE,
  LEGAL_TERMS_VERSION,
} from "../constants/legalConfig";

const termsCopy = {
  en: {
    title: "Terms of Service",
    intro:
      "By creating an account or using CertifiedCarry, you agree to these Terms of Service. If you do not agree, you must not use the platform.",
    sections: [
      {
        heading: "1. Eligibility and Minimum Age",
        points: [
          "You must be at least 13 years old to register or use CertifiedCarry.",
          "You must provide accurate account details and keep your credentials secure.",
          "You are responsible for activity performed through your account.",
          "You must promptly notify us if you suspect unauthorized access to your account.",
        ],
      },
      {
        heading: "2. Account Integrity and Organization Representation",
        points: [
          "Scout and recruiter accounts must represent legitimate organizations.",
          "CertifiedCarry may request verification evidence from scout and recruiter accounts.",
          "Impersonation of users, teams, organizations, or administrators is prohibited.",
          "You must not bypass security controls, scrape restricted data, or intentionally disrupt service availability.",
        ],
      },
      {
        heading: "3. Profiles, Rankings, and Competitive Integrity",
        points: [
          "Players are responsible for truthful rank submissions and profile data.",
          "Submitting fake, manipulated, or misleading rank screenshots is grounds for a permanent ban.",
          "CertifiedCarry may review, approve, reject, or remove leaderboard and profile entries.",
        ],
      },
      {
        heading: "4. Your Content and Platform License",
        points: [
          "You retain ownership of content you upload, including screenshots, profile text, and media.",
          "You grant CertifiedCarry a non-exclusive, worldwide, royalty-free license to host, process, display, and distribute your content for platform operation, moderation, and promotion.",
          "This license ends when your content is deleted, subject to legal retention and backup limitations.",
          "All platform software, trademarks, and service materials (excluding user content) remain the property of CertifiedCarry or its licensors.",
        ],
      },
      {
        heading: "5. Suspension, Removal, and Enforcement",
        points: [
          "CertifiedCarry reserves the right to remove content, suspend accounts, or permanently terminate accounts at its discretion for policy, safety, fraud, or legal reasons.",
          "You may appeal enforcement actions by contacting support.",
          "You may request account deletion, subject to legal and compliance retention obligations.",
        ],
      },
      {
        heading: "6. Service Availability, Outcomes, and Liability",
        points: [
          "CertifiedCarry is provided on an as-is and as-available basis.",
          "We do not guarantee scouting visibility, recruitment outcomes, contracts, or employment opportunities.",
          "To the maximum extent permitted by law, CertifiedCarry is not liable for indirect, incidental, or consequential damages.",
        ],
      },
      {
        heading: "7. Governing Law and Dispute Venue",
        points: [
          "These Terms are governed by the laws and regulations of the Kingdom of Saudi Arabia.",
          "Disputes will be resolved before the competent authorities in the Kingdom of Saudi Arabia unless otherwise required by mandatory law.",
          "In case of interpretation conflicts between language versions, the version required by applicable law and official proceedings will prevail.",
        ],
      },
      {
        heading: "8. Updates and Contact",
        points: [
          "We may update these Terms from time to time and will publish the updated effective date and version.",
          "Material updates may require renewed consent during sign-in or registration.",
          "Where required by law, we will provide reasonable advance notice of material changes.",
          `For legal or policy questions, contact: ${LEGAL_CONTACT_EMAIL}.`,
        ],
      },
    ],
    legalLinksLabel: "Related policies",
    privacyLinkLabel: "Privacy Policy",
    cookiesLinkLabel: "Cookie Policy",
    guidelinesLinkLabel: "Community Guidelines",
  },
  ar: {
    title: "شروط الخدمة",
    intro:
      "عند إنشاء حساب أو استخدام CertifiedCarry فإنك توافق على شروط الخدمة هذه. إذا لم توافق، يجب عليك عدم استخدام المنصة.",
    sections: [
      {
        heading: "1. الأهلية والحد الأدنى للعمر",
        points: [
          "يجب أن يكون عمرك 13 عاما أو أكثر للتسجيل أو استخدام CertifiedCarry.",
          "يجب تقديم بيانات حساب صحيحة والحفاظ على سرية بيانات الدخول.",
          "أنت مسؤول عن جميع الأنشطة التي تتم عبر حسابك.",
          "يجب عليك إبلاغنا فور الاشتباه بأي دخول غير مصرح به إلى حسابك.",
        ],
      },
      {
        heading: "2. سلامة الحساب وتمثيل الجهات",
        points: [
          "يجب أن تمثل حسابات المستقطبين جهات حقيقية ومشروعة.",
          "يحق لـ CertifiedCarry طلب ما يثبت صحة تمثيل الجهة لحسابات المستقطبين.",
          "يمنع انتحال شخصية المستخدمين أو الفرق أو الجهات أو المشرفين.",
          "يمنع تجاوز ضوابط الأمان أو جمع بيانات غير مصرح بها أو تعطيل الخدمة بشكل متعمد.",
        ],
      },
      {
        heading: "3. الملفات والتصنيفات والنزاهة التنافسية",
        points: [
          "اللاعب مسؤول عن صحة بيانات الملف وأدلة التصنيف المرسلة.",
          "رفع صور تصنيف مزيفة أو معدلة أو مضللة يعد سببا للحظر الدائم.",
          "يجوز لـ CertifiedCarry مراجعة أو قبول أو رفض أو إزالة بيانات الملف ولوحة الترتيب.",
        ],
      },
      {
        heading: "4. المحتوى المملوك لك وترخيص المنصة",
        points: [
          "تبقى ملكية المحتوى الذي ترفعه لك، بما في ذلك الصور والنصوص والوسائط.",
          "تمنح CertifiedCarry ترخيصا غير حصري وعالميا ومجانيا لاستخدام المحتوى واستضافته وعرضه لأغراض تشغيل المنصة وإدارتها والترويج لها.",
          "ينتهي هذا الترخيص عند حذف المحتوى مع مراعاة متطلبات الاحتفاظ القانونية والنسخ الاحتياطية.",
          "تظل برمجيات المنصة وعلاماتها ومحتواها الخدمي (باستثناء محتوى المستخدم) مملوكة لـ CertifiedCarry أو للجهات المرخصة لها.",
        ],
      },
      {
        heading: "5. الإيقاف والإزالة وإنفاذ السياسات",
        points: [
          "تحتفظ CertifiedCarry بحق إزالة المحتوى أو إيقاف الحساب أو إنهائه بشكل دائم وفقا لتقديرها عند وجود مخالفة أو احتيال أو مخاطر أمنية أو التزام نظامي.",
          "يمكنك تقديم طلب مراجعة قرار الإيقاف عبر قنوات الدعم.",
          "يمكنك طلب حذف الحساب مع مراعاة متطلبات الاحتفاظ النظامية والامتثال.",
        ],
      },
      {
        heading: "6. توفر الخدمة والنتائج والمسؤولية",
        points: [
          "تقدم المنصة كما هي وحسب التوفر.",
          "لا تضمن CertifiedCarry الظهور في الاستقطاب أو الحصول على عقد أو فرصة عمل.",
          "إلى الحد المسموح به نظاما، لا تتحمل CertifiedCarry المسؤولية عن الأضرار غير المباشرة أو العرضية أو التبعية.",
        ],
      },
      {
        heading: "7. القانون الواجب التطبيق وجهة الفصل",
        points: [
          "تخضع هذه الشروط لأنظمة المملكة العربية السعودية.",
          "يتم الفصل في النزاعات أمام الجهات المختصة في المملكة العربية السعودية ما لم يوجب النظام خلاف ذلك.",
          "عند وجود تعارض في تفسير النسخ اللغوية، تسود النسخة المعتمدة وفق النظام والجهات الرسمية المختصة.",
        ],
      },
      {
        heading: "8. التحديثات والتواصل",
        points: [
          "قد نقوم بتحديث هذه الشروط من وقت لآخر مع نشر تاريخ السريان والإصدار الجديد.",
          "قد تتطلب التحديثات الجوهرية موافقة جديدة أثناء التسجيل أو تسجيل الدخول.",
          "عند طلب النظام، سنقدم إشعارا مسبقا معقولا قبل تطبيق التغييرات الجوهرية.",
          `للاستفسارات القانونية أو التنظيمية: ${LEGAL_CONTACT_EMAIL}.`,
        ],
      },
    ],
    legalLinksLabel: "سياسات ذات صلة",
    privacyLinkLabel: "سياسة الخصوصية",
    cookiesLinkLabel: "سياسة ملفات تعريف الارتباط",
    guidelinesLinkLabel: "إرشادات المجتمع",
  },
};

const TermsPage = () => {
  const { isArabic, t } = useLanguage();
  const copy = isArabic ? termsCopy.ar : termsCopy.en;

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
            {t("legal.versionLabel", isArabic ? "الإصدار" : "Version")}: {LEGAL_TERMS_VERSION}
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
            <p className="cc-body-muted text-[#3c4748] dark:text-[#cae9ea]/75">
              {copy.legalLinksLabel}
            </p>
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <Link
                to="/privacy"
                className="cc-body-muted font-semibold text-[#208c8c] hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
              >
                {copy.privacyLinkLabel}
              </Link>
              <Link
                to="/cookies"
                className="cc-body-muted font-semibold text-[#208c8c] hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
              >
                {copy.cookiesLinkLabel}
              </Link>
              <Link
                to="/community-guidelines"
                className="cc-body-muted font-semibold text-[#208c8c] hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
              >
                {copy.guidelinesLinkLabel}
              </Link>
            </div>
          </div>
        </Card>
      </div>
    </section>
  );
};

export default TermsPage;
