import { Select, SelectItem } from "@heroui/react";
import { RECRUITER_DM_OPENNESS } from "../services/recruiterService";
import { getSelectValue } from "../utils/selectHelpers";

const DmOpennessSelector = ({ isArabic, value, isDisabled, onChange }) => {
  return (
    <Select
      label={isArabic ? "وضع استقبال الرسائل" : "Message Openness Mode"}
      labelPlacement="outside"
      selectedKeys={[value]}
      isDisabled={isDisabled}
      onSelectionChange={(keys) => {
        const selectedValue = getSelectValue(keys);
        if (selectedValue) {
          onChange(String(selectedValue));
        }
      }}
    >
      <SelectItem key={RECRUITER_DM_OPENNESS.CLOSED}>{isArabic ? "مغلق" : "Closed"}</SelectItem>
      <SelectItem key={RECRUITER_DM_OPENNESS.OPEN_ALL_PLAYERS}>
        {isArabic ? "مفتوح لكل اللاعبين" : "Open to all players"}
      </SelectItem>
      <SelectItem key={RECRUITER_DM_OPENNESS.OPEN_VERIFIED_PLAYERS}>
        {isArabic ? "مفتوح للاعبين الموثقين فقط" : "Open to verified players only"}
      </SelectItem>
    </Select>
  );
};

export default DmOpennessSelector;
