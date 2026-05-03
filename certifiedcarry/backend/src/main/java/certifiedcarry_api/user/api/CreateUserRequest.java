package certifiedcarry_api.user.api;

import certifiedcarry_api.user.model.ConsentSource;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record CreateUserRequest(
    String id,
    @NotBlank(message = "fullName is required") String fullName,
    String username,
    @Email(message = "personalEmail must be valid") String personalEmail,
    @Email(message = "email must be valid") String email,
    String organizationName,
    String linkedinUrl,
    RecruiterDmOpenness recruiterDmOpenness,
    @NotBlank(message = "password is required") String password,
    @NotNull(message = "role is required") UserRole role,
    @NotNull(message = "legalConsentAccepted is required") Boolean legalConsentAccepted,
    @NotNull(message = "legalConsentAcceptedAt is required") OffsetDateTime legalConsentAcceptedAt,
    @NotBlank(message = "legalConsentLocale is required") String legalConsentLocale,
    @NotNull(message = "legalConsentSource is required") ConsentSource legalConsentSource,
    @NotBlank(message = "termsVersionAccepted is required") String termsVersionAccepted,
    @NotBlank(message = "privacyVersionAccepted is required") String privacyVersionAccepted) {}
