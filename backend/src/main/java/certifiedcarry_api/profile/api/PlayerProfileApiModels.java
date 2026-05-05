package certifiedcarry_api.profile.api;

import certifiedcarry_api.profile.PlayerProfileFields;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record PlayerProfileCreateRequest(
    String userId,
    String username,
    String profileImage,
    String game,
    String rank,
    Boolean allowPlayerChats,
    Boolean isWithTeam,
    String teamName,
    List<Object> rocketLeagueModes,
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
    Boolean isVerified,
    OffsetDateTime submittedAt,
    OffsetDateTime rankVerifiedAt,
    OffsetDateTime rankExpiresAt,
    OffsetDateTime rankExpiryReminderSentAt,
    OffsetDateTime updatedAt) {

  Map<String, Object> toServiceRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put(PlayerProfileFields.USER_ID, userId);
    request.put(PlayerProfileFields.USERNAME, username);
    request.put(PlayerProfileFields.PROFILE_IMAGE, profileImage);
    request.put(PlayerProfileFields.GAME, game);
    request.put(PlayerProfileFields.RANK, rank);
    request.put(PlayerProfileFields.ALLOW_PLAYER_CHATS, allowPlayerChats);
    request.put(PlayerProfileFields.IS_WITH_TEAM, isWithTeam);
    request.put(PlayerProfileFields.TEAM_NAME, teamName);
    request.put(PlayerProfileFields.ROCKET_LEAGUE_MODES, rocketLeagueModes);
    request.put(PlayerProfileFields.PRIMARY_ROCKET_LEAGUE_MODE, primaryRocketLeagueMode);
    request.put(PlayerProfileFields.IN_GAME_ROLES, inGameRoles);
    request.put(PlayerProfileFields.IN_GAME_ROLE, inGameRole);
    request.put(PlayerProfileFields.RATING_VALUE, ratingValue);
    request.put(PlayerProfileFields.RATING_LABEL, ratingLabel);
    request.put(PlayerProfileFields.PROOF_IMAGE, proofImage);
    request.put(PlayerProfileFields.BIO, bio);
    request.put(PlayerProfileFields.CLIPS_URL, clipsUrl);
    request.put(PlayerProfileFields.RANK_VERIFICATION_STATUS, rankVerificationStatus);
    request.put(PlayerProfileFields.DECLINE_REASON, declineReason);
    request.put(PlayerProfileFields.DECLINED_AT, declinedAt);
    request.put(PlayerProfileFields.IS_VERIFIED, isVerified);
    request.put(PlayerProfileFields.SUBMITTED_AT, submittedAt);
    request.put(PlayerProfileFields.RANK_VERIFIED_AT, rankVerifiedAt);
    request.put(PlayerProfileFields.RANK_EXPIRES_AT, rankExpiresAt);
    request.put(PlayerProfileFields.RANK_EXPIRY_REMINDER_SENT_AT, rankExpiryReminderSentAt);
    request.put(PlayerProfileFields.UPDATED_AT, updatedAt);
    return request;
  }
}

final class PlayerProfilePatchRequest {

  private String userId;
  private boolean userIdSet;
  private String username;
  private boolean usernameSet;
  private String profileImage;
  private boolean profileImageSet;
  private String game;
  private boolean gameSet;
  private String rank;
  private boolean rankSet;
  private Boolean allowPlayerChats;
  private boolean allowPlayerChatsSet;
  private Boolean isWithTeam;
  private boolean isWithTeamSet;
  private String teamName;
  private boolean teamNameSet;
  private List<Object> rocketLeagueModes;
  private boolean rocketLeagueModesSet;
  private String primaryRocketLeagueMode;
  private boolean primaryRocketLeagueModeSet;
  private List<String> inGameRoles;
  private boolean inGameRolesSet;
  private String inGameRole;
  private boolean inGameRoleSet;
  private BigDecimal ratingValue;
  private boolean ratingValueSet;
  private String ratingLabel;
  private boolean ratingLabelSet;
  private String proofImage;
  private boolean proofImageSet;
  private String bio;
  private boolean bioSet;
  private String clipsUrl;
  private boolean clipsUrlSet;
  private String rankVerificationStatus;
  private boolean rankVerificationStatusSet;
  private String declineReason;
  private boolean declineReasonSet;
  private OffsetDateTime declinedAt;
  private boolean declinedAtSet;
  private Boolean isVerified;
  private boolean isVerifiedSet;
  private OffsetDateTime submittedAt;
  private boolean submittedAtSet;
  private OffsetDateTime rankVerifiedAt;
  private boolean rankVerifiedAtSet;
  private OffsetDateTime rankExpiresAt;
  private boolean rankExpiresAtSet;
  private OffsetDateTime rankExpiryReminderSentAt;
  private boolean rankExpiryReminderSentAtSet;
  private OffsetDateTime updatedAt;
  private boolean updatedAtSet;

