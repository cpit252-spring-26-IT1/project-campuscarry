package certifiedcarry_api.notification.service;

import certifiedcarry_api.shared.TextNormalization;
import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import certifiedcarry_api.user.repo.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class NotificationOrchestratorService {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationOrchestratorService.class);

  private final UserRepository userRepository;
  private final JdbcTemplate jdbcTemplate;
  private final NotificationEmailService notificationEmailService;
  private final UserNotificationStateService userNotificationStateService;
  private final int rankExpiryReminderHours;
  private final int rankExpiryBatchSize;
  private final String adminLoginAlertEmail;

  public NotificationOrchestratorService(
      UserRepository userRepository,
      JdbcTemplate jdbcTemplate,
      NotificationEmailService notificationEmailService,
      UserNotificationStateService userNotificationStateService,
      @Value("${notifications.rank-expiry-reminder-hours:24}") int rankExpiryReminderHours,
      @Value("${notifications.rank-expiry-batch-size:100}") int rankExpiryBatchSize,
      @Value("${notifications.security.admin-login-alert-email:}") String adminLoginAlertEmail) {
    this.userRepository = userRepository;
    this.jdbcTemplate = jdbcTemplate;
    this.notificationEmailService = notificationEmailService;
    this.userNotificationStateService = userNotificationStateService;
    this.rankExpiryReminderHours = Math.max(1, rankExpiryReminderHours);
    this.rankExpiryBatchSize = Math.max(1, rankExpiryBatchSize);
    this.adminLoginAlertEmail = normalizeOptional(adminLoginAlertEmail);
  }

  public void markUserLoggedIn(long userId) {
    userNotificationStateService.markLoggedIn(userId);
    registerAfterCommit(() -> handleAdminLoginAlert(userId));
  }

  public void markUserLoggedOut(long userId) {
    userNotificationStateService.markLoggedOut(userId);
  }

  public void recordUserActivity(long userId) {
    userNotificationStateService.recordActivity(userId);
  }

  public void registerChatMessageCreatedAfterCommit(long recipientUserId) {
    registerAfterCommit(() -> handleChatMessageCreated(recipientUserId));
  }

  public void registerAccountApprovedAfterCommit(UserEntity user, UserStatus previousStatus) {
    if (user == null || user.getId() == null) {
      return;
    }

    if (previousStatus == UserStatus.APPROVED || user.getStatus() != UserStatus.APPROVED) {
      return;
    }

    long approvedUserId = user.getId();
    registerAfterCommit(() -> handleAccountApproved(approvedUserId));
  }

  @Scheduled(fixedDelayString = "${notifications.rank-expiry-check-delay-ms:900000}")
  public void processRankValidationExpiries() {
    sendRankExpiryReminders();
    expireRankValidations();
  }

  private void handleChatMessageCreated(long recipientUserId) {
    if (!notificationEmailService.isEmailEnabled()) {
      return;
    }

    Optional<UserEntity> recipient = userRepository.findById(recipientUserId);
    if (recipient.isEmpty()) {
      return;
    }

    UserEntity recipientUser = recipient.get();
    String notificationEmail = resolveNotificationEmail(recipientUser);
    if (notificationEmail == null) {
      return;
    }

    boolean reserved =
        userNotificationStateService.reserveUnreadChatNotificationIfEligible(recipientUserId);
    if (!reserved) {
      return;
    }

    int unreadCount = countUnreadMessages(recipientUserId);
    boolean sent =
        notificationEmailService.sendUnreadChatNotification(
            notificationEmail, recipientUser.getFullName(), unreadCount);

    if (!sent) {
      userNotificationStateService.clearUnreadChatNotificationFlag(recipientUserId);
    }
  }

  private void handleAccountApproved(long userId) {
    Optional<UserEntity> approvedUser = userRepository.findById(userId);
    if (approvedUser.isEmpty()) {
      return;
    }

    UserEntity user = approvedUser.get();
    String notificationEmail = resolveNotificationEmail(user);
    if (notificationEmail == null) {
      return;
    }

    String roleLabel =
        switch (user.getRole()) {
          case PLAYER -> "player";
          case RECRUITER -> "recruiter";
          case ADMIN -> "account";
        };

    notificationEmailService.sendAccountApprovedNotification(
        notificationEmail, user.getFullName(), roleLabel);
  }

  private void handleAdminLoginAlert(long userId) {
    if (adminLoginAlertEmail == null || !notificationEmailService.isEmailEnabled()) {
      return;
    }

    Optional<UserEntity> adminUser = userRepository.findById(userId);
    if (adminUser.isEmpty()) {
      return;
    }

    UserEntity user = adminUser.get();
    if (user.getRole() != UserRole.ADMIN) {
      return;
    }

    String adminEmail = resolveNotificationEmail(user);
    notificationEmailService.sendAdminLoginAlert(
        adminLoginAlertEmail, user.getFullName(), adminEmail, userId, OffsetDateTime.now());
  }

  private void sendRankExpiryReminders() {
    if (!notificationEmailService.isEmailEnabled()) {
      return;
    }

    List<RankReminderCandidate> candidates = loadRankReminderCandidates();
    for (RankReminderCandidate candidate : candidates) {
      String notificationEmail =
          resolveNotificationEmail(
              candidate.role(), candidate.personalEmail(), candidate.workEmail());
      if (notificationEmail == null) {
        continue;
      }

      boolean sent =
          notificationEmailService.sendRankExpiryReminder(
              notificationEmail,
              candidate.fullName(),
              candidate.game(),
              candidate.rank(),
              candidate.rankExpiresAt());

      if (sent) {
        markReminderSent(candidate.profileId());
      }
    }
  }

  private List<RankReminderCandidate> loadRankReminderCandidates() {
    return jdbcTemplate.query(
        """
        SELECT
          pp.id AS profile_id,
          pp.user_id,
          pp.game,
          pp.rank,
          pp.rank_expires_at,
          u.full_name,
          u.role,
          u.personal_email,
          u.email
        FROM player_profiles pp
        JOIN users u ON u.id = pp.user_id
        WHERE pp.rank_expires_at IS NOT NULL
          AND pp.rank_verification_status = 'APPROVED'
          AND pp.is_verified = TRUE
          AND pp.rank_expires_at > NOW()
          AND pp.rank_expires_at <= NOW() + (? * INTERVAL '1 hour')
          AND pp.rank_expiry_reminder_sent_at IS NULL
        ORDER BY pp.rank_expires_at ASC
        LIMIT ?
        """,
        (resultSet, rowNumber) ->
            new RankReminderCandidate(
                resultSet.getLong("profile_id"),
                resultSet.getLong("user_id"),
                resultSet.getString("full_name"),
                resultSet.getString("role"),
                resultSet.getString("personal_email"),
                resultSet.getString("email"),
                resultSet.getString("game"),
                resultSet.getString("rank"),
                resultSet.getObject("rank_expires_at", OffsetDateTime.class)),
        rankExpiryReminderHours,
        rankExpiryBatchSize);
  }

  private void markReminderSent(long profileId) {
    jdbcTemplate.update(
        """
        UPDATE player_profiles
        SET
          rank_expiry_reminder_sent_at = NOW(),
          updated_at = NOW()
        WHERE id = ?
        """,
        profileId);
  }

  private void expireRankValidations() {
    List<ExpiredRankCandidate> expiredCandidates = loadExpiredRankCandidates();
    for (ExpiredRankCandidate candidate : expiredCandidates) {
      jdbcTemplate.update(
          """
          UPDATE player_profiles
          SET
            rank_verification_status = CAST('NOT_SUBMITTED' AS rank_verification_status_enum),
            is_verified = FALSE,
            rank_verified_at = NULL,
            rank_expires_at = NULL,
            rank_expiry_reminder_sent_at = NULL,
            decline_reason = '',
            declined_at = NULL,
            updated_at = NOW()
          WHERE id = ?
          """,
          candidate.profileId());

      jdbcTemplate.update("DELETE FROM leaderboard_entries WHERE user_id = ?", candidate.userId());
    }
  }

  private List<ExpiredRankCandidate> loadExpiredRankCandidates() {
    return jdbcTemplate.query(
        """
        SELECT
          id,
          user_id
        FROM player_profiles
        WHERE rank_expires_at IS NOT NULL
          AND rank_expires_at <= NOW()
          AND (rank_verification_status = 'APPROVED' OR is_verified = TRUE)
        ORDER BY rank_expires_at ASC
        LIMIT ?
        """,
        (resultSet, rowNumber) ->
            new ExpiredRankCandidate(resultSet.getLong("id"), resultSet.getLong("user_id")),
        rankExpiryBatchSize);
  }

  private int countUnreadMessages(long recipientUserId) {
    Integer unreadCount =
        jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM chat_messages
            WHERE recipient_id = ?
              AND read_at IS NULL
            """,
            Integer.class,
            recipientUserId);

    return unreadCount == null ? 1 : Math.max(1, unreadCount);
  }

  private String resolveNotificationEmail(UserEntity user) {
    if (user == null || user.getRole() == null) {
      return null;
    }

    if (user.getRole() == UserRole.PLAYER) {
      String playerEmail = normalizeOptional(user.getPersonalEmail());
      if (playerEmail != null) {
        return playerEmail;
      }

      return normalizeOptional(user.getEmail());
    }

    String workEmail = normalizeOptional(user.getEmail());
    if (workEmail != null) {
      return workEmail;
    }

    return normalizeOptional(user.getPersonalEmail());
  }

  private String resolveNotificationEmail(String role, String personalEmail, String workEmail) {
    String normalizedRole = normalizeOptional(role);
    if ("PLAYER".equalsIgnoreCase(normalizedRole)) {
      String playerEmail = normalizeOptional(personalEmail);
      if (playerEmail != null) {
        return playerEmail;
      }

      return normalizeOptional(workEmail);
    }

    String recruiterEmail = normalizeOptional(workEmail);
    if (recruiterEmail != null) {
      return recruiterEmail;
    }

    return normalizeOptional(personalEmail);
  }

  private void registerAfterCommit(Runnable action) {
    Runnable safeAction =
        () -> {
          try {
            action.run();
          } catch (RuntimeException exception) {
            LOGGER.error("Notification side-effect failed", exception);
          }
        };

    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      safeAction.run();
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            safeAction.run();
          }
        });
  }

  private String normalizeOptional(String value) {
    return TextNormalization.trimToNull(value);
  }

  private record RankReminderCandidate(
      long profileId,
      long userId,
      String fullName,
      String role,
      String personalEmail,
      String workEmail,
      String game,
      String rank,
      OffsetDateTime rankExpiresAt) {}

  private record ExpiredRankCandidate(long profileId, long userId) {}
}
