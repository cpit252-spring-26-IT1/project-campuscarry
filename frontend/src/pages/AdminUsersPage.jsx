import { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Chip,
  Input,
  Modal,
  ModalBody,
  ModalContent,
  ModalFooter,
  ModalHeader,
  Tab,
  Tabs,
} from "@heroui/react";
import { Link } from "react-router-dom";
import { toast } from "react-toastify";
import Card from "../components/Card";
import ThemedLoader from "../components/ThemedLoader";
import useLanguage from "../hooks/useLanguage";
import {
  deleteUserAccount,
  getRegisteredPlayers,
  getRegisteredScouts,
} from "../services/adminService";

const TAB_SCOUTS = "SCOUTS";
const TAB_PLAYERS = "PLAYERS";

const getStatusLabel = (status, isArabic) => {
  const normalizedStatus = String(status || "").toUpperCase();

  if (normalizedStatus === "APPROVED") {
    return isArabic ? "مقبول" : "Approved";
  }

  if (normalizedStatus === "DECLINED") {
    return isArabic ? "مرفوض" : "Declined";
  }

  return isArabic ? "قيد المراجعة" : "Pending";
};

const getStatusChipClass = (status) => {
  const normalizedStatus = String(status || "").toUpperCase();

  if (normalizedStatus === "APPROVED") {
    return "bg-emerald-600/20 text-emerald-800 dark:text-emerald-300";
  }

  if (normalizedStatus === "DECLINED") {
    return "bg-red-600/20 text-red-700 dark:text-red-300";
  }

  return "bg-amber-500/20 text-amber-700 dark:text-amber-300";
};

const formatTimestamp = (value) => {
  if (!value) {
    return "-";
  }

  const parsedDate = new Date(value);
  if (Number.isNaN(parsedDate.getTime())) {
    return "-";
  }

  return parsedDate.toLocaleString();
};

