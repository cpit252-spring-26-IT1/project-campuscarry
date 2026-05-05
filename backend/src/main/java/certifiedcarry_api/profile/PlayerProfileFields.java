package certifiedcarry_api.profile;

public final class PlayerProfileFields {

  public static final String ID = "id";
  public static final String USER_ID = "userId";
  public static final String USERNAME = "username";
  public static final String PROFILE_IMAGE = "profileImage";
  public static final String GAME = "game";
  public static final String RANK = "rank";
  public static final String ALLOW_PLAYER_CHATS = "allowPlayerChats";
  public static final String IS_WITH_TEAM = "isWithTeam";
  public static final String TEAM_NAME = "teamName";
  public static final String ROCKET_LEAGUE_MODES = "rocketLeagueModes";
  public static final String PRIMARY_ROCKET_LEAGUE_MODE = "primaryRocketLeagueMode";
  public static final String IN_GAME_ROLES = "inGameRoles";
  public static final String IN_GAME_ROLE = "inGameRole";
  public static final String RATING_VALUE = "ratingValue";
  public static final String RATING_LABEL = "ratingLabel";
  public static final String PROOF_IMAGE = "proofImage";
  public static final String BIO = "bio";
  public static final String CLIPS_URL = "clipsUrl";
  public static final String RANK_VERIFICATION_STATUS = "rankVerificationStatus";
  public static final String DECLINE_REASON = "declineReason";
  public static final String DECLINED_AT = "declinedAt";
  public static final String IS_VERIFIED = "isVerified";
  public static final String SUBMITTED_AT = "submittedAt";
  public static final String RANK_VERIFIED_AT = "rankVerifiedAt";
  public static final String RANK_EXPIRES_AT = "rankExpiresAt";
  public static final String RANK_EXPIRY_REMINDER_SENT_AT = "rankExpiryReminderSentAt";
  public static final String CREATED_AT = "createdAt";
  public static final String UPDATED_AT = "updatedAt";

  public static final String DEFAULT_RATING_LABEL = "MMR";
  public static final String DEFAULT_EMPTY_TEXT = "";
  public static final String DEFAULT_RANK_STATUS = "NOT_SUBMITTED";

  public static final String STATUS_NOT_SUBMITTED = "NOT_SUBMITTED";
  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_APPROVED = "APPROVED";
  public static final String STATUS_DECLINED = "DECLINED";

  private PlayerProfileFields() {
  }
}
