export const getLandingThemeConfig = (isDarkMode) => {
  if (isDarkMode) {
    return {
      pageBackgroundClass: "bg-transparent",
      overlayClass: "absolute inset-0 bg-[#1d1d1d]/18",
      gridColor: "rgb(32, 140, 140)",
      gridMaxOpacity: 0.5,
      gridFlickerChance: 0.58,
      brandClass:
        "font-display text-4xl font-bold leading-tight tracking-tight text-[#cae9ea] sm:text-5xl md:text-6xl",
      sectionTitleClass: "cc-title-section text-[#cae9ea]",
      sectionBodyClass: "text-[#cae9ea]/90",
      heroHeadlineClass: "cc-title-hero mt-6 max-w-4xl text-[#cae9ea] sm:text-4xl md:text-5xl",
      heroSubheadlineClass: "cc-body-lead my-5 max-w-3xl text-[#cae9ea]/90",
      heroPrimaryButtonClass: "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#3c4748]",
      heroSecondaryButtonClass: "cc-button-text bg-[#cae9ea] text-[#1d1d1d] hover:bg-[#cae9ea]/90",
      cardBg: "bg-[#273b40]/55",
      cardClass: "backdrop-blur-sm border-[#3c4748]/70 shadow-xl",
      cardTitleClass: "cc-title-card text-[#cae9ea]",
      cardBodyClass: "cc-body-lead mt-2 text-[#cae9ea]/90",
      gamePillClass:
        "cc-button-text rounded-lg border border-[#208c8c]/50 bg-[#208c8c]/15 px-4 py-3 text-sm text-[#cae9ea]",
      scoutCtaButtonClass: "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#3c4748]",
      statsValueClass: "font-display text-3xl font-bold text-[#cae9ea]",
      statsLabelClass: "cc-body-muted mt-1 text-[#cae9ea]/90",
      finalCtaButtonClass: "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#3c4748]",
    };
  }

  return {
    pageBackgroundClass: "bg-transparent",
    overlayClass: "absolute inset-0 bg-white/4",
    gridColor: "rgb(39, 59, 64)",
    gridMaxOpacity: 0.44,
    gridFlickerChance: 0.87,
    brandClass:
      "font-display text-4xl font-bold leading-tight tracking-tight text-[#1d1d1d] sm:text-5xl md:text-6xl",
    sectionTitleClass: "cc-title-section text-[#1d1d1d]",
    sectionBodyClass: "text-[#273b40]",
    heroHeadlineClass: "cc-title-hero mt-6 max-w-4xl text-[#1d1d1d] sm:text-4xl md:text-5xl",
    heroSubheadlineClass: "cc-body-lead my-5 max-w-3xl font-semibold text-[#1d1d1d]/90",
    heroPrimaryButtonClass: "cc-button-text bg-[#273b40] text-[#cae9ea] hover:bg-[#1d1d1d]",
    heroSecondaryButtonClass: "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]",
    cardBg: "bg-[#f4fbfb]/88",
    cardClass: "backdrop-blur-sm border-[#3c4748]/55 shadow-xl",
    cardTitleClass: "cc-title-card text-[#1d1d1d]",
    cardBodyClass: "cc-body-lead mt-2 text-[#273b40]",
    gamePillClass:
      "cc-button-text rounded-lg border border-[#3c4748]/60 bg-[#f4fbfb]/95 px-4 py-3 text-sm text-[#273b40]",
    scoutCtaButtonClass: "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]",
    statsValueClass: "font-display text-3xl font-bold text-[#1d1d1d]",
    statsLabelClass: "cc-body-muted mt-1 text-[#273b40]",
    finalCtaButtonClass: "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]",
  };
};
