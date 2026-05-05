import { useEffect, useMemo, useState } from "react";
import {
  Button,
  Chip,
  Input,
  Modal,
  ModalBody,
  ModalContent,
  ModalFooter,
  ModalHeader,
  Switch,
  Tab,
  Tabs,
} from "@heroui/react";
import { Link, useNavigate } from "react-router-dom";
import { toast } from "react-toastify";
import Card from "../components/Card";
import DmOpennessSelector from "../components/DmOpennessSelector";
import useAuth from "../hooks/useAuth";
import useLanguage from "../hooks/useLanguage";
import useTheme from "../hooks/useTheme";
import { getPlayerProfileDetails, updatePlayerChatPreference } from "../services/playerService";
import {
  RECRUITER_DM_OPENNESS,
  getRecruiterDmOpenness,
  getRecruiterDmOpennessLabel,
  updateRecruiterDmOpenness,
} from "../services/recruiterService";
import { trackUiEvent } from "../services/analyticsService";

const TAB_PROFILE = "profile";
const TAB_CHAT_PRIVACY = "chat-privacy";
const TAB_APPEARANCE = "appearance";
const TAB_ACCOUNT = "account";

const getRoleLabel = (role, isArabic) => {
  switch (String(role || "").toUpperCase()) {
    case "PLAYER":
      return isArabic ? "لاعب" : "Player";
    case "RECRUITER":
      return isArabic ? "مستقطب" : "Recruiter";
    case "ADMIN":
      return isArabic ? "مشرف" : "Admin";
    default:
      return isArabic ? "غير معروف" : "Unknown";
  }
};

