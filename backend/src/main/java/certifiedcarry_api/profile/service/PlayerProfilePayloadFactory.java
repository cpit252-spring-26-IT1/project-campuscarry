package certifiedcarry_api.profile.service;

import certifiedcarry_api.profile.PlayerProfileFields;
import certifiedcarry_api.shared.GameNameAlias;
import certifiedcarry_api.shared.HttpErrors;
import certifiedcarry_api.shared.HttpRequestParsers;
import certifiedcarry_api.shared.RequestArrayNormalizer;
import certifiedcarry_api.shared.RequestStatusNormalizer;
import certifiedcarry_api.shared.RocketLeagueModesCodec;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PlayerProfilePayloadFactory {

  private static final Set<String> RANK_VERIFICATION_STATUSES =
      Set.of(
          PlayerProfileFields.STATUS_NOT_SUBMITTED,
          PlayerProfileFields.STATUS_PENDING,
          PlayerProfileFields.STATUS_APPROVED,
          PlayerProfileFields.STATUS_DECLINED);

  private final ObjectMapper objectMapper;
  private final int rankValidationValidityDays;

  PlayerProfilePayloadFactory(ObjectMapper objectMapper, int rankValidationValidityDays) {
    this.objectMapper = objectMapper;
    this.rankValidationValidityDays = Math.max(1, rankValidationValidityDays);
  }

  PlayerProfilePayload buildCreatePayload(Map<String, Object> request) {
    long userId =
        HttpRequestParsers.requireLong(request.get(PlayerProfileFields.USER_ID), PlayerProfileFields.USER_ID);
    String username =
        HttpRequestParsers.requireNonBlank(
            request.get(PlayerProfileFields.USERNAME), PlayerProfileFields.USERNAME);
    String profileImage =
        HttpRequestParsers.defaultIfBlank(
            HttpRequestParsers.optionalString(request.get(PlayerProfileFields.PROFILE_IMAGE)),
            PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    String game =
        GameNameAlias.normalizeNullable(
            HttpRequestParsers.optionalString(request.get(PlayerProfileFields.GAME)));
    String rank = HttpRequestParsers.optionalString(request.get(PlayerProfileFields.RANK));
    boolean allowPlayerChats =
        HttpRequestParsers.optionalBoolean(
            request.get(PlayerProfileFields.ALLOW_PLAYER_CHATS),
            PlayerProfileFields.ALLOW_PLAYER_CHATS,
            true);
    boolean isWithTeam =
        HttpRequestParsers.optionalBoolean(
            request.get(PlayerProfileFields.IS_WITH_TEAM), PlayerProfileFields.IS_WITH_TEAM, false);
    String teamName = normalizeTeamName(request.get(PlayerProfileFields.TEAM_NAME), isWithTeam);
    List<String> rocketLeagueModes =
        RocketLeagueModesCodec.encodeRocketLeagueModes(
            request.get(PlayerProfileFields.ROCKET_LEAGUE_MODES), objectMapper);
    String primaryRocketLeagueMode =
        HttpRequestParsers.optionalString(request.get(PlayerProfileFields.PRIMARY_ROCKET_LEAGUE_MODE));
    List<String> inGameRoles =
        RequestArrayNormalizer.normalizeStringArray(
            request.get(PlayerProfileFields.IN_GAME_ROLES), PlayerProfileFields.IN_GAME_ROLES);
    String inGameRole =
        HttpRequestParsers.optionalString(request.get(PlayerProfileFields.IN_GAME_ROLE));
    BigDecimal ratingValue =
        HttpRequestParsers.optionalBigDecimal(
            request.get(PlayerProfileFields.RATING_VALUE), PlayerProfileFields.RATING_VALUE);
    String ratingLabel =
        HttpRequestParsers.defaultIfBlank(
            HttpRequestParsers.optionalString(request.get(PlayerProfileFields.RATING_LABEL)),
            PlayerProfileFields.DEFAULT_RATING_LABEL);
    String proofImage =
        HttpRequestParsers.defaultIfBlank(
            HttpRequestParsers.optionalString(request.get(PlayerProfileFields.PROOF_IMAGE)),
            PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    String bio =
        HttpRequestParsers.defaultIfBlank(
            HttpRequestParsers.optionalString(request.get(PlayerProfileFields.BIO)),
            PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    String clipsUrl =
        HttpRequestParsers.defaultIfBlank(
            HttpRequestParsers.optionalString(request.get(PlayerProfileFields.CLIPS_URL)),
            PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    String rankVerificationStatus =
        normalizeRankVerificationStatus(
            HttpRequestParsers.optionalString(
                request.get(PlayerProfileFields.RANK_VERIFICATION_STATUS)),
            true);
    String declineReason =
        HttpRequestParsers.defaultIfBlank(
            HttpRequestParsers.optionalString(request.get(PlayerProfileFields.DECLINE_REASON)),
            PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    OffsetDateTime declinedAt =
        HttpRequestParsers.optionalOffsetDateTime(
            request.get(PlayerProfileFields.DECLINED_AT), PlayerProfileFields.DECLINED_AT);
    boolean isVerified =
        HttpRequestParsers.optionalBoolean(
            request.get(PlayerProfileFields.IS_VERIFIED), PlayerProfileFields.IS_VERIFIED, false);
    OffsetDateTime submittedAt =
        HttpRequestParsers.optionalOffsetDateTime(
            request.get(PlayerProfileFields.SUBMITTED_AT), PlayerProfileFields.SUBMITTED_AT);
    OffsetDateTime rankVerifiedAt =
        HttpRequestParsers.optionalOffsetDateTime(
            request.get(PlayerProfileFields.RANK_VERIFIED_AT), PlayerProfileFields.RANK_VERIFIED_AT);
    OffsetDateTime rankExpiresAt =
        HttpRequestParsers.optionalOffsetDateTime(
            request.get(PlayerProfileFields.RANK_EXPIRES_AT), PlayerProfileFields.RANK_EXPIRES_AT);
    HttpRequestParsers.optionalOffsetDateTime(
        request.get(PlayerProfileFields.RANK_EXPIRY_REMINDER_SENT_AT),
        PlayerProfileFields.RANK_EXPIRY_REMINDER_SENT_AT);
    OffsetDateTime updatedAt =
        HttpRequestParsers.optionalOffsetDateTime(
            request.get(PlayerProfileFields.UPDATED_AT), PlayerProfileFields.UPDATED_AT);

    VerificationState verificationState =
        computeVerificationStateForCreate(
            rankVerificationStatus, isVerified, rankVerifiedAt, rankExpiresAt);

    return new PlayerProfilePayload(
        userId,
        username,
        profileImage,
        game,
        rank,
        allowPlayerChats,
        isWithTeam,
        teamName,
        rocketLeagueModes,
        primaryRocketLeagueMode,
        inGameRoles,
        inGameRole,
        ratingValue,
        ratingLabel,
        proofImage,
        bio,
        clipsUrl,
        rankVerificationStatus,
        declineReason,
        declinedAt,
        isVerified,
        submittedAt,
        verificationState.rankVerifiedAt(),
        verificationState.rankExpiresAt(),
        verificationState.rankExpiryReminderSentAt(),
        updatedAt);
  }

  PlayerProfilePayload buildPatchPayload(Map<String, Object> request, PlayerProfileRow existing) {
    long userId = resolveLong(request, PlayerProfileFields.USER_ID, existing.userId());
    String username =
        resolveRequiredString(request, PlayerProfileFields.USERNAME, existing.username());
    String profileImage =
        resolveDefaultedString(
            request,
            PlayerProfileFields.PROFILE_IMAGE,
            existing.profileImage(),
            PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    String game =
        request.containsKey(PlayerProfileFields.GAME)
            ? GameNameAlias.normalizeNullable(
                HttpRequestParsers.optionalString(request.get(PlayerProfileFields.GAME)))
            : existing.game();
    String rank = resolveOptionalString(request, PlayerProfileFields.RANK, existing.rank());
    boolean allowPlayerChats =
        resolveBoolean(
            request, PlayerProfileFields.ALLOW_PLAYER_CHATS, existing.allowPlayerChats());
    boolean isWithTeam =
        resolveBoolean(request, PlayerProfileFields.IS_WITH_TEAM, existing.isWithTeam());
    String teamName = resolveTeamNameForPatch(request, existing, isWithTeam);
    List<String> rocketLeagueModes =
        request.containsKey(PlayerProfileFields.ROCKET_LEAGUE_MODES)
            ? RocketLeagueModesCodec.encodeRocketLeagueModes(
                request.get(PlayerProfileFields.ROCKET_LEAGUE_MODES), objectMapper)
            : existing.rocketLeagueModes();
    String primaryRocketLeagueMode =
        resolveOptionalString(
            request,
            PlayerProfileFields.PRIMARY_ROCKET_LEAGUE_MODE,
            existing.primaryRocketLeagueMode());
    List<String> inGameRoles =
        request.containsKey(PlayerProfileFields.IN_GAME_ROLES)
            ? RequestArrayNormalizer.normalizeStringArray(
                request.get(PlayerProfileFields.IN_GAME_ROLES), PlayerProfileFields.IN_GAME_ROLES)
            : existing.inGameRoles();
    String inGameRole =
        resolveOptionalString(request, PlayerProfileFields.IN_GAME_ROLE, existing.inGameRole());
    BigDecimal ratingValue =
        resolveBigDecimal(request, PlayerProfileFields.RATING_VALUE, existing.ratingValue());
    String ratingLabel =
        resolveDefaultedString(
            request,
            PlayerProfileFields.RATING_LABEL,
            existing.ratingLabel(),
            PlayerProfileFields.DEFAULT_RATING_LABEL);
    String proofImage =
        resolveDefaultedString(
            request,
            PlayerProfileFields.PROOF_IMAGE,
            existing.proofImage(),
            PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    String bio =
        resolveDefaultedString(
            request, PlayerProfileFields.BIO, existing.bio(), PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    String clipsUrl =
        resolveDefaultedString(
            request,
            PlayerProfileFields.CLIPS_URL,
            existing.clipsUrl(),
            PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    String rankVerificationStatus =
        request.containsKey(PlayerProfileFields.RANK_VERIFICATION_STATUS)
            ? normalizeRankVerificationStatus(
                HttpRequestParsers.optionalString(
                    request.get(PlayerProfileFields.RANK_VERIFICATION_STATUS)),
                false)
            : existing.rankVerificationStatus();
    String declineReason =
        resolveDefaultedString(
            request,
            PlayerProfileFields.DECLINE_REASON,
            existing.declineReason(),
            PlayerProfileFields.DEFAULT_EMPTY_TEXT);
    OffsetDateTime declinedAt =
        resolveOffsetDateTime(request, PlayerProfileFields.DECLINED_AT, existing.declinedAt());
    boolean isVerified =
        resolveBoolean(request, PlayerProfileFields.IS_VERIFIED, existing.isVerified());
    OffsetDateTime submittedAt =
        resolveOffsetDateTime(request, PlayerProfileFields.SUBMITTED_AT, existing.submittedAt());
    OffsetDateTime rankVerifiedAt =
        resolveOffsetDateTime(
            request, PlayerProfileFields.RANK_VERIFIED_AT, existing.rankVerifiedAt());
    OffsetDateTime rankExpiresAt =
        resolveOffsetDateTime(
            request, PlayerProfileFields.RANK_EXPIRES_AT, existing.rankExpiresAt());
    OffsetDateTime rankExpiryReminderSentAt =
        resolveOffsetDateTime(
            request,
            PlayerProfileFields.RANK_EXPIRY_REMINDER_SENT_AT,
            existing.rankExpiryReminderSentAt());
    OffsetDateTime updatedAt =
        request.containsKey(PlayerProfileFields.UPDATED_AT)
            ? HttpRequestParsers.optionalOffsetDateTime(
                request.get(PlayerProfileFields.UPDATED_AT), PlayerProfileFields.UPDATED_AT)
            : OffsetDateTime.now(ZoneOffset.UTC);

    VerificationState verificationState =
        computeVerificationStateForPatch(
            request,
            existing,
            rankVerificationStatus,
            isVerified,
            rankVerifiedAt,
            rankExpiresAt,
            rankExpiryReminderSentAt);

    return new PlayerProfilePayload(
        userId,
        username,
        profileImage,
        game,
        rank,
        allowPlayerChats,
        isWithTeam,
        teamName,
        rocketLeagueModes,
        primaryRocketLeagueMode,
        inGameRoles,
        inGameRole,
        ratingValue,
        ratingLabel,
        proofImage,
        bio,
        clipsUrl,
        rankVerificationStatus,
        declineReason,
        declinedAt,
        isVerified,
        submittedAt,
        verificationState.rankVerifiedAt(),
        verificationState.rankExpiresAt(),
        verificationState.rankExpiryReminderSentAt(),
        updatedAt);
  }

  private VerificationState computeVerificationStateForCreate(
      String rankVerificationStatus,
      boolean isVerified,
      OffsetDateTime rankVerifiedAt,
      OffsetDateTime rankExpiresAt) {
    if (!isProfileVerified(rankVerificationStatus, isVerified)) {
      return VerificationState.unverified();
    }

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime verificationBase = rankVerifiedAt != null ? rankVerifiedAt : now;
    OffsetDateTime effectiveExpiresAt =
        rankExpiresAt != null ? rankExpiresAt : verificationBase.plusDays(rankValidationValidityDays);
    return new VerificationState(verificationBase, effectiveExpiresAt, null);
  }

  private VerificationState computeVerificationStateForPatch(
      Map<String, Object> request,
      PlayerProfileRow existing,
      String rankVerificationStatus,
      boolean isVerified,
      OffsetDateTime rankVerifiedAt,
      OffsetDateTime rankExpiresAt,
      OffsetDateTime rankExpiryReminderSentAt) {
    boolean wasVerified =
        isProfileVerified(existing.rankVerificationStatus(), existing.isVerified());
    boolean isNowVerified = isProfileVerified(rankVerificationStatus, isVerified);

    if (!isNowVerified) {
      return VerificationState.unverified();
    }

    boolean becameVerified = !wasVerified;
    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    OffsetDateTime verificationBase = rankVerifiedAt != null ? rankVerifiedAt : now;
    if (becameVerified) {
      verificationBase = now;
    }
    OffsetDateTime effectiveExpiresAt =
        becameVerified || rankExpiresAt == null
            ? verificationBase.plusDays(rankValidationValidityDays)
            : rankExpiresAt;
    boolean verificationTouched =
        request.containsKey(PlayerProfileFields.RANK_VERIFICATION_STATUS)
            || request.containsKey(PlayerProfileFields.IS_VERIFIED);
    OffsetDateTime effectiveReminderSentAt =
        (becameVerified || verificationTouched) ? null : rankExpiryReminderSentAt;

    return new VerificationState(
        verificationBase, effectiveExpiresAt, effectiveReminderSentAt);
  }

  private boolean isProfileVerified(String rankVerificationStatus, boolean isVerified) {
    if (isVerified) {
      return true;
    }

    if (rankVerificationStatus == null) {
      return false;
    }

    return PlayerProfileFields.STATUS_APPROVED.equals(
        rankVerificationStatus.trim().toUpperCase(java.util.Locale.ROOT));
  }

  private String normalizeRankVerificationStatus(String value, boolean defaultOnBlank) {
    return RequestStatusNormalizer.normalize(
        value,
        defaultOnBlank,
        PlayerProfileFields.DEFAULT_RANK_STATUS,
        "rankVerificationStatus is required.",
        RANK_VERIFICATION_STATUSES,
        "rankVerificationStatus must be one of NOT_SUBMITTED, PENDING, APPROVED, or DECLINED.",
        HttpErrors::badRequest);
  }

  private long resolveLong(Map<String, Object> request, String key, long fallback) {
    if (!request.containsKey(key)) {
      return fallback;
    }

    return HttpRequestParsers.requireLong(request.get(key), key);
  }

  private String resolveRequiredString(Map<String, Object> request, String key, String fallback) {
    if (!request.containsKey(key)) {
      return fallback;
    }

    return HttpRequestParsers.requireNonBlank(request.get(key), key);
  }

  private String resolveOptionalString(Map<String, Object> request, String key, String fallback) {
    if (!request.containsKey(key)) {
      return fallback;
    }

    return HttpRequestParsers.optionalString(request.get(key));
  }

  private String resolveDefaultedString(
      Map<String, Object> request,
      String key,
      String fallback,
      String defaultValue) {
    if (!request.containsKey(key)) {
      return fallback;
    }

    return HttpRequestParsers.defaultIfBlank(
        HttpRequestParsers.optionalString(request.get(key)), defaultValue);
  }

  private boolean resolveBoolean(Map<String, Object> request, String key, boolean fallback) {
    if (!request.containsKey(key)) {
      return fallback;
    }

    return HttpRequestParsers.optionalBoolean(request.get(key), key, fallback);
  }

  private BigDecimal resolveBigDecimal(
      Map<String, Object> request, String key, BigDecimal fallback) {
    if (!request.containsKey(key)) {
      return fallback;
    }

    return HttpRequestParsers.optionalBigDecimal(request.get(key), key);
  }

  private OffsetDateTime resolveOffsetDateTime(
      Map<String, Object> request, String key, OffsetDateTime fallback) {
    if (!request.containsKey(key)) {
      return fallback;
    }

    return HttpRequestParsers.optionalOffsetDateTime(request.get(key), key);
  }

  private String resolveTeamNameForPatch(
      Map<String, Object> request, PlayerProfileRow existing, boolean isWithTeam) {
    if (request.containsKey(PlayerProfileFields.TEAM_NAME)) {
      return normalizeTeamName(request.get(PlayerProfileFields.TEAM_NAME), isWithTeam);
    }

    if (request.containsKey(PlayerProfileFields.IS_WITH_TEAM)) {
      return isWithTeam ? normalizeTeamName(existing.teamName(), true) : null;
    }

    return existing.teamName();
  }

  private String normalizeTeamName(Object value, boolean isWithTeam) {
    String normalizedTeamName = HttpRequestParsers.optionalString(value);
    if (!isWithTeam) {
      return null;
    }

    if (normalizedTeamName == null || normalizedTeamName.isBlank()) {
      throw HttpErrors.badRequest("teamName is required when isWithTeam is true.");
    }

    return normalizedTeamName;
  }
}
