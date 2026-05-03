package certifiedcarry_api.user.factory;

import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.RegistrationSource;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class PlayerUserCreationFactory implements UserCreationFactory {

  @Override
  public UserRole supportedRole() {
    return UserRole.PLAYER;
  }

  @Override
  public UserEntity create(CreateUserRequest request, PasswordEncoder passwordEncoder) {
    UserEntity user = new UserEntity();
    user.setFullName(UserFactoryFieldUtils.requireText(request.fullName(), "fullName is required"));
    user.setUsername(
      UserFactoryFieldUtils.requireText(request.username(), "username is required for PLAYER"));
    user.setPersonalEmail(
      UserFactoryFieldUtils.normalizeEmail(
        UserFactoryFieldUtils.requireText(
          request.personalEmail(), "personalEmail is required for PLAYER")));
    user.setEmail(null);
    user.setOrganizationName(null);
    user.setLinkedinUrl(null);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(UserRole.PLAYER);
    user.setStatus(UserStatus.APPROVED);
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
    user.setRecruiterDmOpenness(RecruiterDmOpenness.CLOSED);
    user.setDeclineReason(null);
    user.setDeclinedAt(null);
    return user;
  }
}
