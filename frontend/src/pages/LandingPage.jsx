import { Link } from "react-router-dom";
import { Button } from "@heroui/react";
import { FaCrosshairs } from "react-icons/fa";
import Card from "../components/Card";
import GlitchText from "../components/GlitchText";
import { howItWorks, supportedGames } from "../constants/landingContent";
import { getLandingThemeConfig } from "../constants/landingThemeConfig";
import useLanguage from "../hooks/useLanguage";
import useTheme from "../hooks/useTheme";

const LandingPage = () => {
  const { isDarkMode } = useTheme();
  const { t, isArabic } = useLanguage();
  const themeConfig = getLandingThemeConfig(isDarkMode);
  const cardTextAlignClass = isArabic ? "text-right" : "text-left";
  const heroSubheadlineClass = `${themeConfig.heroSubheadlineClass} ${
    isArabic ? "font-extrabold" : ""
  }`;
  const howItWorksIconClass = isDarkMode
    ? "mb-3 inline-flex h-10 w-10 items-center justify-center rounded-lg bg-[#208c8c]/20 text-[#cae9ea]"
    : "mb-3 inline-flex h-10 w-10 items-center justify-center rounded-lg border border-[#3c4748]/35 bg-[#cae9ea]/70 text-[#1d1d1d]";

  const translatedSteps = howItWorks.map((step, index) => {
    const localizedStep = t(`landing.steps.${index}`);
    return {
      ...step,
      title: localizedStep?.title || step.title,
      body: localizedStep?.body || step.body,
    };
  });

  return (
    <div
      className={`relative isolate min-h-screen overflow-hidden ${themeConfig.pageBackgroundClass}`}
    >
      <div className={themeConfig.overlayClass} aria-hidden="true" />
      <div className="relative z-20">
        <section className="pt-44 md:pt-48">
          <div className="mx-auto flex max-w-7xl flex-col items-center px-4 pb-10 text-center sm:px-6 md:pb-14 lg:px-8">
            <h1 className={themeConfig.brandClass}>
              <GlitchText enableOnHover={true}>CertifiedCarry™</GlitchText>
            </h1>
            <h2 className={themeConfig.heroHeadlineClass}>{t("landing.heroHeadline")}</h2>
            <p className={heroSubheadlineClass}>{t("landing.heroSubheadline")}</p>
            <div className="mt-7 flex flex-col justify-center gap-3 sm:flex-row">
              <Button
                as={Link}
                to="/register?role=PLAYER"
                radius="md"
                size="lg"
                className={themeConfig.heroPrimaryButtonClass}
              >
                {t("landing.getScouted")}
              </Button>
              <Button
                as={Link}
                to="/register?role=RECRUITER"
                radius="md"
                size="lg"
                className={themeConfig.heroSecondaryButtonClass}
              >
                {t("landing.findTalent")}
              </Button>
            </div>
          </div>
        </section>

        <section className="pb-10">
          <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
            <Card
              bg={themeConfig.cardBg}
              className={`${themeConfig.cardClass} ${cardTextAlignClass}`}
            >
              <h3 className={`${themeConfig.sectionTitleClass} ${cardTextAlignClass}`}>
                {t("landing.problemTitle")}
              </h3>
              <p className={`mt-3 text-lg ${themeConfig.sectionBodyClass} ${cardTextAlignClass}`}>
                {t("landing.problemBody")}
              </p>
            </Card>
          </div>
        </section>

        <section className="pb-12">
          <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
            <h3 className={`text-center ${themeConfig.sectionTitleClass}`}>
              {t("landing.howItWorks")}
            </h3>
            <div className="mt-6 grid grid-cols-1 gap-4 md:grid-cols-3">
              {translatedSteps.map((step) => (
                <Card
                  key={step.title}
                  bg={themeConfig.cardBg}
                  className={`${themeConfig.cardClass} ${cardTextAlignClass}`}
                >
                  <div className={howItWorksIconClass}>
                    <step.Icon />
                  </div>
                  <h4 className={`${themeConfig.cardTitleClass} ${cardTextAlignClass}`}>
                    {step.title}
                  </h4>
                  <p className={`${themeConfig.cardBodyClass} ${cardTextAlignClass}`}>
                    {step.body}
                  </p>
                </Card>
              ))}
            </div>
          </div>
        </section>

        <section className="pb-12">
          <div className="mx-auto max-w-7xl px-4 text-center sm:px-6 lg:px-8">
            <h3 className={themeConfig.sectionTitleClass}>{t("landing.supportedGames")}</h3>
            <div className="mt-6 flex flex-wrap items-center justify-center gap-8">
              {supportedGames.map((game) => (
                <div key={game} className={themeConfig.gamePillClass}>
                  {game}
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="pb-12">
          <div className="mx-auto max-w-5xl px-4 sm:px-6 lg:px-8">
            <Card
              bg={themeConfig.cardBg}
              className={`${themeConfig.cardClass} ${cardTextAlignClass}`}
            >
              <h3 className={`${themeConfig.sectionTitleClass} ${cardTextAlignClass}`}>
                {t("landing.forScouts")}
              </h3>
              <p className={`mt-3 text-lg ${themeConfig.sectionBodyClass} ${cardTextAlignClass}`}>
                {t("landing.forScoutsBody")}
              </p>
              <div className="mt-5">
                <Button
                  as={Link}
                  to="/register?role=RECRUITER"
                  radius="md"
                  startContent={<FaCrosshairs />}
                  className={themeConfig.scoutCtaButtonClass}
                >
                  {t("landing.joinAsScout")}
                </Button>
              </div>
            </Card>
          </div>
        </section>

        <section className="pb-12">
          <div className="mx-auto max-w-4xl px-4 text-center sm:px-6 lg:px-8">
            <Card
              bg={themeConfig.cardBg}
              className={`${themeConfig.cardClass} ${cardTextAlignClass}`}
            >
              <h3 className={`${themeConfig.sectionTitleClass} ${cardTextAlignClass}`}>
                {t("landing.readyToBeFound")}
              </h3>
              <p className={`mt-3 ${themeConfig.sectionBodyClass} ${cardTextAlignClass}`}>
                {t("landing.readyToBeFoundBody")}
              </p>
              <div className="mt-5">
                <Button
                  as={Link}
                  to="/register?role=PLAYER"
                  radius="md"
                  size="lg"
                  className={themeConfig.finalCtaButtonClass}
                >
                  {t("landing.createProfile")}
                </Button>
              </div>
            </Card>
          </div>
        </section>
      </div>
    </div>
  );
};

export default LandingPage;