const SettingsPage = () => {
  const navigate = useNavigate();
  const { user, deleteCurrentUserAccount, logoutUser } = useAuth();
  const { isArabic, language, setLanguage } = useLanguage();
  const { isDarkMode, setTheme } = useTheme();

  const [activeTab, setActiveTab] = useState(TAB_PROFILE);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [deleteConfirmText, setDeleteConfirmText] = useState("");
  const [isDeletingAccount, setIsDeletingAccount] = useState(false);
  const [isDeletingCurrentSession, setIsDeletingCurrentSession] = useState(false);

  const [playerProfile, setPlayerProfile] = useState(null);
  const [isLoadingPlayerProfile, setIsLoadingPlayerProfile] = useState(false);
  const [isUpdatingPlayerChatPreference, setIsUpdatingPlayerChatPreference] = useState(false);

  const [recruiterDmOpenness, setRecruiterDmOpenness] = useState(RECRUITER_DM_OPENNESS.CLOSED);
  const [isUpdatingRecruiterDmOpenness, setIsUpdatingRecruiterDmOpenness] = useState(false);

  const isPlayer = user?.role === "PLAYER";
  const isRecruiter = user?.role === "RECRUITER";
  const canSelfDelete = user?.role === "PLAYER" || user?.role === "RECRUITER";
  const deleteConfirmKeyword = "DELETE";
  const canDeleteAccount = deleteConfirmText.trim().toUpperCase() === deleteConfirmKeyword;

  const displayEmail = useMemo(() => {
    const candidate =
      user?.email || user?.personalEmail || user?.workEmail || user?.contactEmail || "";

    const normalized = String(candidate).trim();
    return normalized || "-";
  }, [user]);

  useEffect(() => {
    let isCancelled = false;

    const loadSettingsData = async () => {
      if (isPlayer) {
        setIsLoadingPlayerProfile(true);

        try {
          const profile = await getPlayerProfileDetails(user.id);
          if (!isCancelled) {
            setPlayerProfile(profile);
          }
        } catch {
          if (!isCancelled) {
            setPlayerProfile(null);
          }
        } finally {
          if (!isCancelled) {
            setIsLoadingPlayerProfile(false);
          }
        }
      }

      if (isRecruiter) {
        try {
          const openness = await getRecruiterDmOpenness({ userId: user.id });
          if (!isCancelled) {
            setRecruiterDmOpenness(openness);
          }
        } catch {
          if (!isCancelled) {
            setRecruiterDmOpenness(RECRUITER_DM_OPENNESS.CLOSED);
          }
        }
      }
    };

    void loadSettingsData();

    return () => {
      isCancelled = true;
    };
  }, [isPlayer, isRecruiter, user.id]);

  const handleUpdatePlayerChatPreference = async (nextPreference) => {
    if (!isPlayer || !playerProfile || isUpdatingPlayerChatPreference) {
      return;
    }

    const currentPreference = playerProfile.allowPlayerChats !== false;
    if (nextPreference === currentPreference) {
      return;
    }

    setIsUpdatingPlayerChatPreference(true);

    try {
      const updatedPreference = await updatePlayerChatPreference({
        userId: user.id,
        allowPlayerChats: nextPreference,
      });

      setPlayerProfile((currentProfile) =>
        currentProfile
          ? {
              ...currentProfile,
              allowPlayerChats: updatedPreference,
            }
          : currentProfile,
      );

      toast.success(
        updatedPreference
          ? isArabic
            ? "تم تفعيل استقبال رسائل اللاعبين."
            : "You now accept chats from players."
          : isArabic
            ? "تم تقييد المحادثات لتكون من الكشّافين فقط."
            : "Only scouts can chat with you now.",
      );
      trackUiEvent("player_chat_preference_updated", {
        allowPlayerChats: updatedPreference,
      });
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : isArabic
            ? "تعذر تحديث إعدادات المحادثة."
            : "Unable to update chat preference.";
      toast.error(message);
    } finally {
      setIsUpdatingPlayerChatPreference(false);
    }
  };

  const handleUpdateRecruiterDmOpenness = async (selectedOpenness) => {
    if (!isRecruiter || isUpdatingRecruiterDmOpenness) {
      return;
    }

    const normalizedSelection = String(selectedOpenness || "")
      .trim()
      .toUpperCase();
    if (!normalizedSelection || normalizedSelection === recruiterDmOpenness) {
      return;
    }

    setIsUpdatingRecruiterDmOpenness(true);

    try {
      const updatedOpenness = await updateRecruiterDmOpenness({
        userId: user.id,
        recruiterDmOpenness: normalizedSelection,
      });

      setRecruiterDmOpenness(updatedOpenness);

      toast.success(
        isArabic ? "تم تحديث إعداد فتح الرسائل المباشرة." : "Direct message openness updated.",
      );
      trackUiEvent("recruiter_dm_openness_updated", {
        recruiterDmOpenness: updatedOpenness,
      });
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : isArabic
            ? "تعذر تحديث إعداد الرسائل المباشرة."
            : "Unable to update direct message openness.";
      toast.error(message);
    } finally {
      setIsUpdatingRecruiterDmOpenness(false);
    }
  };

  const logoutCurrentSession = () => {
    if (isDeletingCurrentSession) {
      return;
    }

    setIsDeletingCurrentSession(true);
    logoutUser();
    navigate("/", { replace: true });
  };

  const openDeleteModal = () => {
    setDeleteConfirmText("");
    setIsDeleteModalOpen(true);
  };

  const closeDeleteModal = () => {
    if (isDeletingAccount) {
      return;
    }

    setIsDeleteModalOpen(false);
    setDeleteConfirmText("");
  };

  const handleDeleteMyAccount = async () => {
    if (!canSelfDelete || !canDeleteAccount || isDeletingAccount) {
      return;
    }

    setIsDeletingAccount(true);

    try {
      await deleteCurrentUserAccount();
      setIsDeleteModalOpen(false);
      setDeleteConfirmText("");
      toast.success(
        isArabic
          ? "تم حذف حسابك وجميع البيانات المرتبطة به."
          : "Your account and related data were deleted.",
      );
      navigate("/", { replace: true });
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : isArabic
            ? "تعذر حذف الحساب."
            : "Unable to delete account.";
      toast.error(message);
    } finally {
      setIsDeletingAccount(false);
    }
  };

  const currentDmOpennessLabel = getRecruiterDmOpennessLabel({
    recruiterDmOpenness,
    isArabic,
  });

  return (
    <section className="bg-transparent px-4 py-10">
      <div className="m-auto max-w-5xl">
        <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="mb-6 shadow-xl backdrop-blur-sm">
          <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "الإعدادات" : "Settings"}
          </h1>
          <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
            {isArabic
              ? "إدارة تفضيلات الحساب والخصوصية من مكان واحد."
              : "Manage account preferences and privacy from one place."}
          </p>
        </Card>

        <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
          <Tabs
            selectedKey={activeTab}
            onSelectionChange={(key) => setActiveTab(String(key))}
            classNames={{
              tabList: "bg-[#cae9ea]/40 dark:bg-[#1d1d1d]/35",
              cursor: "bg-[#208c8c]",
              tab: "cc-body-muted",
              tabContent: "group-data-[selected=true]:text-[#cae9ea]",
            }}
          >
            <Tab key={TAB_PROFILE} title={isArabic ? "الملف" : "Profile"} />
            <Tab key={TAB_CHAT_PRIVACY} title={isArabic ? "الدردشة والخصوصية" : "Chat & Privacy"} />
            <Tab
              key={TAB_APPEARANCE}
              title={isArabic ? "اللغة والمظهر" : "Language & Appearance"}
            />
            <Tab key={TAB_ACCOUNT} title={isArabic ? "الحساب" : "Account"} />
          </Tabs>

          {activeTab === TAB_PROFILE ? (
            <div className="mt-5 space-y-5">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <Input
                  label={isArabic ? "الاسم" : "Name"}
                  labelPlacement="outside"
                  value={user?.fullName || "-"}
                  isReadOnly
                />
                <Input
                  label={isArabic ? "الدور" : "Role"}
                  labelPlacement="outside"
                  value={getRoleLabel(user?.role, isArabic)}
                  isReadOnly
                />
                <Input
                  label={isArabic ? "البريد الإلكتروني" : "Email"}
                  labelPlacement="outside"
                  value={displayEmail}
                  isReadOnly
                />
              </div>

              {isPlayer ? (
                <div className="rounded-xl border border-[#3c4748]/25 bg-[#eaf4f4]/70 p-4 dark:border-[#3c4748]/65 dark:bg-[#1d1d1d]/30">
                  <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                    {isArabic ? "الملف العام" : "Public Profile"}
                  </h2>
                  <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
                    {isLoadingPlayerProfile
                      ? isArabic
                        ? "جار تحميل بيانات الملف..."
                        : "Loading profile details..."
                      : isArabic
                        ? `حالة التحقق الحالية: ${String(playerProfile?.rankVerificationStatus || "NOT_SUBMITTED")}`
                        : `Current verification status: ${String(playerProfile?.rankVerificationStatus || "NOT_SUBMITTED")}`}
                  </p>

                  <div className="mt-4 flex flex-wrap gap-3">
                    <Button
                      as={Link}
                      to="/profile-setup"
                      className="cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                    >
                      {isArabic ? "تحديث ملف اللاعب" : "Update Player Profile"}
                    </Button>
                    <Button
                      as={Link}
                      to={`/players/${user.id}`}
                      variant="bordered"
                      className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
                    >
                      {isArabic ? "عرض الملف العام" : "Open Public Profile"}
                    </Button>
                  </div>
                </div>
              ) : null}

              {isRecruiter ? (
                <div className="rounded-xl border border-[#3c4748]/25 bg-[#eaf4f4]/70 p-4 dark:border-[#3c4748]/65 dark:bg-[#1d1d1d]/30">
                  <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                    {isArabic ? "الملف المهني" : "Professional Profile"}
                  </h2>
                  <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
                    {isArabic
                      ? "يمكن للاعبين رؤية جهة العمل وحالة فتح الرسائل المباشرة الخاصة بك."
                      : "Players can see your organization and direct-message openness status."}
                  </p>
                </div>
              ) : null}
            </div>
          ) : null}

          {activeTab === TAB_CHAT_PRIVACY ? (
            <div className="mt-5 space-y-5">
              {isPlayer ? (
                <div className="rounded-xl border border-[#3c4748]/25 bg-[#eaf4f4]/70 p-4 dark:border-[#3c4748]/65 dark:bg-[#1d1d1d]/30">
                  <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                    {isArabic ? "خصوصية اللاعب" : "Player Privacy"}
                  </h2>
                  <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
                    {playerProfile?.allowPlayerChats === false
                      ? isArabic
                        ? "حاليا تستقبل الرسائل المباشرة من الكشّافين فقط."
                        : "You currently accept direct messages from scouts only."
                      : isArabic
                        ? "حاليا تستقبل الرسائل المباشرة من اللاعبين والكشّافين."
                        : "You currently accept direct messages from players and scouts."}
                  </p>

                  <Switch
                    className="mt-4"
                    isSelected={playerProfile?.allowPlayerChats !== false}
                    isDisabled={isUpdatingPlayerChatPreference || isLoadingPlayerProfile}
                    onValueChange={handleUpdatePlayerChatPreference}
                  >
                    {isArabic ? "السماح برسائل اللاعبين" : "Allow Player Chats"}
                  </Switch>
                </div>
              ) : null}

              {isRecruiter ? (
                <div className="rounded-xl border border-[#3c4748]/25 bg-[#eaf4f4]/70 p-4 dark:border-[#3c4748]/65 dark:bg-[#1d1d1d]/30">
                  <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                    {isArabic ? "فتح الرسائل المباشرة" : "Direct Message Openness"}
                  </h2>
                  <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
                    {isArabic
                      ? "تحكم بمن يمكنه بدء محادثة مباشرة معك من اللاعبين."
                      : "Control which players can start direct chats with you."}
                  </p>

                  <div className="mt-3">
                    <Chip
                      variant="flat"
                      className="bg-[#208c8c]/18 text-[#1d1d1d] dark:text-[#cae9ea]"
                    >
                      {isArabic ? "الحالة الحالية:" : "Current state:"} {currentDmOpennessLabel}
                    </Chip>
                  </div>

                  <div className="mt-4">
                    <DmOpennessSelector
                      isArabic={isArabic}
                      value={recruiterDmOpenness}
                      isDisabled={isUpdatingRecruiterDmOpenness}
                      onChange={handleUpdateRecruiterDmOpenness}
                    />
                  </div>
                </div>
              ) : null}

              {!isPlayer && !isRecruiter ? (
                <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
                  {isArabic
                    ? "إعدادات الدردشة والخصوصية المتقدمة متاحة حاليا لحسابات اللاعبين والمستقطبين."
                    : "Advanced chat and privacy controls are currently available for player and recruiter accounts."}
                </p>
              ) : null}
            </div>
          ) : null}

          {activeTab === TAB_APPEARANCE ? (
            <div className="mt-5 space-y-5">
              <div className="rounded-xl border border-[#3c4748]/25 bg-[#eaf4f4]/70 p-4 dark:border-[#3c4748]/65 dark:bg-[#1d1d1d]/30">
                <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                  {isArabic ? "اللغة" : "Language"}
                </h2>
                <div className="mt-4 flex flex-wrap gap-3">
                  <Button
                    type="button"
                    variant={language === "ar" ? "solid" : "bordered"}
                    className={
                      language === "ar"
                        ? "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                        : "cc-button-text border-[#3c4748]/50 text-[#1d1d1d] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
                    }
                    onPress={() => setLanguage("ar")}
                  >
                    العربية
                  </Button>
                  <Button
                    type="button"
                    variant={language === "en" ? "solid" : "bordered"}
                    className={
                      language === "en"
                        ? "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                        : "cc-button-text border-[#3c4748]/50 text-[#1d1d1d] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
                    }
                    onPress={() => setLanguage("en")}
                  >
                    English
                  </Button>
                </div>
              </div>

              <div className="rounded-xl border border-[#3c4748]/25 bg-[#eaf4f4]/70 p-4 dark:border-[#3c4748]/65 dark:bg-[#1d1d1d]/30">
                <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                  {isArabic ? "المظهر" : "Appearance"}
                </h2>
                <div className="mt-4 flex flex-wrap gap-3">
                  <Button
                    type="button"
                    variant={!isDarkMode ? "solid" : "bordered"}
                    className={
                      !isDarkMode
                        ? "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                        : "cc-button-text border-[#3c4748]/50 text-[#1d1d1d] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
                    }
                    onPress={() => setTheme("light")}
                  >
                    {isArabic ? "فاتح" : "Light"}
                  </Button>
                  <Button
                    type="button"
                    variant={isDarkMode ? "solid" : "bordered"}
                    className={
                      isDarkMode
                        ? "cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                        : "cc-button-text border-[#3c4748]/50 text-[#1d1d1d] hover:bg-[#cae9ea]/55 dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30"
                    }
                    onPress={() => setTheme("dark")}
                  >
                    {isArabic ? "داكن" : "Dark"}
                  </Button>
                </div>
              </div>
            </div>
          ) : null}

          {activeTab === TAB_ACCOUNT ? (
            <div className="mt-5 space-y-6">
              <div className="grid grid-cols-1 gap-4 md:grid-cols-2">
                <Input
                  label={isArabic ? "الاسم" : "Name"}
                  labelPlacement="outside"
                  value={user?.fullName || "-"}
                  isReadOnly
                />
                <Input
                  label={isArabic ? "الدور" : "Role"}
                  labelPlacement="outside"
                  value={getRoleLabel(user?.role, isArabic)}
                  isReadOnly
                />
                <Input
                  label={isArabic ? "البريد الإلكتروني" : "Email"}
                  labelPlacement="outside"
                  value={displayEmail}
                  isReadOnly
                />
              </div>

              <div className="rounded-xl border border-[#3c4748]/25 bg-[#eaf4f4]/70 p-4 dark:border-[#3c4748]/65 dark:bg-[#1d1d1d]/30">
                <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                  {isArabic ? "إدارة الجلسة" : "Session Controls"}
                </h2>
                <p className="cc-body-muted mt-2 text-[#273b40] dark:text-[#cae9ea]/85">
                  {isArabic
                    ? "يمكنك تسجيل الخروج من الجلسة الحالية في أي وقت."
                    : "You can sign out of the current session at any time."}
                </p>
                <Button
                  className="cc-button-text mt-4 bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                  onPress={logoutCurrentSession}
                  isLoading={isDeletingCurrentSession}
                  isDisabled={isDeletingCurrentSession}
                >
                  {isArabic ? "تسجيل الخروج" : "Sign Out"}
                </Button>
              </div>

              <div className="rounded-xl border border-red-500/35 bg-[#fff4f4]/90 p-4 dark:bg-[#3a1f26]/45">
                <details>
                  <summary className="cursor-pointer list-none text-lg font-semibold text-[#7a1626] dark:text-[#ffd2d8]">
                    {isArabic ? "منطقة حساسة" : "Danger Zone"}
                  </summary>

                  {canSelfDelete ? (
                    <>
                      <p className="cc-body-muted mt-3 text-[#5a232d] dark:text-[#ffdce1]/85">
                        {isArabic
                          ? "حذف الحساب نهائي، ويشمل جميع البيانات المرتبطة به. لا يمكن التراجع بعد التأكيد."
                          : "Account deletion is permanent and removes related data. This action cannot be undone."}
                      </p>
                      <Button
                        className="cc-button-text mt-5 bg-red-600 text-white hover:bg-red-700"
                        onPress={openDeleteModal}
                      >
                        {isArabic ? "حذف حسابي" : "Delete My Account"}
                      </Button>
                    </>
                  ) : (
                    <p className="cc-body-muted mt-3 text-[#5a232d] dark:text-[#ffdce1]/85">
                      {isArabic
                        ? "حذف الحساب الذاتي متاح حاليا لحسابات اللاعبين والمستقطبين فقط."
                        : "Self account deletion is currently available for player and recruiter accounts only."}
                    </p>
                  )}
                </details>
              </div>
            </div>
          ) : null}
        </Card>
      </div>

      <Modal
        isOpen={isDeleteModalOpen}
        onOpenChange={(isOpen) => {
          if (!isOpen) {
            closeDeleteModal();
          }
        }}
        placement="top-center"
        backdrop="blur"
      >
        <ModalContent>
          <ModalHeader className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
            {isArabic ? "تأكيد حذف حسابك" : "Confirm Account Deletion"}
          </ModalHeader>
          <ModalBody>
            <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic
                ? "سيتم حذف حسابك وجميع البيانات المرتبطة به نهائيا. للتأكيد، اكتب DELETE في الحقل أدناه."
                : "Your account and related data will be deleted permanently. To confirm, type DELETE below."}
            </p>

            <Input
              type="text"
              value={deleteConfirmText}
              onValueChange={setDeleteConfirmText}
              placeholder={isArabic ? "اكتب DELETE للتأكيد" : "Type DELETE to confirm"}
              autoComplete="off"
            />
          </ModalBody>
          <ModalFooter>
            <Button
              variant="bordered"
              className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]"
              onPress={closeDeleteModal}
              isDisabled={isDeletingAccount}
            >
              {isArabic ? "إلغاء" : "Cancel"}
            </Button>
            <Button
              className="cc-button-text bg-red-600 text-white hover:bg-red-700"
              onPress={handleDeleteMyAccount}
              isLoading={isDeletingAccount}
              isDisabled={isDeletingAccount || !canDeleteAccount || !canSelfDelete}
            >
              {isArabic ? "حذف نهائي" : "Delete Permanently"}
            </Button>
          </ModalFooter>
        </ModalContent>
      </Modal>
    </section>
  );
};

export default SettingsPage;