  @JsonSetter(PlayerProfileFields.USER_ID)
  void setUserId(String userId) { this.userId = userId; userIdSet = true; }
  @JsonSetter(PlayerProfileFields.USERNAME)
  void setUsername(String username) { this.username = username; usernameSet = true; }
  @JsonSetter(PlayerProfileFields.PROFILE_IMAGE)
  void setProfileImage(String profileImage) { this.profileImage = profileImage; profileImageSet = true; }
  @JsonSetter(PlayerProfileFields.GAME)
  void setGame(String game) { this.game = game; gameSet = true; }
  @JsonSetter(PlayerProfileFields.RANK)
  void setRank(String rank) { this.rank = rank; rankSet = true; }
  @JsonSetter(PlayerProfileFields.ALLOW_PLAYER_CHATS)
  void setAllowPlayerChats(Boolean allowPlayerChats) { this.allowPlayerChats = allowPlayerChats; allowPlayerChatsSet = true; }
  @JsonSetter(PlayerProfileFields.IS_WITH_TEAM)
  void setIsWithTeam(Boolean isWithTeam) { this.isWithTeam = isWithTeam; isWithTeamSet = true; }
  @JsonSetter(PlayerProfileFields.TEAM_NAME)
  void setTeamName(String teamName) { this.teamName = teamName; teamNameSet = true; }
  @JsonSetter(PlayerProfileFields.ROCKET_LEAGUE_MODES)
  void setRocketLeagueModes(List<Object> rocketLeagueModes) { this.rocketLeagueModes = rocketLeagueModes; rocketLeagueModesSet = true; }
  @JsonSetter(PlayerProfileFields.PRIMARY_ROCKET_LEAGUE_MODE)
  void setPrimaryRocketLeagueMode(String primaryRocketLeagueMode) { this.primaryRocketLeagueMode = primaryRocketLeagueMode; primaryRocketLeagueModeSet = true; }
  @JsonSetter(PlayerProfileFields.IN_GAME_ROLES)
  void setInGameRoles(List<String> inGameRoles) { this.inGameRoles = inGameRoles; inGameRolesSet = true; }
  @JsonSetter(PlayerProfileFields.IN_GAME_ROLE)
  void setInGameRole(String inGameRole) { this.inGameRole = inGameRole; inGameRoleSet = true; }
  @JsonSetter(PlayerProfileFields.RATING_VALUE)
  void setRatingValue(BigDecimal ratingValue) { this.ratingValue = ratingValue; ratingValueSet = true; }
  @JsonSetter(PlayerProfileFields.RATING_LABEL)
  void setRatingLabel(String ratingLabel) { this.ratingLabel = ratingLabel; ratingLabelSet = true; }
  @JsonSetter(PlayerProfileFields.PROOF_IMAGE)
  void setProofImage(String proofImage) { this.proofImage = proofImage; proofImageSet = true; }
  @JsonSetter(PlayerProfileFields.BIO)
  void setBio(String bio) { this.bio = bio; bioSet = true; }
  @JsonSetter(PlayerProfileFields.CLIPS_URL)
  void setClipsUrl(String clipsUrl) { this.clipsUrl = clipsUrl; clipsUrlSet = true; }
  @JsonSetter(PlayerProfileFields.RANK_VERIFICATION_STATUS)
  void setRankVerificationStatus(String rankVerificationStatus) { this.rankVerificationStatus = rankVerificationStatus; rankVerificationStatusSet = true; }
  @JsonSetter(PlayerProfileFields.DECLINE_REASON)
  void setDeclineReason(String declineReason) { this.declineReason = declineReason; declineReasonSet = true; }
  @JsonSetter(PlayerProfileFields.DECLINED_AT)
  void setDeclinedAt(OffsetDateTime declinedAt) { this.declinedAt = declinedAt; declinedAtSet = true; }
  @JsonSetter(PlayerProfileFields.IS_VERIFIED)
  void setIsVerified(Boolean isVerified) { this.isVerified = isVerified; isVerifiedSet = true; }
  @JsonSetter(PlayerProfileFields.SUBMITTED_AT)
  void setSubmittedAt(OffsetDateTime submittedAt) { this.submittedAt = submittedAt; submittedAtSet = true; }
  @JsonSetter(PlayerProfileFields.RANK_VERIFIED_AT)
  void setRankVerifiedAt(OffsetDateTime rankVerifiedAt) { this.rankVerifiedAt = rankVerifiedAt; rankVerifiedAtSet = true; }
  @JsonSetter(PlayerProfileFields.RANK_EXPIRES_AT)
  void setRankExpiresAt(OffsetDateTime rankExpiresAt) { this.rankExpiresAt = rankExpiresAt; rankExpiresAtSet = true; }
  @JsonSetter(PlayerProfileFields.RANK_EXPIRY_REMINDER_SENT_AT)
  void setRankExpiryReminderSentAt(OffsetDateTime rankExpiryReminderSentAt) { this.rankExpiryReminderSentAt = rankExpiryReminderSentAt; rankExpiryReminderSentAtSet = true; }
  @JsonSetter(PlayerProfileFields.UPDATED_AT)
  void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; updatedAtSet = true; }

  Map<String, Object> toServiceRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    if (userIdSet) request.put(PlayerProfileFields.USER_ID, userId);
    if (usernameSet) request.put(PlayerProfileFields.USERNAME, username);
    if (profileImageSet) request.put(PlayerProfileFields.PROFILE_IMAGE, profileImage);
    if (gameSet) request.put(PlayerProfileFields.GAME, game);
    if (rankSet) request.put(PlayerProfileFields.RANK, rank);
    if (allowPlayerChatsSet) request.put(PlayerProfileFields.ALLOW_PLAYER_CHATS, allowPlayerChats);
    if (isWithTeamSet) request.put(PlayerProfileFields.IS_WITH_TEAM, isWithTeam);
    if (teamNameSet) request.put(PlayerProfileFields.TEAM_NAME, teamName);
    if (rocketLeagueModesSet) request.put(PlayerProfileFields.ROCKET_LEAGUE_MODES, rocketLeagueModes);
    if (primaryRocketLeagueModeSet) request.put(PlayerProfileFields.PRIMARY_ROCKET_LEAGUE_MODE, primaryRocketLeagueMode);
    if (inGameRolesSet) request.put(PlayerProfileFields.IN_GAME_ROLES, inGameRoles);
    if (inGameRoleSet) request.put(PlayerProfileFields.IN_GAME_ROLE, inGameRole);
    if (ratingValueSet) request.put(PlayerProfileFields.RATING_VALUE, ratingValue);
    if (ratingLabelSet) request.put(PlayerProfileFields.RATING_LABEL, ratingLabel);
    if (proofImageSet) request.put(PlayerProfileFields.PROOF_IMAGE, proofImage);
    if (bioSet) request.put(PlayerProfileFields.BIO, bio);
    if (clipsUrlSet) request.put(PlayerProfileFields.CLIPS_URL, clipsUrl);
    if (rankVerificationStatusSet) request.put(PlayerProfileFields.RANK_VERIFICATION_STATUS, rankVerificationStatus);
    if (declineReasonSet) request.put(PlayerProfileFields.DECLINE_REASON, declineReason);
    if (declinedAtSet) request.put(PlayerProfileFields.DECLINED_AT, declinedAt);
    if (isVerifiedSet) request.put(PlayerProfileFields.IS_VERIFIED, isVerified);
    if (submittedAtSet) request.put(PlayerProfileFields.SUBMITTED_AT, submittedAt);
    if (rankVerifiedAtSet) request.put(PlayerProfileFields.RANK_VERIFIED_AT, rankVerifiedAt);
    if (rankExpiresAtSet) request.put(PlayerProfileFields.RANK_EXPIRES_AT, rankExpiresAt);
    if (rankExpiryReminderSentAtSet) request.put(PlayerProfileFields.RANK_EXPIRY_REMINDER_SENT_AT, rankExpiryReminderSentAt);
    if (updatedAtSet) request.put(PlayerProfileFields.UPDATED_AT, updatedAt);
    return request;
  }
}

