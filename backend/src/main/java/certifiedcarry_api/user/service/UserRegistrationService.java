package certifiedcarry_api.user.service;

import certifiedcarry_api.shared.HttpErrors;
import certifiedcarry_api.shared.LegalVersionValidator;
import certifiedcarry_api.shared.TextNormalization;
import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.factory.UserCreationFactory;
import certifiedcarry_api.user.factory.UserFactorySelector;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.repo.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserRegistrationService {

  private static final String ADMIN_CREATE_MESSAGE =
      "Admin accounts cannot be created from registration.";
  private static final String LEGAL_CONSENT_REQUIRED_MESSAGE =
      "You must accept legal terms before registration.";
  private static final String UNIQUE_CREATE_CONFLICT_MESSAGE =
      "User already exists with one of the provided unique fields.";
  private static final String USERNAME_TAKEN_MESSAGE = "This username is already taken.";
  private static final String EMAIL_TAKEN_MESSAGE = "An account with this email already exists.";
  private static final String EMAIL_BLANK_MESSAGE = "email cannot be blank";
  private static final String USERNAME_REQUIRED_MESSAGE = "username is required for PLAYER";
  private static final String ORG_REQUIRED_MESSAGE = "organizationName is required for RECRUITER";
  private static final String LINKEDIN_REQUIRED_MESSAGE = "linkedinUrl is required for RECRUITER";
  private static final String LINKEDIN_BLANK_MESSAGE = "linkedinUrl cannot be blank";
  private static final String TERMS_VERSION_REQUIRED_MESSAGE =
      "termsVersionAccepted is required";
  private static final String PRIVACY_VERSION_REQUIRED_MESSAGE =
      "privacyVersionAccepted is required";
  private static final String FIREBASE_REQUIRED_MESSAGE =
      "Firebase authentication is required for registration.";
  private static final String FIREBASE_EMAIL_VERIFIED_MESSAGE =
      "Firebase account email must be verified before registration can be linked.";
  private static final String FIREBASE_EMAIL_REQUIRED_MESSAGE =
      "Firebase token email is required for registration linkage.";
  private static final String FIREBASE_EMAIL_MISMATCH_MESSAGE =
      "Firebase token email does not match the submitted registration email.";

  private final UserRepository userRepository;
  private final UserFactorySelector userFactorySelector;
  private final PasswordEncoder passwordEncoder;
  private final LegalVersionValidator legalVersionValidator;
  private final boolean firebaseEnabled;

  public UserRegistrationService(
      UserRepository userRepository,
      UserFactorySelector userFactorySelector,
      PasswordEncoder passwordEncoder,
      LegalVersionValidator legalVersionValidator,
      @Value("${firebase.enabled:false}") boolean firebaseEnabled) {
    this.userRepository = userRepository;
    this.userFactorySelector = userFactorySelector;
    this.passwordEncoder = passwordEncoder;
    this.legalVersionValidator = legalVersionValidator;
    this.firebaseEnabled = firebaseEnabled;
  }

  public UserResponse createUser(
      CreateUserRequest request,
      String firebaseUid,
      String firebaseEmail,
      Boolean firebaseEmailVerified) {
    if (request.role() == UserRole.ADMIN) {
      throw HttpErrors.badRequest(ADMIN_CREATE_MESSAGE);
    }

    if (!Boolean.TRUE.equals(request.legalConsentAccepted())) {
      throw HttpErrors.badRequest(LEGAL_CONSENT_REQUIRED_MESSAGE);
    }

    validateAcceptedLegalVersions(request);

    String firebaseUidForUser =
        resolveFirebaseUidForCreate(request, firebaseUid, firebaseEmail, firebaseEmailVerified);

    UserResponse relinkedExistingUser =
        maybeRelinkExistingUserForFirebaseRegistration(request, firebaseUidForUser);
    if (relinkedExistingUser != null) {
      return relinkedExistingUser;
    }

    validateUniqueForCreate(request);

    UserCreationFactory factory = userFactorySelector.getFactory(request.role());
    if (factory == null) {
      throw HttpErrors.badRequest("No user factory is registered for role " + request.role());
    }

    try {
      UserEntity candidate = factory.create(request, passwordEncoder);
      if (firebaseUidForUser != null) {
        candidate.setFirebaseUid(firebaseUidForUser);
      }
      if (request.role() == UserRole.RECRUITER) {
        candidate.setLinkedinUrl(
            UserFieldNormalizer.normalizeLinkedinUrl(request.linkedinUrl(), LINKEDIN_BLANK_MESSAGE));
      }
      UserEntity savedUser = userRepository.save(candidate);
      return UserResponseMapper.toResponse(savedUser);
    } catch (IllegalArgumentException exception) {
      throw HttpErrors.badRequest(exception.getMessage());
    } catch (DataIntegrityViolationException exception) {
      throw HttpErrors.conflict(UNIQUE_CREATE_CONFLICT_MESSAGE);
    }
  }

  private void validateUniqueForCreate(CreateUserRequest request) {
    if (request.role() == UserRole.PLAYER) {
      String username =
          UserFieldNormalizer.requireNonBlank(request.username(), USERNAME_REQUIRED_MESSAGE);
      String personalEmail =
          UserFieldNormalizer.normalizeEmail(request.personalEmail(), EMAIL_BLANK_MESSAGE);

      assertUsernameAvailable(username, null);
      assertEmailAvailable(personalEmail, null);
      return;
    }

    if (request.role() == UserRole.RECRUITER) {
      String email = UserFieldNormalizer.normalizeEmail(request.email(), EMAIL_BLANK_MESSAGE);
      UserFieldNormalizer.requireNonBlank(request.organizationName(), ORG_REQUIRED_MESSAGE);
      UserFieldNormalizer.normalizeLinkedinUrl(request.linkedinUrl(), LINKEDIN_REQUIRED_MESSAGE);
      assertEmailAvailable(email, null);
    }
  }

  private UserResponse maybeRelinkExistingUserForFirebaseRegistration(
      CreateUserRequest request, String firebaseUidForUser) {
    if (firebaseUidForUser == null) {
      return null;
    }

    UserEntity existingWithFirebaseUid = userRepository.findByFirebaseUid(firebaseUidForUser).orElse(null);
    if (existingWithFirebaseUid != null) {
      return UserResponseMapper.toResponse(existingWithFirebaseUid);
    }

    UserEntity existingByLoginEmail = findExistingUserByRegistrationEmail(request);
    if (existingByLoginEmail == null) {
      return null;
    }

    if (existingByLoginEmail.getRole() != request.role()) {
      throw HttpErrors.conflict(EMAIL_TAKEN_MESSAGE);
    }

    if (request.role() == UserRole.PLAYER) {
      String requestedUsername =
          UserFieldNormalizer.requireNonBlank(request.username(), USERNAME_REQUIRED_MESSAGE);
      String existingUsername =
          UserFieldNormalizer.requireNonBlank(
              existingByLoginEmail.getUsername(), "Existing account has an invalid username.");
      if (!existingUsername.equalsIgnoreCase(requestedUsername)) {
        throw HttpErrors.conflict(EMAIL_TAKEN_MESSAGE);
      }
    }

    existingByLoginEmail.setFirebaseUid(firebaseUidForUser);
    UserEntity saved = userRepository.save(existingByLoginEmail);
    return UserResponseMapper.toResponse(saved);
  }

  private UserEntity findExistingUserByRegistrationEmail(CreateUserRequest request) {
    if (request.role() == UserRole.PLAYER) {
      String personalEmail =
          UserFieldNormalizer.normalizeEmail(request.personalEmail(), EMAIL_BLANK_MESSAGE);
      return userRepository.findByPersonalEmailIgnoreCase(personalEmail).orElse(null);
    }

    if (request.role() == UserRole.RECRUITER) {
      String email = UserFieldNormalizer.normalizeEmail(request.email(), EMAIL_BLANK_MESSAGE);
      return userRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    return null;
  }

  private void assertUsernameAvailable(String username, Long excludeUserId) {
    boolean alreadyExists =
        excludeUserId == null
            ? userRepository.existsByUsernameIgnoreCase(username)
            : userRepository.existsByUsernameIgnoreCaseAndIdNot(username, excludeUserId);

    if (alreadyExists) {
      throw HttpErrors.conflict(USERNAME_TAKEN_MESSAGE);
    }
  }

  private void assertEmailAvailable(String email, Long excludeUserId) {
    boolean usedInEmail =
        excludeUserId == null
            ? userRepository.existsByEmailIgnoreCase(email)
            : userRepository.existsByEmailIgnoreCaseAndIdNot(email, excludeUserId);

    boolean usedInPersonalEmail =
        excludeUserId == null
            ? userRepository.existsByPersonalEmailIgnoreCase(email)
            : userRepository.existsByPersonalEmailIgnoreCaseAndIdNot(email, excludeUserId);

    if (usedInEmail || usedInPersonalEmail) {
      throw HttpErrors.conflict(EMAIL_TAKEN_MESSAGE);
    }
  }

  private void validateAcceptedLegalVersions(CreateUserRequest request) {
    String acceptedTermsVersion =
        UserFieldNormalizer.requireNonBlank(
            request.termsVersionAccepted(), TERMS_VERSION_REQUIRED_MESSAGE);
    String acceptedPrivacyVersion =
        UserFieldNormalizer.requireNonBlank(
            request.privacyVersionAccepted(), PRIVACY_VERSION_REQUIRED_MESSAGE);

    legalVersionValidator.validateAcceptedVersions(acceptedTermsVersion, acceptedPrivacyVersion);
  }

  private String resolveFirebaseUidForCreate(
      CreateUserRequest request,
      String firebaseUid,
      String firebaseEmail,
      Boolean firebaseEmailVerified) {
    String normalizedFirebaseUid = TextNormalization.trimToNull(firebaseUid);
    if (normalizedFirebaseUid == null) {
      if (firebaseEnabled) {
        throw HttpErrors.badRequest(FIREBASE_REQUIRED_MESSAGE);
      }

      return null;
    }

    if (!Boolean.TRUE.equals(firebaseEmailVerified)) {
      throw HttpErrors.badRequest(FIREBASE_EMAIL_VERIFIED_MESSAGE);
    }

    String normalizedFirebaseEmail = UserFieldNormalizer.normalizeOptionalEmail(firebaseEmail);
    if (normalizedFirebaseEmail == null) {
      throw HttpErrors.badRequest(FIREBASE_EMAIL_REQUIRED_MESSAGE);
    }

    String requestedLoginEmail =
        switch (request.role()) {
          case PLAYER -> request.personalEmail();
          case RECRUITER -> request.email();
          default -> null;
        };

    String normalizedRequestedLoginEmail =
        UserFieldNormalizer.normalizeEmail(requestedLoginEmail, EMAIL_BLANK_MESSAGE);
    if (!normalizedRequestedLoginEmail.equals(normalizedFirebaseEmail)) {
      throw HttpErrors.badRequest(FIREBASE_EMAIL_MISMATCH_MESSAGE);
    }

    return normalizedFirebaseUid;
  }
}
