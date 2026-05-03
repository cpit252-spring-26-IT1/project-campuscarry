package certifiedcarry_api.user.service;

import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;

final class UserResponseMapper {

  private UserResponseMapper() {}

  static UserResponse toResponse(UserEntity user) {
    return new UserResponse(
        String.valueOf(user.getId()),
        user.getFullName(),
        user.getUsername(),
        user.getPersonalEmail(),
        user.getEmail(),
        user.getOrganizationName(),
        user.getRole(),
        user.getStatus(),
        user.getDeclineReason(),
        user.getDeclinedAt(),
        user.getRecruiterDmOpenness(),
        user.getUpdatedAt(),
        user.getLegalConsentLocale(),
        user.getTermsVersionAccepted(),
        user.getPrivacyVersionAccepted());
  }

  static UserResponse sanitizeForNonAdmin(UserResponse user, long actorUserId) {
    if (String.valueOf(actorUserId).equals(user.id())) {
      return user;
    }

    return new UserResponse(
        user.id(),
        user.fullName(),
        user.username(),
        null,
        null,
        user.organizationName(),
        user.role(),
        user.status(),
        null,
        null,
        user.role() == UserRole.RECRUITER ? user.recruiterDmOpenness() : null,
        user.updatedAt(),
        null,
        null,
        null);
  }
}
