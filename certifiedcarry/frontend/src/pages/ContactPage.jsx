import Card from "../components/Card";
import useLanguage from "../hooks/useLanguage";
import { LEGAL_CONTACT_EMAIL } from "../constants/legalConfig";

const contactCopy = {
  en: {
    title: "Contact",
    body: "For support, legal, privacy, or moderation requests, contact us using the email below.",
    label: "Support Email",
  },
  ar: {
    title: "التواصل",
    body: "للدعم أو الاستفسارات القانونية أو طلبات الخصوصية أو الإشراف، تواصل معنا عبر البريد التالي.",
    label: "بريد الدعم",
  },
};

const ContactPage = () => {
  const { isArabic } = useLanguage();
  const copy = isArabic ? contactCopy.ar : contactCopy.en;

  return (
    <section className="bg-transparent px-4 pb-16 pt-40 text-[#1d1d1d] dark:text-[#cae9ea] sm:px-6 lg:px-8">
      <div className="mx-auto max-w-4xl">
        <Card
          bg="bg-[#f4fbfb]/90 dark:bg-[#273b40]/65"
          className="rounded-2xl border border-[#3c4748]/45 p-6 shadow-xl backdrop-blur-sm md:p-8"
        >
          <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">{copy.title}</h1>
          <p className="cc-body-lead mt-6 text-[#273b40] dark:text-[#cae9ea]/90">{copy.body}</p>

          <p className="cc-body-muted mt-6 text-[#3c4748] dark:text-[#cae9ea]/85">{copy.label}</p>
          <a
            href={`mailto:${LEGAL_CONTACT_EMAIL}`}
            className="cc-title-card mt-1 inline-block text-[#208c8c] underline underline-offset-2 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:text-[#208c8c]"
          >
            {LEGAL_CONTACT_EMAIL}
          </a>
        </Card>
      </div>
    </section>
  );
};

export default ContactPage;
