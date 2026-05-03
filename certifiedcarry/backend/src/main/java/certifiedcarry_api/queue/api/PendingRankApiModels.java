package certifiedcarry_api.queue.api;

import certifiedcarry_api.queue.PendingQueueFields;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record PendingRankCreateRequest(
    String userId,
    String username,
    String fullName,
    String game,
    String claimedRank,
    List<String> inGameRoles,
    String inGameRole,
    BigDecimal ratingValue,
    String ratingLabel,
    List<String> rocketLeagueModes,
    String primaryRocketLeagueMode,
    String proofImage,
    String status,
    OffsetDateTime submittedAt,
    OffsetDateTime resolvedAt,
    String declineReason,
    Boolean editedAfterDecline,
    OffsetDateTime editedAt) {

  Map<String, Object> toServiceRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put(PendingQueueFields.USER_ID, userId);
    request.put(PendingQueueFields.USERNAME, username);
    request.put(PendingQueueFields.FULL_NAME, fullName);
    request.put(PendingQueueFields.GAME, game);
    request.put(PendingQueueFields.CLAIMED_RANK, claimedRank);
    request.put(PendingQueueFields.IN_GAME_ROLES, inGameRoles);
    request.put(PendingQueueFields.IN_GAME_ROLE, inGameRole);
    request.put(PendingQueueFields.RATING_VALUE, ratingValue);
    request.put(PendingQueueFields.RATING_LABEL, ratingLabel);
    request.put(PendingQueueFields.ROCKET_LEAGUE_MODES, rocketLeagueModes);
    request.put(PendingQueueFields.PRIMARY_ROCKET_LEAGUE_MODE, primaryRocketLeagueMode);
    request.put(PendingQueueFields.PROOF_IMAGE, proofImage);
    request.put(PendingQueueFields.STATUS, status);
    request.put(PendingQueueFields.SUBMITTED_AT, submittedAt);
    request.put(PendingQueueFields.RESOLVED_AT, resolvedAt);
    request.put(PendingQueueFields.DECLINE_REASON, declineReason);
    request.put(PendingQueueFields.EDITED_AFTER_DECLINE, editedAfterDecline);
    request.put(PendingQueueFields.EDITED_AT, editedAt);
    return request;
  }
}

final class PendingRankPatchRequest {

  private String userId;
  private boolean userIdSet;
  private String username;
  private boolean usernameSet;
  private String fullName;
  private boolean fullNameSet;
  private String game;
  private boolean gameSet;
  private String claimedRank;
  private boolean claimedRankSet;
  private List<String> inGameRoles;
  private boolean inGameRolesSet;
  private String inGameRole;
  private boolean inGameRoleSet;
  private BigDecimal ratingValue;
  private boolean ratingValueSet;
  private String ratingLabel;
  private boolean ratingLabelSet;
  private List<String> rocketLeagueModes;
  private boolean rocketLeagueModesSet;
  private String primaryRocketLeagueMode;
  private boolean primaryRocketLeagueModeSet;
  private String proofImage;
  private boolean proofImageSet;
  private String status;
  private boolean statusSet;
  private OffsetDateTime submittedAt;
  private boolean submittedAtSet;
  private OffsetDateTime resolvedAt;
  private boolean resolvedAtSet;
  private String declineReason;
  private boolean declineReasonSet;
  private Boolean editedAfterDecline;
  private boolean editedAfterDeclineSet;
  private OffsetDateTime editedAt;
  private boolean editedAtSet;

  @JsonSetter(PendingQueueFields.USER_ID)
  void setUserId(String userId) {
    this.userId = userId;
    userIdSet = true;
  }

  @JsonSetter(PendingQueueFields.USERNAME)
  void setUsername(String username) {
    this.username = username;
    usernameSet = true;
  }

  @JsonSetter(PendingQueueFields.FULL_NAME)
  void setFullName(String fullName) {
    this.fullName = fullName;
    fullNameSet = true;
  }

