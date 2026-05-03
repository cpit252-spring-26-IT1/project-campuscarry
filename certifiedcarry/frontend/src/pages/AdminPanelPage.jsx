import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button } from "@heroui/react";
import { toast } from "react-toastify";
import Card from "../components/Card";
import AdminPanelHeader from "../components/admin/AdminPanelHeader";
import DeclineReasonModal from "../components/admin/DeclineReasonModal";
import DeclinedItemsTab from "../components/admin/DeclinedItemsTab";
import ProofImageViewer from "../components/admin/ProofImageViewer";
import RankRequestsTab from "../components/admin/RankRequestsTab";
import RecruiterRequestsTab from "../components/admin/RecruiterRequestsTab";
import { ADMIN_CARD_BG, ADMIN_CARD_CLASS, PRIMARY_BUTTON_CLASS } from "../components/admin/adminShared";
import ThemedLoader from "../components/ThemedLoader";
import useLanguage from "../hooks/useLanguage";
import { logBackgroundAuthIssue } from "../services/auth/shared";
import {
  approveRankSubmission,
  approveRecruiter,
  declineRankSubmission,
  declineRecruiter,
  getDeclinedItems,
  getPendingRanks,
  getPendingRecruiters,
} from "../services/adminService";

const TABS = {
  RECRUITERS: "RECRUITERS",
  RANKS: "RANKS",
  DECLINED: "DECLINED",
};

const IMAGE_MIN_ZOOM = 1;
const IMAGE_ZOOM_STEP = 0.25;
const IMAGE_MAX_ZOOM = 5;
const clampZoom = (value) => Math.min(IMAGE_MAX_ZOOM, Math.max(IMAGE_MIN_ZOOM, value));

