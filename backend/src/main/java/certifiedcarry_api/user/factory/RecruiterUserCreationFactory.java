package certifiedcarry_api.user.factory;

import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.RegistrationSource;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class RecruiterUserCreationFactory implements UserCreationFactory {

  @Override
  public UserRole supportedRole() {
    return UserRole.RECRUITER;
  }

  @Override
  public UserEntity create(CreateUserRequest request, PasswordEncoder passwordEncoder) {
    UserEntity user = new UserEntity();
    user.setFullName(UserFactoryFieldUtils.requireText(request.fullName(), "fullName is required"));
    user.setUsername(null);
    user.setPersonalEmail(null);
    user.setEmail(
      UserFactoryFieldUtils.normalizeEmail(
        UserFactoryFieldUtils.requireText(
          request.email(), "email is required for RECRUITER")));
    user.setOrganizationName(
      UserFactoryFieldUtils.requireText(
        request.organizationName(), "organizationName is required for RECRUITER"));
    user.setLinkedinUrl(
      UserFactoryFieldUtils.requireText(
        request.linkedinUrl(), "linkedinUrl is required for RECRUITER"));
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(UserRole.RECRUITER);
    user.setStatus(UserStatus.PENDING);
    user.setRegistrationSource(RegistrationSource.WEB_REGISTRATION);
    user.setLegalConsentAccepted(Boolean.TRUE);
    user.setLegalConsentAcceptedAt(request.legalConsentAcceptedAt());
    user.setLegalConsentLocale(
        UserFactoryFieldUtils.requireText(
            request.legalConsentLocale(), "legalConsentLocale is required"));
    user.setLegalConsentSource(request.legalConsentSource());
    user.setTermsVersionAccepted(
        UserFactoryFieldUtils.requireText(
            request.termsVersionAccepted(), "termsVersionAccepted is required"));
    user.setPrivacyVersionAccepted(
        UserFactoryFieldUtils.requireText(
            request.privacyVersionAccepted(), "privacyVersionAccepted is required"));
    user.setRecruiterDmOpenness(
      request.recruiterDmOpenness() == null
        ? RecruiterDmOpenness.CLOSED
        : request.recruiterDmOpenness());
    user.setDeclineReason("");
    user.setDeclinedAt(null);
    return user;
  }
}