record PlayerProfileResponse(
    String id,
    String userId,
    String username,
    String profileImage,
    String game,
    String rank,
    boolean allowPlayerChats,
    boolean isWithTeam,
    String teamName,
    List<Object> rocketLeagueModes,
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
    OffsetDateTime updatedAt) {

  static PlayerProfileResponse fromServiceRow(Map<String, Object> row) {
    return new PlayerProfileResponse(
        (String) row.get(PlayerProfileFields.ID),
        (String) row.get(PlayerProfileFields.USER_ID),
        (String) row.get(PlayerProfileFields.USERNAME),
        (String) row.get(PlayerProfileFields.PROFILE_IMAGE),
        (String) row.get(PlayerProfileFields.GAME),
        (String) row.get(PlayerProfileFields.RANK),
        Boolean.TRUE.equals(row.get(PlayerProfileFields.ALLOW_PLAYER_CHATS)),
        Boolean.TRUE.equals(row.get(PlayerProfileFields.IS_WITH_TEAM)),
        (String) row.get(PlayerProfileFields.TEAM_NAME),
        castObjectList(row.get(PlayerProfileFields.ROCKET_LEAGUE_MODES)),
        (String) row.get(PlayerProfileFields.PRIMARY_ROCKET_LEAGUE_MODE),
        castStringList(row.get(PlayerProfileFields.IN_GAME_ROLES)),
        (String) row.get(PlayerProfileFields.IN_GAME_ROLE),
        (BigDecimal) row.get(PlayerProfileFields.RATING_VALUE),
        (String) row.get(PlayerProfileFields.RATING_LABEL),
        (String) row.get(PlayerProfileFields.PROOF_IMAGE),
        (String) row.get(PlayerProfileFields.BIO),
        (String) row.get(PlayerProfileFields.CLIPS_URL),
        (String) row.get(PlayerProfileFields.RANK_VERIFICATION_STATUS),
        (String) row.get(PlayerProfileFields.DECLINE_REASON),
        (OffsetDateTime) row.get(PlayerProfileFields.DECLINED_AT),
        Boolean.TRUE.equals(row.get(PlayerProfileFields.IS_VERIFIED)),
        (OffsetDateTime) row.get(PlayerProfileFields.SUBMITTED_AT),
        (OffsetDateTime) row.get(PlayerProfileFields.RANK_VERIFIED_AT),
        (OffsetDateTime) row.get(PlayerProfileFields.RANK_EXPIRES_AT),
        (OffsetDateTime) row.get(PlayerProfileFields.RANK_EXPIRY_REMINDER_SENT_AT),
        (OffsetDateTime) row.get(PlayerProfileFields.CREATED_AT),
        (OffsetDateTime) row.get(PlayerProfileFields.UPDATED_AT));
  }

  static List<PlayerProfileResponse> fromServiceRows(List<Map<String, Object>> rows) {
    return rows.stream().map(PlayerProfileResponse::fromServiceRow).toList();
  }

  @SuppressWarnings("unchecked")
  private static List<String> castStringList(Object value) {
    return (List<String>) value;
  }

  @SuppressWarnings("unchecked")
  private static List<Object> castObjectList(Object value) {
    return (List<Object>) value;
  }
}
