package certifiedcarry_api.user.service;

import certifiedcarry_api.shared.TextNormalization;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class FirebaseAccountDeletionQueueService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FirebaseAccountDeletionQueueService.class);

  private final JdbcTemplate jdbcTemplate;

  @Value("${account-deletion.firebase-retry-batch-size:25}")
  private int retryBatchSize;

  @Value("${account-deletion.firebase-retry-base-delay-seconds:30}")
  private int retryBaseDelaySeconds;

  @Value("${account-deletion.firebase-retry-max-delay-seconds:3600}")
  private int retryMaxDelaySeconds;

  public FirebaseAccountDeletionQueueService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public void deleteOrQueue(Long backendUserId, String firebaseUid, String email) {
    String normalizedFirebaseUid = normalizeOptionalText(firebaseUid);
    String normalizedEmail = normalizeOptionalText(email);

    if (normalizedFirebaseUid == null && normalizedEmail == null) {
      return;
    }

    try {
      deleteFirebaseAccount(normalizedFirebaseUid, normalizedEmail);
    } catch (FirebaseAuthException exception) {
      if (exception.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
        return;
      }

      queueDeletion(backendUserId, normalizedFirebaseUid, normalizedEmail, describeException(exception));
    } catch (RuntimeException exception) {
      queueDeletion(backendUserId, normalizedFirebaseUid, normalizedEmail, describeException(exception));
    }
  }

  @Scheduled(fixedDelayString = "${account-deletion.firebase-retry-delay-ms:60000}")
  public void retryQueuedDeletions() {
    if (FirebaseApp.getApps().isEmpty()) {
      return;
    }

    List<QueueItem> items =
        jdbcTemplate.query(
            """
            SELECT id, backend_user_id, firebase_uid, email, attempts
            FROM firebase_account_deletion_queue
            WHERE next_attempt_at <= NOW()
            ORDER BY id ASC
            LIMIT ?
            """,
            (rs, rowNum) ->
                new QueueItem(
                    rs.getLong("id"),
                    rs.getObject("backend_user_id", Long.class),
                    rs.getString("firebase_uid"),
                    rs.getString("email"),
                    rs.getInt("attempts")),
            retryBatchSize);

    for (QueueItem item : items) {
      try {
        deleteFirebaseAccount(item.firebaseUid(), item.email());
        jdbcTemplate.update("DELETE FROM firebase_account_deletion_queue WHERE id = ?", item.id());
      } catch (FirebaseAuthException exception) {
        if (exception.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
          jdbcTemplate.update("DELETE FROM firebase_account_deletion_queue WHERE id = ?", item.id());
          continue;
        }

        scheduleRetry(item, describeException(exception));
      } catch (RuntimeException exception) {
        scheduleRetry(item, describeException(exception));
      }
    }
  }

  private void queueDeletion(Long backendUserId, String firebaseUid, String email, String error) {
    int nextAttempts = 1;
    long delaySeconds = computeRetryDelaySeconds(nextAttempts);

    jdbcTemplate.update(
        """
        INSERT INTO firebase_account_deletion_queue
          (backend_user_id, firebase_uid, email, attempts, last_error, next_attempt_at)
        VALUES (?, ?, ?, ?, ?, NOW() + (? * INTERVAL '1 second'))
        """,
        backendUserId,
        firebaseUid,
        email,
        nextAttempts,
        truncate(error, 2000),
        delaySeconds);

    LOGGER.warn(
        "Queued Firebase account deletion for backendUserId={} (uid={}, email={}) due to: {}",
        backendUserId,
        firebaseUid,
        email,
        error);
  }

  private void scheduleRetry(QueueItem item, String error) {
    int nextAttempts = item.attempts() + 1;
    long delaySeconds = computeRetryDelaySeconds(nextAttempts);

    jdbcTemplate.update(
        """
        UPDATE firebase_account_deletion_queue
        SET attempts = ?,
            last_error = ?,
            next_attempt_at = NOW() + (? * INTERVAL '1 second'),
            updated_at = NOW()
        WHERE id = ?
        """,
        nextAttempts,
        truncate(error, 2000),
        delaySeconds,
        item.id());

    LOGGER.warn(
        "Retrying queued Firebase account deletion id={} in {}s (attempt={}): {}",
        item.id(),
        delaySeconds,
        nextAttempts,
        error);
  }

  private void deleteFirebaseAccount(String firebaseUid, String email) throws FirebaseAuthException {
    if (FirebaseApp.getApps().isEmpty()) {
      throw new IllegalStateException("Firebase Admin SDK is not initialized.");
    }

    if (firebaseUid != null) {
      try {
        FirebaseAuth.getInstance().deleteUser(firebaseUid);
        return;
      } catch (FirebaseAuthException exception) {
        if (exception.getAuthErrorCode() != AuthErrorCode.USER_NOT_FOUND || email == null) {
          throw exception;
        }
      }
    }

    if (email == null) {
      return;
    }

    var userRecord = FirebaseAuth.getInstance().getUserByEmail(email);
    FirebaseAuth.getInstance().deleteUser(userRecord.getUid());
  }

  private String normalizeOptionalText(String value) {
    return TextNormalization.trimToNull(value);
  }

  private long computeRetryDelaySeconds(int attempts) {
    long exponent = Math.max(0, Math.min(6, attempts - 1));
    long exponentialDelay = (long) retryBaseDelaySeconds * (1L << exponent);
    return Math.min(Math.max(1, retryMaxDelaySeconds), Math.max(1, exponentialDelay));
  }

  private String describeException(Exception exception) {
    String message = exception.getMessage();
    if (message == null || message.isBlank()) {
      return exception.getClass().getSimpleName();
    }

    return message;
  }

  private String truncate(String value, int maxLength) {
    if (value == null || value.length() <= maxLength) {
      return value;
    }

    return value.substring(0, maxLength);
  }

  private record QueueItem(long id, Long backendUserId, String firebaseUid, String email, int attempts) {}
}
