package certifiedcarry_api.notification.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class UserNotificationStateServiceTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Test
  void loginLogoutClearAndRecordActivityEnsureStateRowBeforeUpdates() {
    UserNotificationStateService service = new UserNotificationStateService(jdbcTemplate);

    service.markLoggedIn(9L);
    service.markLoggedOut(9L);
    service.clearUnreadChatNotificationFlag(9L);
    service.recordActivity(9L);

    verify(jdbcTemplate, times(4)).update(eq("""
        INSERT INTO user_notification_states (user_id)
        VALUES (?)
        ON CONFLICT (user_id) DO NOTHING
        """), eq(9L));
    verify(jdbcTemplate).update(eq("""
        UPDATE user_notification_states
        SET
          is_logged_in = TRUE,
          chat_unread_notified_since_last_login = FALSE,
          last_login_at = NOW(),
          last_activity_at = NOW(),
          updated_at = NOW()
        WHERE user_id = ?
        """), eq(9L));
    verify(jdbcTemplate).update(eq("""
        UPDATE user_notification_states
        SET
          is_logged_in = FALSE,
          last_logout_at = NOW(),
          last_activity_at = NOW(),
          updated_at = NOW()
        WHERE user_id = ?
        """), eq(9L));
    verify(jdbcTemplate).update(eq("""
        UPDATE user_notification_states
        SET
          chat_unread_notified_since_last_login = FALSE,
          last_chat_unread_notified_at = NULL,
          updated_at = NOW()
        WHERE user_id = ?
        """), eq(9L));
    verify(jdbcTemplate).update(eq("""
        UPDATE user_notification_states
        SET
          last_activity_at = NOW(),
          updated_at = NOW()
        WHERE user_id = ?
        """), eq(9L));
  }

  @Test
  void reserveUnreadChatNotificationReturnsTrueWhenRowIsUpdated() {
    UserNotificationStateService service = new UserNotificationStateService(jdbcTemplate);
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(14L))).thenReturn(List.of(14L));

    boolean reserved = service.reserveUnreadChatNotificationIfEligible(14L);

    assertTrue(reserved);
    verify(jdbcTemplate).update(eq("""
        INSERT INTO user_notification_states (user_id)
        VALUES (?)
        ON CONFLICT (user_id) DO NOTHING
        """), eq(14L));
  }

  @Test
  void reserveUnreadChatNotificationReturnsFalseWhenUserIsNotEligible() {
    UserNotificationStateService service = new UserNotificationStateService(jdbcTemplate);
    when(jdbcTemplate.query(any(String.class), any(RowMapper.class), eq(14L))).thenReturn(List.of());

    boolean reserved = service.reserveUnreadChatNotificationIfEligible(14L);

    assertFalse(reserved);
  }
}
