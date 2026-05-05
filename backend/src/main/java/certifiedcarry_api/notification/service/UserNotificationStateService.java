package certifiedcarry_api.notification.service;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserNotificationStateService {

  private final JdbcTemplate jdbcTemplate;

  public UserNotificationStateService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public void markLoggedIn(long userId) {
    ensureStateRow(userId);
    jdbcTemplate.update(
        """
        UPDATE user_notification_states
        SET
          is_logged_in = TRUE,
          chat_unread_notified_since_last_login = FALSE,
          last_login_at = NOW(),
          last_activity_at = NOW(),
          updated_at = NOW()
        WHERE user_id = ?
        """,
        userId);
  }

  @Transactional
  public void markLoggedOut(long userId) {
    ensureStateRow(userId);
    jdbcTemplate.update(
        """
        UPDATE user_notification_states
        SET
          is_logged_in = FALSE,
          last_logout_at = NOW(),
          last_activity_at = NOW(),
          updated_at = NOW()
        WHERE user_id = ?
        """,
        userId);
  }

  @Transactional
  public boolean reserveUnreadChatNotificationIfEligible(long userId) {
    ensureStateRow(userId);

    List<Long> rows =
        jdbcTemplate.query(
            """
            UPDATE user_notification_states
            SET
              chat_unread_notified_since_last_login = TRUE,
              last_chat_unread_notified_at = NOW(),
              last_activity_at = NOW(),
              updated_at = NOW()
            WHERE user_id = ?
              AND is_logged_in = FALSE
              AND chat_unread_notified_since_last_login = FALSE
            RETURNING user_id
            """,
            (resultSet, rowNumber) -> resultSet.getLong("user_id"),
            userId);

    return !rows.isEmpty();
  }

  @Transactional
  public void clearUnreadChatNotificationFlag(long userId) {
    ensureStateRow(userId);
    jdbcTemplate.update(
        """
        UPDATE user_notification_states
        SET
          chat_unread_notified_since_last_login = FALSE,
          last_chat_unread_notified_at = NULL,
          updated_at = NOW()
        WHERE user_id = ?
        """,
        userId);
  }

  @Transactional
  public void recordActivity(long userId) {
    ensureStateRow(userId);
    jdbcTemplate.update(
        """
        UPDATE user_notification_states
        SET
          last_activity_at = NOW(),
          updated_at = NOW()
        WHERE user_id = ?
        """,
        userId);
  }

  private void ensureStateRow(long userId) {
    jdbcTemplate.update(
        """
        INSERT INTO user_notification_states (user_id)
        VALUES (?)
        ON CONFLICT (user_id) DO NOTHING
        """,
        userId);
  }
}
