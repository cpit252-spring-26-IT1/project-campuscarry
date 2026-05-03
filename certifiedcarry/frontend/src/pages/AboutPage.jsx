import Card from "../components/Card";
import useLanguage from "../hooks/useLanguage";

const aboutCopy = {
  en: {
    title: "About CertifiedCarry",
    body: "CertifiedCarry is the home of Saudi competitive talent. We help players build verified competitive identities and help organizations discover talent with trusted context.",
  },
  ar: {
    title: "عن CertifiedCarry",
    body: "CertifiedCarry هو موطن المواهب التنافسية السعودية. نساعد اللاعبين على بناء هوية تنافسية موثقة ونساعد الجهات على اكتشاف المواهب بسياق موثوق.",
  },
};

const AboutPage = () => {
  const { isArabic } = useLanguage();
  const copy = isArabic ? aboutCopy.ar : aboutCopy.en;

  return (
    <section className="bg-transparent px-4 pb-16 pt-40 text-[#1d1d1d] dark:text-[#cae9ea] sm:px-6 lg:px-8">
      <div className="mx-auto max-w-4xl">
        <Card
          bg="bg-[#f4fbfb]/90 dark:bg-[#273b40]/65"
          className="rounded-2xl border border-[#3c4748]/45 p-6 shadow-xl backdrop-blur-sm md:p-8"
        >
          <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">{copy.title}</h1>
          <p className="cc-body-lead mt-6 text-[#273b40] dark:text-[#cae9ea]/90">{copy.body}</p>
        </Card>
      </div>
    </section>
  );
};

export default AboutPage;
