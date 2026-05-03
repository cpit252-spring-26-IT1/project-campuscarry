import { ClipLoader } from "react-spinners";
import useLanguage from "../hooks/useLanguage";
import useTheme from "../hooks/useTheme";

const ThemedLoader = ({ className = "", label, size = 36, showLabel = true }) => {
  const { isDarkMode } = useTheme();
  const { t } = useLanguage();

  const spinnerColor = isDarkMode ? "#cae9ea" : "#273b40";
  const labelColorClass = isDarkMode ? "text-[#cae9ea]/85" : "text-[#273b40]/85";
  const resolvedLabel = typeof label === "string" ? label : `${t("common.loading")}...`;

  return (
    <div className={`flex flex-col items-center justify-center gap-3 ${className}`}>
      <ClipLoader color={spinnerColor} size={size} speedMultiplier={0.9} />
      {showLabel ? (
        <p className={`text-sm font-semibold ${labelColorClass}`}>{resolvedLabel}</p>
      ) : null}
    </div>
  );
};

export default ThemedLoader;
