import { Link } from "react-router-dom";
import Card from "../components/Card";
import useLanguage from "../hooks/useLanguage";
import {
  LEGAL_CONTACT_EMAIL,
  LEGAL_EFFECTIVE_DATE,
  LEGAL_PRIVACY_VERSION,
} from "../constants/legalConfig";

const privacyCopy = {
  en: {
    title: "Privacy Policy",
    intro:
      "This policy explains how CertifiedCarry collects, uses, stores, and protects personal data in alignment with applicable Saudi data protection requirements, including the Saudi PDPL.",
    sections: [
      {
        heading: "1. Data Controller and Scope",
        points: [
          "CertifiedCarry acts as the data controller for personal data processed through the platform.",
          "This policy applies to account registration, profiles, rank verification, messaging, and support requests.",
          `For privacy requests, contact: ${LEGAL_CONTACT_EMAIL}.`,
        ],
      },
      {
        heading: "2. Data We Collect",
        points: [
          "Identity and account data: full name, email, role, organization name, and recruiter LinkedIn URL.",
          "Player competitive data: game, rank, role, rank screenshot evidence, and optional profile media or bio.",
          "Platform usage data: authentication logs, moderation events, and required technical metadata.",
        ],
      },
      {
        heading: "3. Why We Process Data",
        points: [
          "To create and operate user accounts and platform features.",
          "To verify ranks, support scouting workflows, and moderate misuse.",
          "To meet legal obligations, maintain audit trails, and respond to lawful requests.",
        ],
      },
      {
        heading: "4. Legal Bases for Processing",
        points: [
          "Contractual necessity: providing account and platform services requested by users.",
          "Consent: where required for specific processing activities.",
          "Legal obligation and legitimate interest: fraud prevention, platform security, and compliance.",
        ],
      },
      {
        heading: "5. Sharing and Disclosure",
        points: [
          "We do not sell personal data.",
          "We do not share personal data with third parties except service providers needed to run the platform or where legally required.",
          "Personal data may be disclosed to competent authorities where required by law.",
          "Service provider categories may include cloud hosting, authentication infrastructure, and security or moderation tooling required for platform operation.",
        ],
      },
      {
        heading: "6. Data Retention",
        points: [
          "Active account data is retained while your account remains active.",
          "Inactive account and profile data may be retained for up to 24 months for security, dispute handling, and operational continuity.",
          "Compliance and audit records may be retained for up to 5 years, or longer when required by law.",
          "Where deletion is approved, we will delete or irreversibly anonymize eligible data within a reasonable operational period, subject to legal retention obligations.",
        ],
      },
      {
        heading: "7. Your Rights Under Applicable Law",
        points: [
          "You may request access to your personal data and receive a copy where required by law.",
          "You may request correction, completion, deletion, or restriction of inaccurate or unnecessary data.",
          "You may request account deletion and withdrawal of consent where applicable, subject to legal and compliance limits.",
          "CertifiedCarry does not rely solely on automated decision-making that produces legal or similarly significant effects without human oversight.",
        ],
      },
      {
        heading: "8. Security and International Transfers",
        points: [
          "We apply reasonable technical and organizational safeguards to protect personal data.",
          "No internet service can guarantee absolute security.",
          "If personal data is processed outside Saudi Arabia, we apply safeguards required by applicable law.",
          "If a personal data incident creates a high risk under applicable law, we will handle notification and response in accordance with legal requirements.",
        ],
      },
      {
        heading: "9. Children",
        points: [
          "Users must be 13 years or older.",
          "If we discover data collected in violation of this requirement, we may remove the account and related data.",
        ],
      },
      {
        heading: "10. Data Requests and Contact",
        points: [
          `To request data access, correction, deletion, or other privacy action, contact: ${LEGAL_CONTACT_EMAIL}.`,
          "We may require identity verification before fulfilling sensitive requests.",
          "We will respond within the timelines required by applicable law.",
        ],
      },
    ],
    legalLinksLabel: "Related policies",
    termsLinkLabel: "Terms of Service",
    cookiesLinkLabel: "Cookie Policy",
    guidelinesLinkLabel: "Community Guidelines",
  },
  ar: {
    title: "سياسة الخصوصية",
    intro:
      "توضح هذه السياسة كيف يجمع CertifiedCarry البيانات الشخصية ويستخدمها ويحتفظ بها ويحميها بما يتماشى مع المتطلبات النظامية في المملكة، بما في ذلك نظام حماية البيانات الشخصية السعودي (PDPL).",
    sections: [
      {
        heading: "1. جهة التحكم ونطاق السياسة",
        points: [
          "يعمل CertifiedCarry بصفته الجهة المتحكمة في البيانات الشخصية التي تتم معالجتها عبر المنصة.",
          "تسري هذه السياسة على التسجيل والحسابات والملفات الشخصية والتحقق من التصنيف والمراسلة وطلبات الدعم.",
          `للتواصل بخصوص الخصوصية وطلبات البيانات: ${LEGAL_CONTACT_EMAIL}.`,
        ],
      },
      {
        heading: "2. البيانات التي نجمعها",
        points: [
          "بيانات الهوية والحساب: الاسم الكامل والبريد الإلكتروني والدور واسم الجهة ورابط لينكدإن للمستقطبين.",
          "بيانات اللاعب التنافسية: اللعبة والرتبة والدور وصورة إثبات التصنيف والوسائط أو النبذة الاختيارية.",
          "بيانات الاستخدام التقنية: سجلات الدخول وأحداث الإشراف والبيانات التقنية اللازمة للتشغيل.",
        ],
      },
      {
        heading: "3. أسباب المعالجة",
        points: [
          "لإنشاء الحسابات وتشغيل ميزات المنصة.",
          "للتحقق من التصنيفات ودعم عمليات الاستقطاب والإشراف على إساءة الاستخدام.",
          "للالتزام بالمتطلبات النظامية وحفظ سجلات التدقيق والاستجابة للطلبات النظامية.",
        ],
      },
      {
        heading: "4. الأسس النظامية للمعالجة",
        points: [
          "تنفيذ العلاقة التعاقدية: تقديم خدمات الحساب والمنصة المطلوبة من المستخدم.",
          "الموافقة: عند الحاجة لمهام معالجة محددة.",
          "الالتزام النظامي والمصلحة المشروعة: منع الاحتيال وحماية المنصة والامتثال.",
        ],
      },
      {
        heading: "5. الإفصاح والمشاركة",
        points: [
          "لا نقوم ببيع البيانات الشخصية.",
          "لا نشارك البيانات الشخصية مع أطراف ثالثة إلا مع مزودي خدمة ضروريين لتشغيل المنصة أو عند وجود إلزام نظامي.",
          "قد نفصح عن البيانات للجهات المختصة عند الطلب النظامي.",
          "قد تشمل فئات مزودي الخدمة الاستضافة السحابية وبنية التحقق من الهوية وأدوات الأمان أو الإشراف اللازمة لتشغيل المنصة.",
        ],
      },
      {
        heading: "6. فترات الاحتفاظ بالبيانات",
        points: [
          "تحتفظ بيانات الحساب النشط طوال مدة نشاط الحساب.",
          "قد يتم الاحتفاظ ببيانات الحسابات غير النشطة لمدة تصل إلى 24 شهرا لأغراض الأمان ومعالجة النزاعات واستمرارية التشغيل.",
          "قد يتم الاحتفاظ بسجلات الامتثال والتدقيق لمدة تصل إلى 5 سنوات أو مدة أطول إذا تطلب النظام.",
          "عند الموافقة على طلب الحذف، سنقوم بحذف البيانات المؤهلة أو تحويلها إلى بيانات غير قابلة للتعريف خلال مدة تشغيلية معقولة مع مراعاة متطلبات الاحتفاظ النظامية.",
        ],
      },
      {
        heading: "7. حقوقك النظامية",
        points: [
          "يمكنك طلب الوصول إلى بياناتك الشخصية والحصول على نسخة منها بحسب ما يسمح به النظام.",
          "يمكنك طلب تصحيح البيانات غير الدقيقة أو إكمالها أو حذفها أو تقييد معالجتها عند عدم الحاجة.",
          "يمكنك طلب حذف الحساب وسحب الموافقة عند انطباق ذلك، مع مراعاة حدود الامتثال والاحتفاظ النظامي.",
          "لا يعتمد CertifiedCarry على قرارات آلية بحتة ذات أثر نظامي أو جوهري مماثل دون إشراف بشري مناسب.",
        ],
      },
      {
        heading: "8. الحماية والنقل خارج المملكة",
        points: [
          "نطبق إجراءات تقنية وتنظيمية معقولة لحماية البيانات الشخصية.",
          "لا توجد خدمة رقمية تضمن أمانا مطلقا.",
          "إذا تمت معالجة البيانات خارج المملكة العربية السعودية، فسيتم تطبيق الضمانات المطلوبة نظاما.",
          "إذا وقع حادث بيانات يسبب خطرا مرتفعا وفق النظام، فسيتم التعامل مع الإشعار والاستجابة بحسب المتطلبات النظامية.",
        ],
      },
      {
        heading: "9. القصر",
        points: [
          "يجب أن يكون عمر المستخدم 13 عاما أو أكثر.",
          "إذا تبين لنا جمع بيانات بما يخالف هذا الشرط، قد نقوم بإزالة الحساب والبيانات المرتبطة به.",
        ],
      },
      {
        heading: "10. طلبات البيانات ووسائل التواصل",
        points: [
          `لتقديم طلب وصول أو تصحيح أو حذف أو أي طلب خصوصية آخر: ${LEGAL_CONTACT_EMAIL}.`,
          "قد نطلب التحقق من الهوية قبل تنفيذ الطلبات الحساسة.",
          "سنستجيب خلال المدد النظامية المقررة.",
        ],
      },
    ],
    legalLinksLabel: "سياسات ذات صلة",
    termsLinkLabel: "شروط الخدمة",
    cookiesLinkLabel: "سياسة ملفات تعريف الارتباط",
    guidelinesLinkLabel: "إرشادات المجتمع",
  },
};

const PrivacyPage = () => {
  const { isArabic, t } = useLanguage();
  const copy = isArabic ? privacyCopy.ar : privacyCopy.en;

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
            <p className="cc-body-muted text-[#3c4748] dark:text-[#cae9ea]/75">
              {copy.legalLinksLabel}
            </p>
            <div className="mt-3 flex flex-wrap items-center gap-3">
              <Link
                to="/terms"
                className="cc-body-muted font-semibold text-[#208c8c] hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
              >
                {copy.termsLinkLabel}
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

export default PrivacyPage;