const AdminUsersPage = () => {
  const { isArabic } = useLanguage();

  const [activeTab, setActiveTab] = useState(TAB_SCOUTS);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [scouts, setScouts] = useState([]);
  const [players, setPlayers] = useState([]);
  const [deletingById, setDeletingById] = useState({});
  const [deleteTarget, setDeleteTarget] = useState(null);

  const loadUsers = useCallback(
    async (refresh = false) => {
      if (refresh) {
        setIsRefreshing(true);
      } else {
        setIsLoading(true);
      }

      try {
        const [nextScouts, nextPlayers] = await Promise.all([
          getRegisteredScouts(),
          getRegisteredPlayers(),
        ]);

        setScouts(nextScouts);
        setPlayers(nextPlayers);
      } catch (error) {
        console.error("Failed loading registered users:", error);
        toast.error(isArabic ? "تعذر تحميل قائمة المستخدمين." : "Unable to load registered users.");
      } finally {
        if (refresh) {
          setIsRefreshing(false);
        } else {
          setIsLoading(false);
        }
      }
    },
    [isArabic],
  );

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const normalizedSearch = searchQuery.trim().toLowerCase();

  const filteredScouts = useMemo(() => {
    if (!normalizedSearch) {
      return scouts;
    }

    return scouts.filter((scout) =>
      [scout.fullName, scout.email, scout.organizationName]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
        .includes(normalizedSearch),
    );
  }, [normalizedSearch, scouts]);

  const filteredPlayers = useMemo(() => {
    if (!normalizedSearch) {
      return players;
    }

    return players.filter((player) =>
      [player.fullName, player.username, player.email]
        .filter(Boolean)
        .join(" ")
        .toLowerCase()
        .includes(normalizedSearch),
    );
  }, [normalizedSearch, players]);

  const currentList = activeTab === TAB_SCOUTS ? filteredScouts : filteredPlayers;

  const openDeleteDialog = (user) => {
    setDeleteTarget(user);
  };

  const closeDeleteDialog = () => {
    setDeleteTarget(null);
  };

  const confirmDelete = async () => {
    if (!deleteTarget) {
      return;
    }

    const userId = String(deleteTarget.id);

    setDeletingById((current) => ({
      ...current,
      [userId]: true,
    }));

    try {
      await deleteUserAccount(userId);
      toast.success(isArabic ? "تم حذف الحساب." : "Account deleted.");
      closeDeleteDialog();
      await loadUsers(true);
    } catch (error) {
      const fallbackMessage = isArabic ? "تعذر حذف الحساب." : "Unable to delete account.";
      const message = error instanceof Error ? error.message : fallbackMessage;
      toast.error(message || fallbackMessage);
    } finally {
      setDeletingById((current) => ({
        ...current,
        [userId]: false,
      }));
    }
  };

  return (
    <section className="bg-transparent px-4 py-10">
      <div className="m-auto max-w-6xl">
        <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="mb-6 shadow-xl backdrop-blur-sm">
          <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
            <div>
              <h1 className="cc-title-section text-[#1d1d1d] dark:text-[#cae9ea]">
                {isArabic ? "إدارة الحسابات" : "Account Management"}
              </h1>
              <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/85">
                {isArabic
                  ? "عرض جميع حسابات المستقطبين واللاعبين، مع إمكانية حذف أي حساب مع بياناته المرتبطة."
                  : "View all scout and player accounts and remove any account with related data."}
              </p>
            </div>

            <div className="flex flex-wrap items-center gap-2">
              <Button
                as={Link}
                to="/admin"
                variant="bordered"
                className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]"
              >
                {isArabic ? "العودة للوحة الإدارة" : "Back to Admin Panel"}
              </Button>
              <Button
                className="cc-button-text bg-[#208c8c] text-[#cae9ea] hover:bg-[#273b40]"
                isLoading={isRefreshing}
                isDisabled={isLoading || isRefreshing}
                onPress={() => loadUsers(true)}
              >
                {isArabic ? "تحديث" : "Refresh"}
              </Button>
            </div>
          </div>

          <div className="mb-4 max-w-md">
            <Input
              type="search"
              value={searchQuery}
              onValueChange={setSearchQuery}
              placeholder={
                isArabic
                  ? "ابحث بالاسم أو البريد أو الجهة"
                  : "Search by name, email, or organization"
              }
              isClearable
              onClear={() => setSearchQuery("")}
            />
          </div>

          <Tabs
            selectedKey={activeTab}
            onSelectionChange={(key) => setActiveTab(String(key))}
            variant="bordered"
            radius="sm"
          >
            <Tab
              key={TAB_SCOUTS}
              title={`${isArabic ? "المستقطبون" : "Scouts"} (${scouts.length})`}
            />
            <Tab
              key={TAB_PLAYERS}
              title={`${isArabic ? "اللاعبون" : "Players"} (${players.length})`}
            />
          </Tabs>
        </Card>

        {isLoading ? (
          <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
            <ThemedLoader />
          </Card>
        ) : currentList.length === 0 ? (
          <Card bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58" className="shadow-xl backdrop-blur-sm">
            <p className="cc-body-lead text-[#273b40] dark:text-[#cae9ea]/85">
              {isArabic ? "لا توجد نتائج مطابقة." : "No matching users found."}
            </p>
          </Card>
        ) : (
          <div className="space-y-4">
            {currentList.map((user) => {
              const isDeleting = Boolean(deletingById[String(user.id)]);
              const isScout = activeTab === TAB_SCOUTS;

              return (
                <Card
                  key={user.id}
                  bg="bg-[#f4fbfb]/88 dark:bg-[#273b40]/58"
                  className="shadow-xl backdrop-blur-sm"
                >
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <h2 className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
                        {user.fullName || (isArabic ? "بدون اسم" : "Unnamed")}
                      </h2>
                      {isScout ? (
                        <>
                          <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/85">
                            {user.organizationName || "-"}
                          </p>
                          <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
                            {user.email || "-"}
                          </p>
                        </>
                      ) : (
                        <>
                          <p className="cc-body-muted mt-1 text-[#273b40] dark:text-[#cae9ea]/85">
                            {isArabic ? "اسم اللاعب" : "Gamertag"}: {user.username || "-"}
                          </p>
                          <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/85">
                            {user.email || "-"}
                          </p>
                        </>
                      )}
                      <p className="mt-1 text-xs text-[#273b40]/75 dark:text-[#cae9ea]/70">
                        {isArabic ? "آخر تحديث" : "Last Updated"}: {formatTimestamp(user.updatedAt)}
                      </p>
                      {user.declineReason ? (
                        <p className="mt-2 rounded-md border border-red-300/50 bg-red-50/70 px-3 py-2 text-sm text-red-700 dark:border-red-500/30 dark:bg-red-500/10 dark:text-red-200">
                          {isArabic ? "سبب الرفض" : "Decline Reason"}: {user.declineReason}
                        </p>
                      ) : null}
                    </div>

                    <div className="flex flex-col items-end gap-2">
                      <Chip variant="flat" className={getStatusChipClass(user.status)}>
                        {getStatusLabel(user.status, isArabic)}
                      </Chip>
                      <Button
                        size="sm"
                        className="cc-button-text bg-red-600 text-white hover:bg-red-700"
                        isLoading={isDeleting}
                        isDisabled={isDeleting}
                        onPress={() =>
                          openDeleteDialog({
                            ...user,
                            roleLabel: isScout
                              ? isArabic
                                ? "مستقطب"
                                : "Scout"
                              : isArabic
                                ? "لاعب"
                                : "Player",
                          })
                        }
                      >
                        {isArabic ? "حذف الحساب" : "Delete Account"}
                      </Button>
                    </div>
                  </div>
                </Card>
              );
            })}
          </div>
        )}

        <Modal
          isOpen={Boolean(deleteTarget)}
          onOpenChange={(isOpen) => {
            if (!isOpen) {
              closeDeleteDialog();
            }
          }}
          placement="top-center"
          backdrop="blur"
        >
          <ModalContent>
            <ModalHeader className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
              {isArabic ? "تأكيد حذف الحساب" : "Confirm Account Deletion"}
            </ModalHeader>
            <ModalBody>
              <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/80">
                {isArabic
                  ? `سيتم حذف حساب ${deleteTarget?.roleLabel || ""} (${deleteTarget?.fullName || "-"}) وكل البيانات المرتبطة به بشكل نهائي.`
                  : `This will permanently delete the ${deleteTarget?.roleLabel || ""} account (${deleteTarget?.fullName || "-"}) and related data.`}
              </p>
            </ModalBody>
            <ModalFooter>
              <Button
                type="button"
                variant="bordered"
                className="cc-button-text border-[#3c4748]/50 text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]"
                onPress={closeDeleteDialog}
              >
                {isArabic ? "إلغاء" : "Cancel"}
              </Button>
              <Button
                type="button"
                className="cc-button-text bg-red-600 text-white hover:bg-red-700"
                isLoading={Boolean(deleteTarget && deletingById[String(deleteTarget.id)])}
                isDisabled={Boolean(deleteTarget && deletingById[String(deleteTarget.id)])}
                onPress={confirmDelete}
              >
                {isArabic ? "تأكيد الحذف" : "Delete Permanently"}
              </Button>
            </ModalFooter>
          </ModalContent>
        </Modal>
      </div>
    </section>
  );
};

export default AdminUsersPage;
