package certifiedcarry_api.notification.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.model.UserStatus;
import certifiedcarry_api.user.repo.UserRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class NotificationOrchestratorServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private JdbcTemplate jdbcTemplate;

  @Mock
  private NotificationEmailService notificationEmailService;

  @Mock
  private UserNotificationStateService userNotificationStateService;

  @Test
  void loginAndApprovalHooksRespectRoleEmailAndEnabledState() {
    NotificationOrchestratorService service =
        new NotificationOrchestratorService(
            userRepository,
            jdbcTemplate,
            notificationEmailService,
            userNotificationStateService,
            24,
            10,
            "security@certifiedcarry.me");

    UserEntity admin = user(7L, UserRole.ADMIN, "Admin", null, "admin@example.com");
    UserEntity recruiter = user(8L, UserRole.RECRUITER, "Scout", null, "recruiter@example.com");

    when(notificationEmailService.isEmailEnabled()).thenReturn(true);
    when(userRepository.findById(7L)).thenReturn(Optional.of(admin));
    when(userRepository.findById(8L)).thenReturn(Optional.of(recruiter));

    service.markUserLoggedIn(7L);
    verify(userNotificationStateService).markLoggedIn(7L);
    verify(notificationEmailService)
        .sendAdminLoginAlert(
            eq("security@certifiedcarry.me"),
            eq("Admin"),
            eq("admin@example.com"),
            eq(7L),
            any(OffsetDateTime.class));

    service.markUserLoggedOut(7L);
    service.recordUserActivity(7L);
    verify(userNotificationStateService).markLoggedOut(7L);
    verify(userNotificationStateService).recordActivity(7L);

    service.registerAccountApprovedAfterCommit(recruiter, UserStatus.PENDING);
    verify(notificationEmailService)
        .sendAccountApprovedNotification("recruiter@example.com", "Scout", "recruiter");

    service.registerAccountApprovedAfterCommit(recruiter, UserStatus.APPROVED);
    verify(notificationEmailService, never())
        .sendAccountApprovedNotification("recruiter@example.com", "Scout", "account");
  }

  @Test
  void chatNotificationFlowReservesCountsAndClearsOnSendFailure() {
    NotificationOrchestratorService service =
        new NotificationOrchestratorService(
            userRepository,
            jdbcTemplate,
            notificationEmailService,
            userNotificationStateService,
            24,
            10,
            "");

    UserEntity player = user(13L, UserRole.PLAYER, "Bluy", "player@example.com", null);
    when(notificationEmailService.isEmailEnabled()).thenReturn(true);
    when(userRepository.findById(13L)).thenReturn(Optional.of(player));
    when(userNotificationStateService.reserveUnreadChatNotificationIfEligible(13L)).thenReturn(true);
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(13L))).thenReturn(null);
    when(notificationEmailService.sendUnreadChatNotification("player@example.com", "Bluy", 1))
        .thenReturn(false);

    service.registerChatMessageCreatedAfterCommit(13L);

    verify(notificationEmailService).sendUnreadChatNotification("player@example.com", "Bluy", 1);
    verify(userNotificationStateService).clearUnreadChatNotificationFlag(13L);
  }

  @Test
  void rankReminderAndExpiryProcessingSendAndCleanUpExpectedRecords() throws SQLException {
    NotificationOrchestratorService service =
        new NotificationOrchestratorService(
            userRepository,
            jdbcTemplate,
            notificationEmailService,
            userNotificationStateService,
            24,
            2,
            "");

    when(notificationEmailService.isEmailEnabled()).thenReturn(true);
    when(notificationEmailService.sendRankExpiryReminder(
            "player@example.com",
            "Bluy",
            "Valorant",
            "Immortal",
            OffsetDateTime.parse("2026-05-14T00:00:00Z")))
        .thenReturn(true);

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              if (((String) invocation.getArgument(0)).contains("rank_expiry_reminder_sent_at IS NULL")) {
                return List.of(mapper.mapRow(mockReminderResultSet(), 0));
              }
              return List.of(mapper.mapRow(mockExpiredResultSet(), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), any());

    doAnswer(
            invocation -> {
              RowMapper<?> mapper = invocation.getArgument(1);
              if (((String) invocation.getArgument(0)).contains("rank_expiry_reminder_sent_at IS NULL")) {
                return List.of(mapper.mapRow(mockReminderResultSet(), 0));
              }
              return List.of(mapper.mapRow(mockExpiredResultSet(), 0));
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), anyInt(), anyInt());

    assertDoesNotThrow(service::processRankValidationExpiries);

    verify(notificationEmailService)
        .sendRankExpiryReminder(
            "player@example.com",
            "Bluy",
            "Valorant",
            "Immortal",
            OffsetDateTime.parse("2026-05-14T00:00:00Z"));
    verify(jdbcTemplate, times(2)).update(anyString(), eq(91L));
    verify(jdbcTemplate).update(anyString(), eq(92L));
  }

  @Test
  void chatAndApprovalHooksShortCircuitWhenUsersOrEligibilityAreMissing() {
    NotificationOrchestratorService service =
        new NotificationOrchestratorService(
            userRepository,
            jdbcTemplate,
            notificationEmailService,
            userNotificationStateService,
            24,
            10,
            "");

    when(notificationEmailService.isEmailEnabled()).thenReturn(false, true, true, true);
    when(userRepository.findById(14L)).thenReturn(Optional.empty());
    when(userRepository.findById(15L))
        .thenReturn(Optional.of(user(15L, UserRole.PLAYER, "No Email", "   ", "   ")));
    when(userRepository.findById(16L))
        .thenReturn(Optional.of(user(16L, UserRole.PLAYER, "Bluy", "player@example.com", null)));
    when(userNotificationStateService.reserveUnreadChatNotificationIfEligible(16L)).thenReturn(false);

    service.registerChatMessageCreatedAfterCommit(13L);
    service.registerChatMessageCreatedAfterCommit(14L);
    service.registerChatMessageCreatedAfterCommit(15L);
    service.registerChatMessageCreatedAfterCommit(16L);

    UserEntity pendingRecruiter = user(8L, UserRole.RECRUITER, "Scout", null, "recruiter@example.com");
    pendingRecruiter.setStatus(UserStatus.PENDING);
    service.registerAccountApprovedAfterCommit(null, UserStatus.PENDING);
    service.registerAccountApprovedAfterCommit(pendingRecruiter, UserStatus.PENDING);

    UserEntity noIdApproved = user(null, UserRole.RECRUITER, "Scout", null, "recruiter@example.com");
    service.registerAccountApprovedAfterCommit(noIdApproved, UserStatus.PENDING);

    verify(notificationEmailService, never())
        .sendUnreadChatNotification(anyString(), anyString(), anyInt());
    verify(notificationEmailService, never())
        .sendAccountApprovedNotification(anyString(), anyString(), anyString());
  }

  @Test
  void adminLoginAlertsRequireConfiguredRecipientAndAdminRole() {
    NotificationOrchestratorService blankRecipientService =
        new NotificationOrchestratorService(
            userRepository,
            jdbcTemplate,
            notificationEmailService,
            userNotificationStateService,
            24,
            10,
            "   ");

    when(notificationEmailService.isEmailEnabled()).thenReturn(true);

    blankRecipientService.markUserLoggedIn(7L);

    verify(userNotificationStateService).markLoggedIn(7L);

    NotificationOrchestratorService service =
        new NotificationOrchestratorService(
            userRepository,
            jdbcTemplate,
            notificationEmailService,
            userNotificationStateService,
            24,
            10,
            "security@certifiedcarry.me");

    UserEntity recruiter = user(8L, UserRole.RECRUITER, "Scout", null, "recruiter@example.com");
    when(userRepository.findById(8L)).thenReturn(Optional.of(recruiter));

    service.markUserLoggedIn(8L);

    verify(userNotificationStateService).markLoggedIn(8L);
    verify(notificationEmailService, never())
        .sendAdminLoginAlert(
            anyString(), anyString(), anyString(), anyLong(), any(OffsetDateTime.class));
  }

  @Test
  void rankReminderProcessingSkipsMissingAddressesAndDoesNotMarkFailedSends() throws SQLException {
    NotificationOrchestratorService service =
        new NotificationOrchestratorService(
            userRepository,
            jdbcTemplate,
            notificationEmailService,
            userNotificationStateService,
            24,
            10,
            "");

    when(notificationEmailService.isEmailEnabled()).thenReturn(true);
    when(notificationEmailService.sendRankExpiryReminder(
            "recruiter@example.com",
            "Scout",
            "Rocket League",
            "GC",
            OffsetDateTime.parse("2026-05-14T03:00:00Z")))
        .thenReturn(false);

    doAnswer(
            invocation -> {
              String sql = invocation.getArgument(0);
              RowMapper<?> mapper = invocation.getArgument(1);
              if (sql.contains("rank_expiry_reminder_sent_at IS NULL")) {
                return List.of(
                    mapper.mapRow(mockReminderResultSetWithoutEmail(), 0),
                    mapper.mapRow(mockRecruiterReminderResultSet(), 1));
              }
              return List.of();
            })
        .when(jdbcTemplate)
        .query(anyString(), any(RowMapper.class), anyInt(), anyInt());

    service.processRankValidationExpiries();

    verify(notificationEmailService)
        .sendRankExpiryReminder(
            "recruiter@example.com",
            "Scout",
            "Rocket League",
            "GC",
            OffsetDateTime.parse("2026-05-14T03:00:00Z"));
    verify(jdbcTemplate, never()).update(anyString(), eq(93L));
    verify(jdbcTemplate, never()).update(anyString(), eq(94L));
  }

  @Test
  void chatNotificationSuccessUsesPositiveUnreadCountWithoutClearingFlag() {
    NotificationOrchestratorService service =
        new NotificationOrchestratorService(
            userRepository,
            jdbcTemplate,
            notificationEmailService,
            userNotificationStateService,
            24,
            10,
            "");

    UserEntity player = user(17L, UserRole.PLAYER, "Bluy", "player@example.com", null);
    when(notificationEmailService.isEmailEnabled()).thenReturn(true);
    when(userRepository.findById(17L)).thenReturn(Optional.of(player));
    when(userNotificationStateService.reserveUnreadChatNotificationIfEligible(17L)).thenReturn(true);
    when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(17L))).thenReturn(4);
    when(notificationEmailService.sendUnreadChatNotification("player@example.com", "Bluy", 4))
        .thenReturn(true);

    service.registerChatMessageCreatedAfterCommit(17L);

    verify(notificationEmailService).sendUnreadChatNotification("player@example.com", "Bluy", 4);
    verify(userNotificationStateService, never()).clearUnreadChatNotificationFlag(17L);
  }

  @Test
  void accountApprovalUsesAccountLabelForAdminUsers() {
    NotificationOrchestratorService service =
        new NotificationOrchestratorService(
            userRepository,
            jdbcTemplate,
            notificationEmailService,
            userNotificationStateService,
            24,
            10,
            "");

    UserEntity admin = user(19L, UserRole.ADMIN, "Admin", null, "admin@example.com");
    when(userRepository.findById(19L)).thenReturn(Optional.of(admin));

    service.registerAccountApprovedAfterCommit(admin, UserStatus.PENDING);

    verify(notificationEmailService)
        .sendAccountApprovedNotification("admin@example.com", "Admin", "account");
  }

  private UserEntity user(Long id, UserRole role, String fullName, String personalEmail, String workEmail) {
    UserEntity user = new UserEntity();
    user.setId(id);
    user.setRole(role);
    user.setStatus(UserStatus.APPROVED);
    user.setFullName(fullName);
    user.setPersonalEmail(personalEmail);
    user.setEmail(workEmail);
    return user;
  }

  private ResultSet mockReminderResultSet() throws SQLException {
    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
    when(resultSet.getLong("profile_id")).thenReturn(91L);
    when(resultSet.getLong("user_id")).thenReturn(13L);
    when(resultSet.getString("full_name")).thenReturn("Bluy");
    when(resultSet.getString("role")).thenReturn("PLAYER");
    when(resultSet.getString("personal_email")).thenReturn("player@example.com");
    when(resultSet.getString("email")).thenReturn(null);
    when(resultSet.getString("game")).thenReturn("Valorant");
    when(resultSet.getString("rank")).thenReturn("Immortal");
    when(resultSet.getObject("rank_expires_at", OffsetDateTime.class))
        .thenReturn(OffsetDateTime.parse("2026-05-14T00:00:00Z"));
    return resultSet;
  }

  private ResultSet mockExpiredResultSet() throws SQLException {
    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
    when(resultSet.getLong("id")).thenReturn(91L);
    when(resultSet.getLong("user_id")).thenReturn(92L);
    return resultSet;
  }

  private ResultSet mockReminderResultSetWithoutEmail() throws SQLException {
    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
    when(resultSet.getLong("profile_id")).thenReturn(93L);
    when(resultSet.getLong("user_id")).thenReturn(21L);
    when(resultSet.getString("full_name")).thenReturn("No Email");
    when(resultSet.getString("role")).thenReturn("PLAYER");
    when(resultSet.getString("personal_email")).thenReturn("   ");
    when(resultSet.getString("email")).thenReturn(null);
    when(resultSet.getString("game")).thenReturn("Valorant");
    when(resultSet.getString("rank")).thenReturn("Ascendant");
    when(resultSet.getObject("rank_expires_at", OffsetDateTime.class))
        .thenReturn(OffsetDateTime.parse("2026-05-14T02:00:00Z"));
    return resultSet;
  }

  private ResultSet mockRecruiterReminderResultSet() throws SQLException {
    ResultSet resultSet = org.mockito.Mockito.mock(ResultSet.class);
    when(resultSet.getLong("profile_id")).thenReturn(94L);
    when(resultSet.getLong("user_id")).thenReturn(22L);
    when(resultSet.getString("full_name")).thenReturn("Scout");
    when(resultSet.getString("role")).thenReturn("RECRUITER");
    when(resultSet.getString("personal_email")).thenReturn(null);
    when(resultSet.getString("email")).thenReturn("recruiter@example.com");
    when(resultSet.getString("game")).thenReturn("Rocket League");
    when(resultSet.getString("rank")).thenReturn("GC");
    when(resultSet.getObject("rank_expires_at", OffsetDateTime.class))
        .thenReturn(OffsetDateTime.parse("2026-05-14T03:00:00Z"));
    return resultSet;
  }
}
