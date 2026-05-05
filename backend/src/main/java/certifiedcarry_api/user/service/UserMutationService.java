package certifiedcarry_api.user.service;

import certifiedcarry_api.notification.service.NotificationOrchestratorService;
import certifiedcarry_api.shared.HttpErrors;
import certifiedcarry_api.user.api.UpdateUserRequest;
import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.model.RecruiterDmOpenness;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import certifiedcarry_api.user.repo.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class UserMutationService {

  private static final Logger LOGGER = LoggerFactory.getLogger(UserMutationService.class);
  private static final String USER_NOT_FOUND_PREFIX = "User not found for id ";
  private static final String ADMIN_DELETE_MESSAGE =
      "Admin accounts cannot be deleted from this endpoint.";
  private static final String UNIQUE_PATCH_CONFLICT_MESSAGE =
      "Patch violates a unique or integrity constraint.";
  private static final String USERNAME_TAKEN_MESSAGE = "This username is already taken.";
  private static final String EMAIL_TAKEN_MESSAGE = "An account with this email already exists.";
  private static final String ID_NUMERIC_MESSAGE = "id must be a numeric string";
  private static final String EMAIL_BLANK_MESSAGE = "email cannot be blank";
  private static final String LINKEDIN_BLANK_MESSAGE = "linkedinUrl cannot be blank";
  private static final String RECRUITER_ROLE_REQUIRED_MESSAGE = "Recruiter role is required.";
  private static final String DELETE_CONFLICT_MESSAGE =
      "Unable to delete user due to related records.";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JdbcTemplate jdbcTemplate;
  private final FirebaseAccountDeletionQueueService firebaseAccountDeletionQueueService;
  private final NotificationOrchestratorService notificationOrchestratorService;

  public UserMutationService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JdbcTemplate jdbcTemplate,
      FirebaseAccountDeletionQueueService firebaseAccountDeletionQueueService,
      NotificationOrchestratorService notificationOrchestratorService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jdbcTemplate = jdbcTemplate;
    this.firebaseAccountDeletionQueueService = firebaseAccountDeletionQueueService;
    this.notificationOrchestratorService = notificationOrchestratorService;
  }

  @Transactional
  public UserResponse patchUser(String userId, UpdateUserRequest request) {
    Long parsedUserId = UserFieldNormalizer.parseUserId(userId, ID_NUMERIC_MESSAGE);

    UserEntity user =
        userRepository
            .findById(parsedUserId)
            .orElseThrow(() -> HttpErrors.notFound(USER_NOT_FOUND_PREFIX + userId));

    UserStatus previousStatus = user.getStatus();

    if (request.fullName() != null) {
      user.setFullName(UserFieldNormalizer.requireNonBlank(request.fullName(), "fullName cannot be blank"));
    }

    if (request.username() != null) {
      if (user.getRole() != UserRole.PLAYER) {
        throw HttpErrors.badRequest("username can only be updated for PLAYER users.");
      }

      String normalizedUsername =
          UserFieldNormalizer.requireNonBlank(request.username(), "username cannot be blank");
      assertUsernameAvailable(normalizedUsername, user.getId());
      user.setUsername(normalizedUsername);
    }

    if (request.personalEmail() != null) {
      if (user.getRole() != UserRole.PLAYER) {
        throw HttpErrors.badRequest("personalEmail can only be updated for PLAYER users.");
      }

      String normalizedEmail =
          UserFieldNormalizer.normalizeEmail(request.personalEmail(), EMAIL_BLANK_MESSAGE);
      assertEmailAvailable(normalizedEmail, user.getId());
      user.setPersonalEmail(normalizedEmail);
    }

    if (request.email() != null) {
      if (user.getRole() == UserRole.PLAYER) {
        throw HttpErrors.badRequest("email cannot be updated for PLAYER users.");
      }

      String normalizedEmail =
          UserFieldNormalizer.normalizeEmail(request.email(), EMAIL_BLANK_MESSAGE);
      assertEmailAvailable(normalizedEmail, user.getId());
      user.setEmail(normalizedEmail);
    }

    if (request.organizationName() != null) {
      if (user.getRole() != UserRole.RECRUITER) {
        throw HttpErrors.badRequest("organizationName can only be updated for RECRUITER users.");
      }

      user.setOrganizationName(
          UserFieldNormalizer.requireNonBlank(
              request.organizationName(), "organizationName cannot be blank"));
    }

    if (request.linkedinUrl() != null) {
      if (user.getRole() != UserRole.RECRUITER) {
        throw HttpErrors.badRequest("linkedinUrl can only be updated for RECRUITER users.");
      }

      user.setLinkedinUrl(
          UserFieldNormalizer.normalizeLinkedinUrl(request.linkedinUrl(), LINKEDIN_BLANK_MESSAGE));
    }

    if (request.password() != null) {
      user.setPasswordHash(
          passwordEncoder.encode(
              UserFieldNormalizer.requireNonBlank(
                  request.password(), "password cannot be blank")));
    }

    if (request.status() != null) {
      user.setStatus(request.status());
    }

    if (request.declineReason() != null) {
      user.setDeclineReason(request.declineReason().trim());
    }

    if (request.declinedAt() != null) {
      user.setDeclinedAt(request.declinedAt());
    }

    if (request.recruiterDmOpenness() != null) {
      if (user.getRole() != UserRole.RECRUITER) {
        throw HttpErrors.badRequest(
            "recruiterDmOpenness can only be updated for RECRUITER users.");
      }

      user.setRecruiterDmOpenness(request.recruiterDmOpenness());
    }

    try {
      UserEntity updated = userRepository.save(user);
      notificationOrchestratorService.registerAccountApprovedAfterCommit(updated, previousStatus);
      return UserResponseMapper.toResponse(updated);
    } catch (DataIntegrityViolationException exception) {
      throw HttpErrors.conflict(UNIQUE_PATCH_CONFLICT_MESSAGE);
    }
  }

  public RecruiterDmOpenness getRecruiterDmOpenness(long actorUserId) {
    UserEntity recruiter = requireRecruiter(actorUserId);
    return recruiter.getRecruiterDmOpenness();
  }

  @Transactional
  public RecruiterDmOpenness updateRecruiterDmOpenness(
      long actorUserId, RecruiterDmOpenness recruiterDmOpenness) {
    if (recruiterDmOpenness == null) {
      throw HttpErrors.badRequest("recruiterDmOpenness is required.");
    }

    UserEntity recruiter = requireRecruiter(actorUserId);
    recruiter.setRecruiterDmOpenness(recruiterDmOpenness);
    UserEntity updated = userRepository.save(recruiter);
    return updated.getRecruiterDmOpenness();
  }

  @Transactional
  public void deleteUser(String userId) {
    Long parsedUserId = UserFieldNormalizer.parseUserId(userId, ID_NUMERIC_MESSAGE);

    UserEntity user =
        userRepository
            .findById(parsedUserId)
            .orElseThrow(() -> HttpErrors.notFound(USER_NOT_FOUND_PREFIX + userId));

    if (user.getRole() == UserRole.ADMIN) {
      throw HttpErrors.badRequest(ADMIN_DELETE_MESSAGE);
    }

    String firebaseUid = user.getFirebaseUid();
    String firebaseEmail = getUserLoginEmail(user);

    try {
      purgeUserRelatedRecords(parsedUserId);
      userRepository.delete(user);
      userRepository.flush();
      registerFirebaseCleanupAfterCommit(parsedUserId, firebaseUid, firebaseEmail);
    } catch (DataIntegrityViolationException exception) {
      throw HttpErrors.conflict(DELETE_CONFLICT_MESSAGE);
    }
  }

  private void registerFirebaseCleanupAfterCommit(
      Long backendUserId, String firebaseUid, String firebaseEmail) {
    Runnable cleanup =
        () -> {
          try {
            firebaseAccountDeletionQueueService.deleteOrQueue(
                backendUserId, firebaseUid, firebaseEmail);
          } catch (RuntimeException exception) {
            LOGGER.error(
                "Unexpected failure during Firebase cleanup for deleted backendUserId={}",
                backendUserId,
                exception);
          }
        };

    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      cleanup.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            cleanup.run();
          }
        });
  }

  private void purgeUserRelatedRecords(Long userId) {
    executeDeleteIfTableExists("chat_messages", "sender_id = ? OR recipient_id = ?", userId, userId);
    executeDeleteIfTableExists(
        "chat_threads",
        "participant_user_id_1 = ? OR participant_user_id_2 = ? OR initiated_by_id = ? OR last_sender_id = ?",
        userId,
        userId,
        userId,
        userId);
    executeDeleteIfTableExists("leaderboard_entries", "user_id = ?", userId);
    executeDeleteIfTableExists("pending_ranks", "user_id = ?", userId);
    executeDeleteIfTableExists("pending_recruiters", "user_id = ?", userId);
    executeDeleteIfTableExists("player_profiles", "user_id = ?", userId);
    executeDeleteIfTableExists("user_consent_events", "user_id = ?", userId);
  }

  private void executeDeleteIfTableExists(String tableName, String whereClause, Object... args) {
    if (!tableExists(tableName)) {
      return;
    }

    String sql = "DELETE FROM " + tableName + " WHERE " + whereClause;
    jdbcTemplate.update(sql, args);
  }

  private boolean tableExists(String tableName) {
    String relationName =
        jdbcTemplate.queryForObject("SELECT to_regclass(?)", String.class, "public." + tableName);
    return relationName != null;
  }

  private String getUserLoginEmail(UserEntity user) {
    if (user.getRole() == UserRole.PLAYER) {
      return user.getPersonalEmail();
    }

    return user.getEmail();
  }

  private UserEntity requireRecruiter(long userId) {
    UserEntity user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> HttpErrors.notFound(USER_NOT_FOUND_PREFIX + userId));

    if (user.getRole() != UserRole.RECRUITER) {
      throw HttpErrors.badRequest(RECRUITER_ROLE_REQUIRED_MESSAGE);
    }

    return user;
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
}
