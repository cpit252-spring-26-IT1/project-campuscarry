package certifiedcarry_api.leaderboard.api;

import certifiedcarry_api.leaderboard.LeaderboardFields;
import com.fasterxml.jackson.annotation.JsonSetter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record LeaderboardCreateRequest(
    String userId,
    String username,
    String game,
    String rank,
    String role,
    BigDecimal ratingValue,
    String ratingLabel,
    OffsetDateTime updatedAt) {

  Map<String, Object> toServiceRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    request.put(LeaderboardFields.USER_ID, userId);
    request.put(LeaderboardFields.USERNAME, username);
    request.put(LeaderboardFields.GAME, game);
    request.put(LeaderboardFields.RANK, rank);
    request.put(LeaderboardFields.ROLE, role);
    request.put(LeaderboardFields.RATING_VALUE, ratingValue);
    request.put(LeaderboardFields.RATING_LABEL, ratingLabel);
    request.put(LeaderboardFields.UPDATED_AT, updatedAt);
    return request;
  }
}

final class LeaderboardPatchRequest {

  private String userId;
  private boolean userIdSet;
  private String username;
  private boolean usernameSet;
  private String game;
  private boolean gameSet;
  private String rank;
  private boolean rankSet;
  private String role;
  private boolean roleSet;
  private BigDecimal ratingValue;
  private boolean ratingValueSet;
  private String ratingLabel;
  private boolean ratingLabelSet;
  private OffsetDateTime updatedAt;
  private boolean updatedAtSet;

  @JsonSetter(LeaderboardFields.USER_ID)
  void setUserId(String userId) {
    this.userId = userId;
    userIdSet = true;
  }

  @JsonSetter(LeaderboardFields.USERNAME)
  void setUsername(String username) {
    this.username = username;
    usernameSet = true;
  }

  @JsonSetter(LeaderboardFields.GAME)
  void setGame(String game) {
    this.game = game;
    gameSet = true;
  }

  @JsonSetter(LeaderboardFields.RANK)
  void setRank(String rank) {
    this.rank = rank;
    rankSet = true;
  }

  @JsonSetter(LeaderboardFields.ROLE)
  void setRole(String role) {
    this.role = role;
    roleSet = true;
  }

  @JsonSetter(LeaderboardFields.RATING_VALUE)
  void setRatingValue(BigDecimal ratingValue) {
    this.ratingValue = ratingValue;
    ratingValueSet = true;
  }

  @JsonSetter(LeaderboardFields.RATING_LABEL)
  void setRatingLabel(String ratingLabel) {
    this.ratingLabel = ratingLabel;
    ratingLabelSet = true;
  }

  @JsonSetter(LeaderboardFields.UPDATED_AT)
  void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
    updatedAtSet = true;
  }

  Map<String, Object> toServiceRequest() {
    Map<String, Object> request = new LinkedHashMap<>();
    if (userIdSet) {
      request.put(LeaderboardFields.USER_ID, userId);
    }
    if (usernameSet) {
      request.put(LeaderboardFields.USERNAME, username);
    }
    if (gameSet) {
      request.put(LeaderboardFields.GAME, game);
    }
    if (rankSet) {
      request.put(LeaderboardFields.RANK, rank);
    }
    if (roleSet) {
      request.put(LeaderboardFields.ROLE, role);
    }
    if (ratingValueSet) {
      request.put(LeaderboardFields.RATING_VALUE, ratingValue);
    }
    if (ratingLabelSet) {
      request.put(LeaderboardFields.RATING_LABEL, ratingLabel);
    }
    if (updatedAtSet) {
      request.put(LeaderboardFields.UPDATED_AT, updatedAt);
    }
    return request;
  }
}

record LeaderboardResponse(
    String id,
    String userId,
    String username,
    String game,
    String rank,
    String role,
    BigDecimal ratingValue,
    String ratingLabel,
    OffsetDateTime updatedAt) {

  static LeaderboardResponse fromServiceRow(Map<String, Object> row) {
    return new LeaderboardResponse(
        (String) row.get(LeaderboardFields.ID),
        (String) row.get(LeaderboardFields.USER_ID),
        (String) row.get(LeaderboardFields.USERNAME),
        (String) row.get(LeaderboardFields.GAME),
        (String) row.get(LeaderboardFields.RANK),
        (String) row.get(LeaderboardFields.ROLE),
        (BigDecimal) row.get(LeaderboardFields.RATING_VALUE),
        (String) row.get(LeaderboardFields.RATING_LABEL),
        (OffsetDateTime) row.get(LeaderboardFields.UPDATED_AT));
  }

  static List<LeaderboardResponse> fromServiceRows(List<Map<String, Object>> rows) {
    return rows.stream().map(LeaderboardResponse::fromServiceRow).toList();
  }
}
