package certifiedcarry_api.queue;

public final class PendingQueueFields {

  public static final String ID = "id";
  public static final String USER_ID = "userId";
  public static final String USERNAME = "username";
  public static final String FULL_NAME = "fullName";
  public static final String EMAIL = "email";
  public static final String LINKEDIN_URL = "linkedinUrl";
  public static final String ORGANIZATION_NAME = "organizationName";
  public static final String SUBMITTED_AT = "submittedAt";
  public static final String LEGAL_CONSENT_ACCEPTED_AT = "legalConsentAcceptedAt";
  public static final String LEGAL_CONSENT_LOCALE = "legalConsentLocale";
  public static final String TERMS_VERSION_ACCEPTED = "termsVersionAccepted";
  public static final String PRIVACY_VERSION_ACCEPTED = "privacyVersionAccepted";

  public static final String GAME = "game";
  public static final String CLAIMED_RANK = "claimedRank";
  public static final String IN_GAME_ROLES = "inGameRoles";
  public static final String IN_GAME_ROLE = "inGameRole";
  public static final String RATING_VALUE = "ratingValue";
  public static final String RATING_LABEL = "ratingLabel";
  public static final String ROCKET_LEAGUE_MODES = "rocketLeagueModes";
  public static final String PRIMARY_ROCKET_LEAGUE_MODE = "primaryRocketLeagueMode";
  public static final String PROOF_IMAGE = "proofImage";
  public static final String STATUS = "status";
  public static final String RESOLVED_AT = "resolvedAt";
  public static final String DECLINE_REASON = "declineReason";
  public static final String EDITED_AFTER_DECLINE = "editedAfterDecline";
  public static final String EDITED_AT = "editedAt";
  public static final String UPDATED_AT = "updatedAt";

  public static final String STATUS_PENDING = "PENDING";
  public static final String STATUS_APPROVED = "APPROVED";
  public static final String STATUS_DECLINED = "DECLINED";

  public static final String DEFAULT_RATING_LABEL = "MMR";
  public static final String DEFAULT_EMPTY_TEXT = "";

  private PendingQueueFields() {
  }
}
