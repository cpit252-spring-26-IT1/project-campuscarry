import { Button, Input, Select, SelectItem } from "@heroui/react";
import {
  GAME_OPTIONS,
  GAME_RANK_OPTIONS,
  ROCKET_LEAGUE_MODES,
} from "../../constants/gameConfig";
import { OUTLINE_BUTTON_CLASS, PRIMARY_BUTTON_CLASS } from "./profileSetupShared";

const GameAndRankSection = ({
  availableRanks,
  game,
  handleGameChange,
  handleRocketLeagueModeRankChange,
  handleRocketLeagueModeRatingChange,
  handleRocketLeagueModeToggle,
  isArabic,
  isRocketLeague,
  labelClass,
  needsRating,
  primaryRocketLeagueMode,
  rank,
  ratingLabel,
  ratingValue,
  requiredAsteriskClass,
  rocketLeagueModeDetails,
  rocketLeagueModes,
  setPrimaryRocketLeagueMode,
  setRank,
  setRatingValue,
}) => {
  return (
    <>
      <div className="mb-4">
        <p className={labelClass}>
          {isArabic ? "اللعبة" : "Game"} <span className={requiredAsteriskClass}>*</span>
        </p>
        <Select selectedKeys={game ? [game] : []} onSelectionChange={handleGameChange}>
          {GAME_OPTIONS.map((option) => (
            <SelectItem key={option}>{option}</SelectItem>
          ))}
        </Select>
      </div>

      {isRocketLeague ? (
        <>
          <div className="mb-4">
            <p className={labelClass}>
              {isArabic
                ? "أنماط اللعب (اختر نمطا واحدا على الأقل)"
                : "Game Modes (select at least one)"}
            </p>
            <div className="flex flex-wrap gap-2">
              {ROCKET_LEAGUE_MODES.map((mode) => {
                const isSelected = rocketLeagueModes.includes(mode);

                return (
                  <Button
                    key={mode}
                    size="sm"
                    radius="sm"
                    className={isSelected ? PRIMARY_BUTTON_CLASS : `cc-button-text border border-[#3c4748]/50 bg-transparent text-[#1d1d1d] transition-colors hover:bg-[#cae9ea]/55 hover:text-[#273b40] dark:text-[#cae9ea] dark:hover:bg-[#208c8c]/30 dark:hover:text-[#cae9ea]`}
                    onPress={() => handleRocketLeagueModeToggle(mode)}
                  >
                    {mode}
                  </Button>
                );
              })}
            </div>
          </div>

          {rocketLeagueModes.map((mode) => (
            <div
              key={mode}
              className="mb-4 rounded-md border border-[#3c4748]/35 bg-[#cae9ea]/20 p-3 dark:bg-[#1d1d1d]/30"
            >
              <div className="mb-3 flex flex-wrap items-center justify-between gap-2">
                <p className="text-sm font-semibold text-[#1d1d1d] dark:text-[#cae9ea]">{mode}</p>
                <Button
                  size="sm"
                  variant="bordered"
                  className={OUTLINE_BUTTON_CLASS}
                  onPress={() => setPrimaryRocketLeagueMode(mode)}
                >
                  {primaryRocketLeagueMode === mode
                    ? isArabic
                      ? "النمط الأساسي"
                      : "Primary Mode"
                    : isArabic
                      ? "اجعله أساسيا"
                      : "Set as Primary"}
                </Button>
              </div>

              <div className="mb-3">
                <p
                  className={`mb-2 text-xs font-semibold text-[#1d1d1d] dark:text-[#cae9ea] ${
                    isArabic ? "text-right" : "text-left"
                  }`}
                >
                  {isArabic ? "الرتبة" : "Rank"} ({mode}){" "}
                  <span className={requiredAsteriskClass}>*</span>
                </p>
                <Select
                  selectedKeys={
                    rocketLeagueModeDetails[mode]?.rank ? [rocketLeagueModeDetails[mode].rank] : []
                  }
                  onSelectionChange={(keys) => handleRocketLeagueModeRankChange(mode, keys)}
                >
                  {GAME_RANK_OPTIONS["Rocket League"].map((option) => (
                    <SelectItem key={option}>{option}</SelectItem>
                  ))}
                </Select>
              </div>

              <div>
                <p
                  className={`mb-2 text-xs font-semibold text-[#1d1d1d] dark:text-[#cae9ea] ${
                    isArabic ? "text-right" : "text-left"
                  }`}
                >
                  MMR ({mode}) <span className={requiredAsteriskClass}>*</span>
                </p>
                <Input
                  type="number"
                  min="0"
                  isRequired
                  placeholder={isArabic ? "مثال: 1200" : "Example: 1200"}
                  value={rocketLeagueModeDetails[mode]?.ratingValue || ""}
                  onValueChange={(nextValue) => handleRocketLeagueModeRatingChange(mode, nextValue)}
                />
              </div>
            </div>
          ))}
        </>
      ) : (
        <>
          <div className="mb-4">
            <p className={labelClass}>
              {isArabic ? "الرتبة" : "Rank"} <span className={requiredAsteriskClass}>*</span>
            </p>
            <Select
              selectedKeys={rank ? [rank] : []}
              onSelectionChange={(keys) => setRank(keys?.currentKey || Array.from(keys)[0] || "")}
              isDisabled={!game}
            >
              {availableRanks.map((option) => (
                <SelectItem key={option}>{option}</SelectItem>
              ))}
            </Select>
          </div>

          {needsRating ? (
            <div className="mb-4">
              <p className={labelClass}>
                {isArabic ? (ratingLabel === "MMR" ? "قيمة MMR" : "قيمة Skill Rating") : ratingLabel}{" "}
                <span className={requiredAsteriskClass}>*</span>
              </p>
              <Input
                type="number"
                min="0"
                isRequired
                placeholder={isArabic ? "مثال: 420" : "Example: 420"}
                value={ratingValue}
                onValueChange={setRatingValue}
              />
            </div>
          ) : null}
        </>
      )}
    </>
  );
};

export default GameAndRankSection;
