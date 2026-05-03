package certifiedcarry_api.user.api;

import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
    String id,
    String fullName,
    String username,
    String personalEmail,
    String email,
    String organizationName,
    UserRole role,
    UserStatus status,
    String declineReason,
    OffsetDateTime declinedAt,
    RecruiterDmOpenness recruiterDmOpenness,
    OffsetDateTime updatedAt,
    String legalConsentLocale,
    String termsVersionAccepted,
    String privacyVersionAccepted) {}
