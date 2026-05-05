package certifiedcarry_api.profile.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

record PlayerProfilePayload(
    long userId,
    String username,
    String profileImage,
    String game,
    String rank,
    boolean allowPlayerChats,
    boolean isWithTeam,
    String teamName,
    List<String> rocketLeagueModes,
    String primaryRocketLeagueMode,
    List<String> inGameRoles,
    String inGameRole,
    BigDecimal ratingValue,
    String ratingLabel,
    String proofImage,
    String bio,
    String clipsUrl,
    String rankVerificationStatus,
    String declineReason,
    OffsetDateTime declinedAt,
    boolean isVerified,
    OffsetDateTime submittedAt,
    OffsetDateTime rankVerifiedAt,
    OffsetDateTime rankExpiresAt,
    OffsetDateTime rankExpiryReminderSentAt,
    OffsetDateTime updatedAt) {}

record VerificationState(
    OffsetDateTime rankVerifiedAt,
    OffsetDateTime rankExpiresAt,
    OffsetDateTime rankExpiryReminderSentAt) {

  static VerificationState unverified() {
    return new VerificationState(null, null, null);
  }
}

record PlayerProfileRow(
    long id,
    long userId,
    String username,
    String profileImage,
    String game,
    String rank,
    boolean allowPlayerChats,
    boolean isWithTeam,
    String teamName,
    List<String> rocketLeagueModes,
    String primaryRocketLeagueMode,
    List<String> inGameRoles,
    String inGameRole,
    BigDecimal ratingValue,
    String ratingLabel,
    String proofImage,
    String bio,
    String clipsUrl,
    String rankVerificationStatus,
    String declineReason,
    OffsetDateTime declinedAt,
    boolean isVerified,
    OffsetDateTime submittedAt,
    OffsetDateTime rankVerifiedAt,
    OffsetDateTime rankExpiresAt,
    OffsetDateTime rankExpiryReminderSentAt,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
