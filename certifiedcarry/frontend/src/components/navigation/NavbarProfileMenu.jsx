import {
  Button,
  Dropdown,
  DropdownItem,
  DropdownMenu,
  DropdownTrigger,
  Switch,
} from "@heroui/react";

const NavbarProfileMenu = ({
  handleLogout,
  isDarkMode,
  language,
  navigate,
  profileImageSrc,
  profileMenuAriaLabel,
  profileMenuLogoutLabel,
  profileMenuProfileLabel,
  profileMenuSettingsLabel,
  profileMenuThemeLabel,
  profileMenuLanguageLabel,
  profileMenuTriggerClass,
  profilePath,
  switchLanguageLabel,
  switchThemeLabel,
  toggleLanguage,
  toggleTheme,
}) => {
  const hasProfileImage = Boolean(profileImageSrc);

  return (
    <Dropdown placement="bottom-end">
      <DropdownTrigger>
        <Button
          type="button"
          variant="light"
          radius="full"
          className={profileMenuTriggerClass}
          aria-label={profileMenuAriaLabel}
          title={profileMenuAriaLabel}
        >
          <img
            src={profileImageSrc}
            alt=""
            aria-hidden="true"
            className={hasProfileImage ? "h-8 w-8 rounded-full object-cover" : "h-7 w-7 object-contain"}
          />
          <span
            aria-hidden="true"
            className={`text-xs font-semibold ${isDarkMode ? "text-[#cae9ea]" : "text-[#273b40]"}`}
          >
            ▾
          </span>
        </Button>
      </DropdownTrigger>
      <DropdownMenu aria-label={profileMenuAriaLabel}>
        <DropdownItem key="profile" onPress={() => navigate(profilePath)}>
          {profileMenuProfileLabel}
        </DropdownItem>

        <DropdownItem key="language" onPress={toggleLanguage} closeOnSelect={false}>
          <div className="flex w-full items-center justify-between gap-3">
            <div className="flex flex-col">
              <span>{profileMenuLanguageLabel}</span>
              <span className="text-[11px] opacity-70">{switchLanguageLabel}</span>
            </div>
            <Switch
              size="sm"
              isSelected={language === "ar"}
              className="pointer-events-none"
              aria-label={switchLanguageLabel}
            />
          </div>
        </DropdownItem>

        <DropdownItem key="theme" onPress={toggleTheme} closeOnSelect={false}>
          <div className="flex w-full items-center justify-between gap-3">
            <div className="flex flex-col">
              <span>{profileMenuThemeLabel}</span>
              <span className="text-[11px] opacity-70">{switchThemeLabel}</span>
            </div>
            <Switch
              size="sm"
              isSelected={isDarkMode}
              className="pointer-events-none"
              aria-label={switchThemeLabel}
            />
          </div>
        </DropdownItem>

        <DropdownItem key="settings" onPress={() => navigate("/settings")}>
          {profileMenuSettingsLabel}
        </DropdownItem>

        <DropdownItem key="logout" color="danger" onPress={handleLogout}>
          {profileMenuLogoutLabel}
        </DropdownItem>
      </DropdownMenu>
    </Dropdown>
  );
};

export default NavbarProfileMenu;
