import {
  Button,
  Modal,
  ModalBody,
  ModalContent,
  ModalFooter,
  ModalHeader,
  Textarea,
} from "@heroui/react";
import { DECLINE_BUTTON_CLASS, OUTLINE_BUTTON_CLASS } from "./adminShared";

const DeclineReasonModal = ({
  closeDeclineDialog,
  declineDialog,
  declineReasonInput,
  isArabic,
  setDeclineReasonInput,
  submitDeclineDialog,
}) => {
  return (
    <Modal
      isOpen={Boolean(declineDialog)}
      onOpenChange={(isOpen) => {
        if (!isOpen) {
          closeDeclineDialog();
        }
      }}
      placement="top-center"
      backdrop="blur"
    >
      <ModalContent>
        <ModalHeader className="cc-title-card text-[#1d1d1d] dark:text-[#cae9ea]">
          {isArabic ? "سبب الرفض" : "Decline Reason"}
        </ModalHeader>
        <ModalBody>
          <p className="cc-body-muted text-[#273b40] dark:text-[#cae9ea]/80">
            {isArabic
              ? `اكتب سبب الرفض لـ ${declineDialog?.requestLabel || ""}.`
              : `Type a decline reason for this ${declineDialog?.requestLabel || "request"}.`}
          </p>
          <Textarea
            autoFocus
            minRows={4}
            value={declineReasonInput}
            onValueChange={setDeclineReasonInput}
            placeholder={
              isArabic
                ? "مثال: الصورة غير واضحة أو الرتبة غير مطابقة."
                : "Example: proof image is unclear or rank details do not match."
            }
          />
        </ModalBody>
        <ModalFooter>
          <Button type="button" variant="bordered" className={OUTLINE_BUTTON_CLASS} onPress={closeDeclineDialog}>
            {isArabic ? "إلغاء" : "Cancel"}
          </Button>
          <Button type="button" className={DECLINE_BUTTON_CLASS} onPress={submitDeclineDialog}>
            {isArabic ? "تأكيد الرفض" : "Confirm Decline"}
          </Button>
        </ModalFooter>
      </ModalContent>
    </Modal>
  );
};

export default DeclineReasonModal;