  @JsonSetter(PendingQueueFields.GAME)
  void setGame(String game) {
    this.game = game;
    gameSet = true;
  }

  @JsonSetter(PendingQueueFields.CLAIMED_RANK)
  void setClaimedRank(String claimedRank) {
    this.claimedRank = claimedRank;
    claimedRankSet = true;
  }

  @JsonSetter(PendingQueueFields.IN_GAME_ROLES)
  void setInGameRoles(List<String> inGameRoles) {
    this.inGameRoles = inGameRoles;
    inGameRolesSet = true;
  }

  @JsonSetter(PendingQueueFields.IN_GAME_ROLE)
  void setInGameRole(String inGameRole) {
    this.inGameRole = inGameRole;
    inGameRoleSet = true;
  }

  @JsonSetter(PendingQueueFields.RATING_VALUE)
  void setRatingValue(BigDecimal ratingValue) {
    this.ratingValue = ratingValue;
    ratingValueSet = true;
  }

  @JsonSetter(PendingQueueFields.RATING_LABEL)
  void setRatingLabel(String ratingLabel) {
    this.ratingLabel = ratingLabel;
    ratingLabelSet = true;
  }

  @JsonSetter(PendingQueueFields.ROCKET_LEAGUE_MODES)
  void setRocketLeagueModes(List<String> rocketLeagueModes) {
    this.rocketLeagueModes = rocketLeagueModes;
    rocketLeagueModesSet = true;
  }

  @JsonSetter(PendingQueueFields.PRIMARY_ROCKET_LEAGUE_MODE)
  void setPrimaryRocketLeagueMode(String primaryRocketLeagueMode) {
    this.primaryRocketLeagueMode = primaryRocketLeagueMode;
    primaryRocketLeagueModeSet = true;
  }

  @JsonSetter(PendingQueueFields.PROOF_IMAGE)
  void setProofImage(String proofImage) {
    this.proofImage = proofImage;
    proofImageSet = true;
  }

  @JsonSetter(PendingQueueFields.STATUS)
  void setStatus(String status) {
    this.status = status;
    statusSet = true;
  }

  @JsonSetter(PendingQueueFields.SUBMITTED_AT)
  void setSubmittedAt(OffsetDateTime submittedAt) {
    this.submittedAt = submittedAt;
    submittedAtSet = true;
  }

  @JsonSetter(PendingQueueFields.RESOLVED_AT)
  void setResolvedAt(OffsetDateTime resolvedAt) {
    this.resolvedAt = resolvedAt;
    resolvedAtSet = true;
  }

  @JsonSetter(PendingQueueFields.DECLINE_REASON)
  void setDeclineReason(String declineReason) {
    this.declineReason = declineReason;
    declineReasonSet = true;
  }

  @JsonSetter(PendingQueueFields.EDITED_AFTER_DECLINE)
  void setEditedAfterDecline(Boolean editedAfterDecline) {
    this.editedAfterDecline = editedAfterDecline;
    editedAfterDeclineSet = true;
  }

  @JsonSetter(PendingQueueFields.EDITED_AT)
  void setEditedAt(OffsetDateTime editedAt) {
    this.editedAt = editedAt;
    editedAtSet = true;
  }

