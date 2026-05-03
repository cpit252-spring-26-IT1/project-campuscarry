import { Button, NavbarItem } from "@heroui/react";

const NavbarUtilityButtons = ({
  eToADarkMode,
  eToALightMode,
  iconToggleButtonClass,
  isDarkMode,
  lightModeIcon,
  darkModeIcon,
  switchLanguageLabel,
  switchThemeLabel,
  toggleLanguage,
  toggleTheme,
}) => {
  return (
    <>
      <NavbarItem>
        <Button
          type="button"
          variant="light"
          radius="sm"
          onPress={toggleLanguage}
          className={iconToggleButtonClass}
          aria-label={switchLanguageLabel}
          title={switchLanguageLabel}
        >
          <img
            src={isDarkMode ? eToADarkMode : eToALightMode}
            alt=""
            aria-hidden="true"
            className="h-7 w-7"
          />
        </Button>
      </NavbarItem>

      <NavbarItem>
        <Button
          type="button"
          variant="light"
          radius="sm"
          onPress={toggleTheme}
          className={iconToggleButtonClass}
          aria-label={switchThemeLabel}
          title={switchThemeLabel}
        >
          <img
            src={isDarkMode ? lightModeIcon : darkModeIcon}
            alt=""
            aria-hidden="true"
            className="h-6 w-6"
          />
        </Button>
      </NavbarItem>
    </>
  );
};

export default NavbarUtilityButtons;
