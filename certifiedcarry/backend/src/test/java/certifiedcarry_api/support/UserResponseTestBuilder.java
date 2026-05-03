package certifiedcarry_api.support;

import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import java.time.OffsetDateTime;

public final class UserResponseTestBuilder {

  private String id = "1";
  private String fullName = "Test User";
  private String username = "test_user";
  private String personalEmail = "test-user@example.local";
  private String email = "test-user@example.local";
  private String organizationName;
  private UserRole role = UserRole.PLAYER;
  private UserStatus status = UserStatus.APPROVED;
  private String declineReason;
  private OffsetDateTime declinedAt;
  private RecruiterDmOpenness recruiterDmOpenness;
  private OffsetDateTime updatedAt = OffsetDateTime.parse("2026-04-04T00:00:00Z");
  private String legalConsentLocale = "en";
  private String termsVersionAccepted = "cc-terms-2026-04-04";
  private String privacyVersionAccepted = "cc-privacy-2026-04-04";

  private UserResponseTestBuilder() {}

  public static UserResponseTestBuilder aUser() {
    return new UserResponseTestBuilder();
  }

  public static UserResponseTestBuilder aPlayer() {
    return new UserResponseTestBuilder().withRole(UserRole.PLAYER).withStatus(UserStatus.APPROVED);
  }

  public UserResponseTestBuilder withId(String value) {
    this.id = value;
    return this;
  }

  public UserResponseTestBuilder withFullName(String value) {
    this.fullName = value;
    return this;
  }

  public UserResponseTestBuilder withUsername(String value) {
    this.username = value;
    return this;
  }

  public UserResponseTestBuilder withPersonalEmail(String value) {
    this.personalEmail = value;
    return this;
  }

  public UserResponseTestBuilder withEmail(String value) {
    this.email = value;
    return this;
  }

  public UserResponseTestBuilder withOrganizationName(String value) {
    this.organizationName = value;
    return this;
  }

  public UserResponseTestBuilder withRole(UserRole value) {
    this.role = value;
    return this;
  }

  public UserResponseTestBuilder withStatus(UserStatus value) {
    this.status = value;
    return this;
  }

  public UserResponseTestBuilder withDeclineReason(String value) {
    this.declineReason = value;
    return this;
  }

  public UserResponseTestBuilder withDeclinedAt(OffsetDateTime value) {
    this.declinedAt = value;
    return this;
  }

  public UserResponseTestBuilder withRecruiterDmOpenness(RecruiterDmOpenness value) {
    this.recruiterDmOpenness = value;
    return this;
  }

  public UserResponseTestBuilder withUpdatedAt(OffsetDateTime value) {
    this.updatedAt = value;
    return this;
  }

  public UserResponseTestBuilder withLegalConsentLocale(String value) {
    this.legalConsentLocale = value;
    return this;
  }

  public UserResponseTestBuilder withTermsVersionAccepted(String value) {
    this.termsVersionAccepted = value;
    return this;
  }

  public UserResponseTestBuilder withPrivacyVersionAccepted(String value) {
    this.privacyVersionAccepted = value;
    return this;
  }

  public UserResponse build() {
    return new UserResponse(
        id,
        fullName,
        username,
        personalEmail,
        email,
        organizationName,
        role,
        status,
        declineReason,
        declinedAt,
        recruiterDmOpenness,
        updatedAt,
        legalConsentLocale,
        termsVersionAccepted,
        privacyVersionAccepted);
  }
}