  Map<String, Object> toServiceRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    if (userIdSet) {
      request.put(PendingQueueFields.USER_ID, userId);
    }
    if (usernameSet) {
      request.put(PendingQueueFields.USERNAME, username);
    }
    if (fullNameSet) {
      request.put(PendingQueueFields.FULL_NAME, fullName);
    }
    if (gameSet) {
      request.put(PendingQueueFields.GAME, game);
    }
    if (claimedRankSet) {
      request.put(PendingQueueFields.CLAIMED_RANK, claimedRank);
    }
    if (inGameRolesSet) {
      request.put(PendingQueueFields.IN_GAME_ROLES, inGameRoles);
    }
    if (inGameRoleSet) {
      request.put(PendingQueueFields.IN_GAME_ROLE, inGameRole);
    }
    if (ratingValueSet) {
      request.put(PendingQueueFields.RATING_VALUE, ratingValue);
    }
    if (ratingLabelSet) {
      request.put(PendingQueueFields.RATING_LABEL, ratingLabel);
    }
    if (rocketLeagueModesSet) {
      request.put(PendingQueueFields.ROCKET_LEAGUE_MODES, rocketLeagueModes);
    }
    if (primaryRocketLeagueModeSet) {
      request.put(PendingQueueFields.PRIMARY_ROCKET_LEAGUE_MODE, primaryRocketLeagueMode);
    }
    if (proofImageSet) {
      request.put(PendingQueueFields.PROOF_IMAGE, proofImage);
    }
    if (statusSet) {
      request.put(PendingQueueFields.STATUS, status);
    }
    if (submittedAtSet) {
      request.put(PendingQueueFields.SUBMITTED_AT, submittedAt);
    }
    if (resolvedAtSet) {
      request.put(PendingQueueFields.RESOLVED_AT, resolvedAt);
    }
    if (declineReasonSet) {
      request.put(PendingQueueFields.DECLINE_REASON, declineReason);
    }
    if (editedAfterDeclineSet) {
      request.put(PendingQueueFields.EDITED_AFTER_DECLINE, editedAfterDecline);
    }
    if (editedAtSet) {
      request.put(PendingQueueFields.EDITED_AT, editedAt);
    }
    return request;
  }
}

record PendingRankResponse(
    String id,
    String userId,
    String username,
    String fullName,
    String game,
    String claimedRank,
    List<String> inGameRoles,
    String inGameRole,
    BigDecimal ratingValue,
    String ratingLabel,
    List<String> rocketLeagueModes,
    String primaryRocketLeagueMode,
    String proofImage,
    String status,
    OffsetDateTime submittedAt,
    OffsetDateTime resolvedAt,
    String declineReason,
    boolean editedAfterDecline,
    OffsetDateTime editedAt,
    OffsetDateTime updatedAt) {

  static PendingRankResponse fromServiceRow(Map<String, Object> row) {
    return new PendingRankResponse(
        (String) row.get(PendingQueueFields.ID),
        (String) row.get(PendingQueueFields.USER_ID),
        (String) row.get(PendingQueueFields.USERNAME),
        (String) row.get(PendingQueueFields.FULL_NAME),
        (String) row.get(PendingQueueFields.GAME),
        (String) row.get(PendingQueueFields.CLAIMED_RANK),
        castStringList(row.get(PendingQueueFields.IN_GAME_ROLES)),
        (String) row.get(PendingQueueFields.IN_GAME_ROLE),
        (BigDecimal) row.get(PendingQueueFields.RATING_VALUE),
        (String) row.get(PendingQueueFields.RATING_LABEL),
        castStringList(row.get(PendingQueueFields.ROCKET_LEAGUE_MODES)),
        (String) row.get(PendingQueueFields.PRIMARY_ROCKET_LEAGUE_MODE),
        (String) row.get(PendingQueueFields.PROOF_IMAGE),
        (String) row.get(PendingQueueFields.STATUS),
        (OffsetDateTime) row.get(PendingQueueFields.SUBMITTED_AT),
        (OffsetDateTime) row.get(PendingQueueFields.RESOLVED_AT),
        (String) row.get(PendingQueueFields.DECLINE_REASON),
        Boolean.TRUE.equals(row.get(PendingQueueFields.EDITED_AFTER_DECLINE)),
        (OffsetDateTime) row.get(PendingQueueFields.EDITED_AT),
        (OffsetDateTime) row.get(PendingQueueFields.UPDATED_AT));
  }

  static List<PendingRankResponse> fromServiceRows(List<Map<String, Object>> rows) {
    return rows.stream().map(PendingRankResponse::fromServiceRow).toList();
  }

  @SuppressWarnings("unchecked")
  private static List<String> castStringList(Object value) {
    return (List<String>) value;
  }
}