const AdminPanelPage = () => {
  const { isArabic } = useLanguage();

  const [activeTab, setActiveTab] = useState(TABS.RECRUITERS);
  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [actionInProgress, setActionInProgress] = useState({});

  const [pendingRecruiters, setPendingRecruiters] = useState([]);
  const [pendingRanks, setPendingRanks] = useState([]);
  const [declinedItems, setDeclinedItems] = useState([]);

  const [declineDialog, setDeclineDialog] = useState(null);
  const [declineReasonInput, setDeclineReasonInput] = useState("");

  const [activeProofImage, setActiveProofImage] = useState(null);
  const [imageZoom, setImageZoom] = useState(1);
  const [imagePan, setImagePan] = useState({ x: 0, y: 0 });
  const [isPanningImage, setIsPanningImage] = useState(false);

  const panStartRef = useRef({ x: 0, y: 0 });
  const panOriginRef = useRef({ x: 0, y: 0 });

  const loadQueues = useCallback(
    async (refresh = false) => {
      if (refresh) {
        setIsRefreshing(true);
      } else {
        setIsLoading(true);
      }

      try {
        const [recruiters, ranks, declined] = await Promise.all([
          getPendingRecruiters(),
          getPendingRanks(),
          getDeclinedItems(),
        ]);

        setPendingRecruiters(recruiters);
        setPendingRanks(ranks);
        setDeclinedItems(declined);
      } catch (error) {
        logBackgroundAuthIssue("Failed loading admin queues:", error, "error");
        toast.error(isArabic ? "تعذر تحميل قوائم الإدارة" : "Unable to load admin queues.");
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
    loadQueues();
  }, [loadQueues]);

  const resetImageViewer = useCallback(() => {
    setImageZoom(1);
    setImagePan({ x: 0, y: 0 });
    setIsPanningImage(false);
  }, []);

  const closeImageViewer = useCallback(() => {
    setActiveProofImage(null);
    resetImageViewer();
  }, [resetImageViewer]);

  const openImageViewer = useCallback(
    (proofImage, username) => {
      setActiveProofImage({ src: proofImage, username });
      resetImageViewer();
    },
    [resetImageViewer],
  );

  const changeZoom = useCallback((delta) => {
    setImageZoom((currentZoom) => {
      const nextZoom = clampZoom(currentZoom + delta);

      if (nextZoom <= IMAGE_MIN_ZOOM) {
        setImagePan({ x: 0, y: 0 });
      }

      return nextZoom;
    });
  }, []);

  const handleImageWheel = useCallback(
    (event) => {
      event.preventDefault();
      const delta = event.deltaY < 0 ? IMAGE_ZOOM_STEP : -IMAGE_ZOOM_STEP;
      changeZoom(delta);
    },
    [changeZoom],
  );

  const handleImagePointerDown = useCallback(
    (event) => {
      if (imageZoom <= IMAGE_MIN_ZOOM) {
        return;
      }

      event.preventDefault();
      event.currentTarget.setPointerCapture(event.pointerId);
      setIsPanningImage(true);
      panStartRef.current = { x: event.clientX, y: event.clientY };
      panOriginRef.current = { ...imagePan };
    },
    [imagePan, imageZoom],
  );

  const handleImagePointerMove = useCallback(
    (event) => {
      if (!isPanningImage) {
        return;
      }

      const deltaX = event.clientX - panStartRef.current.x;
      const deltaY = event.clientY - panStartRef.current.y;

      setImagePan({
        x: panOriginRef.current.x + deltaX,
        y: panOriginRef.current.y + deltaY,
      });
    },
    [isPanningImage],
  );

  const stopImagePanning = useCallback((event) => {
    if (event.currentTarget.hasPointerCapture(event.pointerId)) {
      event.currentTarget.releasePointerCapture(event.pointerId);
    }

    setIsPanningImage(false);
  }, []);

  useEffect(() => {
    if (!activeProofImage) {
      return undefined;
    }

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const handleKeyDown = (event) => {
      if (event.key === "Escape") {
        closeImageViewer();
        return;
      }

      if (event.key === "+" || event.key === "=") {
        event.preventDefault();
        changeZoom(IMAGE_ZOOM_STEP);
        return;
      }

      if (event.key === "-" || event.key === "_") {
        event.preventDefault();
        changeZoom(-IMAGE_ZOOM_STEP);
      }
    };

    window.addEventListener("keydown", handleKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [activeProofImage, changeZoom, closeImageViewer]);

  const runAction = async ({ key, action, successMessage, errorMessage }) => {
    setActionInProgress((current) => ({ ...current, [key]: true }));

    try {
      await action();
      toast.success(successMessage);
      await loadQueues(true);
    } catch (error) {
      logBackgroundAuthIssue("Admin action failed:", error, "error");
      toast.error(errorMessage);
    } finally {
      setActionInProgress((current) => ({ ...current, [key]: false }));
    }
  };

  const openDeclineDialog = ({ actionKey, requestType, payload, requestLabel }) => {
    setDeclineReasonInput("");
    setDeclineDialog({ actionKey, requestType, payload, requestLabel });
  };

  const closeDeclineDialog = () => {
    setDeclineDialog(null);
    setDeclineReasonInput("");
  };

  const submitDeclineDialog = () => {
    if (!declineDialog) {
      return;
    }

    const reason = declineReasonInput.trim();
    if (!reason) {
      toast.error(isArabic ? "سبب الرفض مطلوب." : "Decline reason is required.");
      return;
    }

    const isRecruiterRequest = declineDialog.requestType === "RECRUITER";
    const successMessage = isRecruiterRequest
      ? isArabic
        ? "تم رفض المستقطب"
        : "Scout declined successfully."
      : isArabic
        ? "تم رفض إثبات الرتبة"
        : "Rank submission declined.";
    const errorMessage = isRecruiterRequest
      ? isArabic
        ? "تعذر رفض المستقطب"
        : "Unable to decline scout."
      : isArabic
        ? "تعذر رفض إثبات الرتبة"
        : "Unable to decline rank submission.";

    runAction({
      key: declineDialog.actionKey,
      action: () =>
        isRecruiterRequest
          ? declineRecruiter(declineDialog.payload, reason)
          : declineRankSubmission(declineDialog.payload, reason),
      successMessage,
      errorMessage,
    });

    closeDeclineDialog();
  };

  const tabButtons = useMemo(
    () => [
      {
        key: TABS.RECRUITERS,
        label: isArabic ? "طلبات المستقطبين" : "Pending Scouts",
        count: pendingRecruiters.length,
      },
      {
        key: TABS.RANKS,
        label: isArabic ? "طلبات الرتب" : "Pending Ranks",
        count: pendingRanks.length,
      },
      {
        key: TABS.DECLINED,
        label: isArabic ? "المرفوض" : "Declined",
        count: declinedItems.length,
      },
    ],
    [declinedItems.length, isArabic, pendingRanks.length, pendingRecruiters.length],
  );

  return (
    <section className="bg-transparent px-4 py-10">
      <div className="m-auto max-w-6xl">
        <AdminPanelHeader
          activeTab={activeTab}
          isArabic={isArabic}
          isLoading={isLoading}
          isRefreshing={isRefreshing}
          loadQueues={loadQueues}
          onTabChange={setActiveTab}
          tabButtons={tabButtons}
        />

        {isLoading ? (
          <Card bg={ADMIN_CARD_BG} className={ADMIN_CARD_CLASS}>
            <ThemedLoader />
          </Card>
        ) : null}

        {!isLoading && activeTab === TABS.RECRUITERS ? (
          <RecruiterRequestsTab
            actionInProgress={actionInProgress}
            isArabic={isArabic}
            onApprove={(recruiter, actionKey) =>
              runAction({
                key: actionKey,
                action: () => approveRecruiter(recruiter),
                successMessage: isArabic
                  ? "تمت الموافقة على المستقطب"
                  : "Scout approved successfully.",
                errorMessage: isArabic
                  ? "تعذر الموافقة على المستقطب"
                  : "Unable to approve scout.",
              })
            }
            onDecline={(recruiter, actionKey) =>
              openDeclineDialog({
                actionKey,
                requestType: "RECRUITER",
                payload: recruiter,
                requestLabel: isArabic ? "طلب المستقطب" : "scout request",
              })
            }
            pendingRecruiters={pendingRecruiters}
          />
        ) : null}

        {!isLoading && activeTab === TABS.RANKS ? (
          <RankRequestsTab
            actionInProgress={actionInProgress}
            isArabic={isArabic}
            onApprove={(submission, actionKey) =>
              runAction({
                key: actionKey,
                action: () => approveRankSubmission(submission),
                successMessage: isArabic
                  ? "تم اعتماد الرتبة بنجاح"
                  : "Rank approved successfully.",
                errorMessage: isArabic
                  ? "تعذر اعتماد الرتبة"
                  : "Unable to approve rank submission.",
              })
            }
            onDecline={(submission, actionKey) =>
              openDeclineDialog({
                actionKey,
                requestType: "RANK",
                payload: submission,
                requestLabel: isArabic ? "طلب الرتبة" : "rank request",
              })
            }
            onOpenImageViewer={openImageViewer}
            pendingRanks={pendingRanks}
          />
        ) : null}

        {!isLoading && activeTab === TABS.DECLINED ? (
          <DeclinedItemsTab
            declinedItems={declinedItems}
            isArabic={isArabic}
            onOpenImageViewer={openImageViewer}
          />
        ) : null}

        <ProofImageViewer
          activeProofImage={activeProofImage}
          changeZoom={changeZoom}
          closeImageViewer={closeImageViewer}
          imagePan={imagePan}
          imageZoom={imageZoom}
          isArabic={isArabic}
          isPanningImage={isPanningImage}
          onPointerDown={handleImagePointerDown}
          onPointerMove={handleImagePointerMove}
          onWheel={handleImageWheel}
          resetImageViewer={resetImageViewer}
          stopImagePanning={stopImagePanning}
          zoomStep={IMAGE_ZOOM_STEP}
        />

        <DeclineReasonModal
          closeDeclineDialog={closeDeclineDialog}
          declineDialog={declineDialog}
          declineReasonInput={declineReasonInput}
          isArabic={isArabic}
          setDeclineReasonInput={setDeclineReasonInput}
          submitDeclineDialog={submitDeclineDialog}
        />
      </div>
    </section>
  );
};

export default AdminPanelPage;
