package certifiedcarry_api.notification.service;

import certifiedcarry_api.shared.TextNormalization;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationEmailService {

  private static final Logger LOGGER = LoggerFactory.getLogger(NotificationEmailService.class);
  private static final DateTimeFormatter UTC_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'", Locale.ROOT);

  private final JavaMailSender mailSender;
  private final boolean emailEnabled;
  private final String fromAddress;
  private final String replyToAddress;
  private final String appBaseUrl;

  public NotificationEmailService(
      JavaMailSender mailSender,
      @Value("${notifications.email.enabled:false}") boolean emailEnabled,
      @Value("${notifications.email.from:no-reply@certifiedcarry.me}") String fromAddress,
      @Value("${notifications.email.reply-to:support@certifiedcarry.me}") String replyToAddress,
      @Value("${notifications.email.app-base-url:https://certifiedcarry.me}") String appBaseUrl) {
    this.mailSender = mailSender;
    this.emailEnabled = emailEnabled;
    this.fromAddress = normalizeRequired(fromAddress, "no-reply@certifiedcarry.me");
    this.replyToAddress = normalizeOptional(replyToAddress);
    this.appBaseUrl = normalizeRequired(appBaseUrl, "https://certifiedcarry.me");
  }

  public boolean isEmailEnabled() {
    return emailEnabled;
  }

  public boolean sendUnreadChatNotification(String toEmail, String recipientName, int unreadCount) {
    String safeName = fallbackName(recipientName);
    int safeUnreadCount = Math.max(1, unreadCount);

    String subject = "You have unread chats on Certified Carry";
    String body =
        "Hello "
            + safeName
            + ",\n\n"
            + "You have unread chats waiting in your inbox.\n"
            + "Current unread messages: "
            + safeUnreadCount
            + "\n\n"
            + "Open your chats: "
            + appBaseUrl
            + "/chats\n\n"
            + "Certified Carry notifications";

    return sendPlainTextEmail(toEmail, subject, body);
  }

  public boolean sendAccountApprovedNotification(
      String toEmail, String recipientName, String userRoleLabel) {
    String safeName = fallbackName(recipientName);
    String safeRole = normalizeRequired(userRoleLabel, "account");

    String subject = "Your Certified Carry account has been approved";
    String body =
        "Hello "
            + safeName
            + ",\n\n"
            + "Good news: your "
            + safeRole
            + " account is now approved.\n"
            + "You can sign in and continue using Certified Carry.\n\n"
            + "Sign in: "
            + appBaseUrl
            + "/login\n\n"
            + "Certified Carry notifications";

    return sendPlainTextEmail(toEmail, subject, body);
  }

  public boolean sendRankExpiryReminder(
      String toEmail,
      String recipientName,
      String game,
      String rank,
      OffsetDateTime expiresAt) {
    String safeName = fallbackName(recipientName);
    String safeGame = normalizeRequired(game, "your selected game");
    String safeRank = normalizeRequired(rank, "your selected rank");
    String safeExpiry = formatUtc(expiresAt);

    String subject = "Rank validation expires in about 1 day";
    String body =
        "Hello "
            + safeName
            + ",\n\n"
            + "Your validated rank is scheduled to expire soon.\n"
            + "Game: "
            + safeGame
            + "\n"
            + "Rank: "
            + safeRank
            + "\n"
            + "Expiry time: "
            + safeExpiry
            + "\n\n"
            + "If it expires, you will be removed from the leaderboard until you submit and re-validate your rank.\n\n"
            + "Update your profile: "
            + appBaseUrl
            + "/profile-setup\n\n"
            + "Certified Carry notifications";

    return sendPlainTextEmail(toEmail, subject, body);
  }

  public boolean sendAdminLoginAlert(
      String toEmail,
      String adminName,
      String adminEmail,
      long adminUserId,
      OffsetDateTime loggedInAt) {
    String safeName = fallbackName(adminName);
    String safeEmail = normalizeRequired(adminEmail, "unknown");
    String safeTimestamp = formatUtc(loggedInAt);

    String subject = "Security alert: admin login detected";
    String body =
        "Admin login event detected.\n\n"
            + "Time: "
            + safeTimestamp
            + "\n"
            + "Admin name: "
            + safeName
            + "\n"
            + "Admin email: "
            + safeEmail
            + "\n"
            + "Admin user id: "
            + adminUserId
            + "\n\n"
            + "If this login was not expected, rotate credentials immediately and review audit events.\n"
            + "Admin panel: "
            + appBaseUrl
            + "/admin\n\n"
            + "Certified Carry security notifications";

    return sendPlainTextEmail(toEmail, subject, body);
  }

  private boolean sendPlainTextEmail(String toEmail, String subject, String body) {
    String normalizedTo = normalizeOptional(toEmail);
    if (normalizedTo == null) {
      return false;
    }

    if (!emailEnabled) {
      LOGGER.debug("Skipped email to {} because notifications.email.enabled=false", normalizedTo);
      return false;
    }

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(normalizedTo);
    message.setSubject(subject);
    message.setText(body);

    if (replyToAddress != null) {
      message.setReplyTo(replyToAddress);
    }

    try {
      mailSender.send(message);
      return true;
    } catch (MailException exception) {
      LOGGER.error("Failed sending notification email to {}", normalizedTo, exception);
      return false;
    }
  }

  private String fallbackName(String value) {
    String normalized = normalizeOptional(value);
    return normalized == null ? "there" : normalized;
  }

  private String formatUtc(OffsetDateTime value) {
    if (value == null) {
      return "unknown";
    }

    return value.withOffsetSameInstant(ZoneOffset.UTC).format(UTC_FORMATTER);
  }

  private String normalizeRequired(String value, String fallback) {
    String normalized = normalizeOptional(value);
    return normalized == null ? fallback : normalized;
  }

  private String normalizeOptional(String value) {
    return TextNormalization.trimToNull(value);
  }
}
