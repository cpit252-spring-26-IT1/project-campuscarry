package certifiedcarry_api.user.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "username", columnDefinition = "citext")
  private String username;

  @Column(name = "personal_email", columnDefinition = "citext")
  private String personalEmail;

  @Column(name = "email", columnDefinition = "citext")
  private String email;

  @Column(name = "firebase_uid")
  private String firebaseUid;

  @Column(name = "organization_name")
  private String organizationName;

  @Column(name = "linkedin_url")
  private String linkedinUrl;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(
      name = "recruiter_dm_openness",
      nullable = false,
      columnDefinition = "recruiter_dm_openness_enum")
  private RecruiterDmOpenness recruiterDmOpenness;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "role", nullable = false, columnDefinition = "user_role_enum")
  private UserRole role;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "status", nullable = false, columnDefinition = "user_status_enum")
  private UserStatus status;

  @Column(name = "decline_reason")
  private String declineReason;

  @Column(name = "declined_at")
  private OffsetDateTime declinedAt;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "registration_source", nullable = false, columnDefinition = "registration_source_enum")
  private RegistrationSource registrationSource;

  @Column(name = "legal_consent_accepted", nullable = false)
  private Boolean legalConsentAccepted;

  @Column(name = "legal_consent_accepted_at", nullable = false)
  private OffsetDateTime legalConsentAcceptedAt;

  @Column(name = "legal_consent_locale", nullable = false)
  private String legalConsentLocale;

  @Enumerated(EnumType.STRING)
  @JdbcTypeCode(SqlTypes.NAMED_ENUM)
  @Column(name = "legal_consent_source", nullable = false, columnDefinition = "consent_source_enum")
  private ConsentSource legalConsentSource;

  @Column(name = "terms_version_accepted", nullable = false)
  private String termsVersionAccepted;

  @Column(name = "privacy_version_accepted", nullable = false)
  private String privacyVersionAccepted;

  @Column(name = "created_at", insertable = false, updatable = false)
  private OffsetDateTime createdAt;

  @Column(name = "updated_at", insertable = false)
  private OffsetDateTime updatedAt;
}
