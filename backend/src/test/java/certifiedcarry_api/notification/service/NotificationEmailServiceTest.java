package certifiedcarry_api.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class NotificationEmailServiceTest {

  @Test
  void emailNotificationsRespectEnabledFlagAndNormalizeOutgoingContent() {
    JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
    NotificationEmailService disabledService =
        new NotificationEmailService(mailSender, false, " no-reply@certifiedcarry.me ", " ", " https://certifiedcarry.me ");

    assertFalse(disabledService.sendUnreadChatNotification("player@example.com", "Blue", 0));
    assertFalse(disabledService.sendUnreadChatNotification("   ", null, 2));

    NotificationEmailService enabledService =
        new NotificationEmailService(
            mailSender,
            true,
            " no-reply@certifiedcarry.me ",
            " support@certifiedcarry.me ",
            " https://certifiedcarry.me ");

    assertTrue(enabledService.sendUnreadChatNotification("player@example.com", "  ", 0));

    ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(mailSender).send(messageCaptor.capture());

    SimpleMailMessage message = messageCaptor.getValue();
    assertEquals("no-reply@certifiedcarry.me", message.getFrom());
    assertEquals("support@certifiedcarry.me", message.getReplyTo());
    assertEquals("player@example.com", message.getTo()[0]);
    assertEquals("You have unread chats on Certified Carry", message.getSubject());
    assertTrue(message.getText().contains("Hello there"));
    assertTrue(message.getText().contains("Current unread messages: 1"));
    assertTrue(message.getText().contains("https://certifiedcarry.me/chats"));
  }

  @Test
  void emailServiceFormatsDifferentTemplatesAndHandlesMailFailures() {
    JavaMailSender mailSender = org.mockito.Mockito.mock(JavaMailSender.class);
    NotificationEmailService service =
        new NotificationEmailService(
            mailSender,
            true,
            "alerts@certifiedcarry.me",
            "support@certifiedcarry.me",
            "https://certifiedcarry.me");

    assertTrue(service.sendAccountApprovedNotification("recruiter@example.com", "Scout", "recruiter"));
    assertTrue(
        service.sendRankExpiryReminder(
            "player@example.com",
            "Bluy",
            "Valorant",
            "Immortal",
            OffsetDateTime.parse("2026-05-14T12:30:00+03:00")));
    assertTrue(
        service.sendAdminLoginAlert(
            "security@example.com",
            null,
            "admin@example.com",
            7L,
            OffsetDateTime.parse("2026-05-13T09:00:00+03:00")));

    doThrow(new MailSendException("boom")).when(mailSender).send(any(SimpleMailMessage.class));
    assertFalse(service.sendAdminLoginAlert("security@example.com", "Admin", "admin@example.com", 9L, null));
  }
}
