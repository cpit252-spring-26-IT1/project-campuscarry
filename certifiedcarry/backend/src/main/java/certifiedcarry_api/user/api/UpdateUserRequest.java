package certifiedcarry_api.user.api;

import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserStatus;
import java.time.OffsetDateTime;

public record UpdateUserRequest(
    String fullName,
    String username,
    String personalEmail,
    String email,
    String organizationName,
    String linkedinUrl,
    String password,
    UserStatus status,
    String declineReason,
    OffsetDateTime declinedAt,
    RecruiterDmOpenness recruiterDmOpenness) {}
