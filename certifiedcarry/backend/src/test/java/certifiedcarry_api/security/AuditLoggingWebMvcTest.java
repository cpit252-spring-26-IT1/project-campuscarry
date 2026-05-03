package certifiedcarry_api.security;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import certifiedcarry_api.audit.service.AuditEventRecord;
import certifiedcarry_api.audit.service.AuditEventService;
import certifiedcarry_api.config.SecurityConfig;
import certifiedcarry_api.config.audit.AuditLoggingFilter;
import certifiedcarry_api.support.UserResponseTestBuilder;
import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.api.UserController;
import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserController.class)
@Import({SecurityConfig.class, AuditLoggingFilter.class})
class AuditLoggingWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserService userService;

  @MockitoBean private AuditEventService auditEventService;

  @Test
  void deniedAdminRouteIsCapturedAsAuditEvent() throws Exception {
    mockMvc.perform(delete("/users/{userId}", "44")).andExpect(status().isForbidden());

    ArgumentCaptor<AuditEventRecord> eventCaptor = ArgumentCaptor.forClass(AuditEventRecord.class);
    verify(auditEventService).recordEvent(eventCaptor.capture());

    AuditEventRecord event = eventCaptor.getValue();
    assertThat(event.action()).isEqualTo("USER_DELETE");
    assertThat(event.outcome()).isEqualTo("DENIED");
    assertThat(event.httpMethod()).isEqualTo("DELETE");
    assertThat(event.endpoint()).isEqualTo("/users/44");
    assertThat(event.targetType()).isEqualTo("user");
    assertThat(event.targetId()).isEqualTo("44");
    assertThat(event.statusCode()).isEqualTo(403);
    assertThat(event.actorBackendUserId()).isNull();
    assertThat(event.requestId()).isNotBlank();
  }

  @Test
  void successfulRegistrationIsCapturedAsAllowedAuditEvent() throws Exception {
    when(userService.createUser(any(CreateUserRequest.class), any(), any(), any()))
        .thenReturn(buildCreatedUser());

    String requestBody =
        objectMapper.writeValueAsString(
            Map.ofEntries(
                entry("fullName", "Audit User"),
                entry("username", "audit_user"),
                entry("personalEmail", "audit-user@example.local"),
                entry("email", "audit-user@example.local"),
                entry("password", "password-123"),
                entry("role", "PLAYER"),
                entry("legalConsentAccepted", true),
                entry("legalConsentAcceptedAt", "2026-04-04T00:00:00Z"),
                entry("legalConsentLocale", "en"),
                entry("legalConsentSource", "REGISTER_PAGE"),
                entry("termsVersionAccepted", "cc-terms-2026-04-04"),
                entry("privacyVersionAccepted", "cc-privacy-2026-04-04")));

    mockMvc
        .perform(
            post("/users")
                .header("X-Forwarded-For", "198.51.100.20")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isCreated());

    ArgumentCaptor<AuditEventRecord> eventCaptor = ArgumentCaptor.forClass(AuditEventRecord.class);
    verify(auditEventService).recordEvent(eventCaptor.capture());

    AuditEventRecord event = eventCaptor.getValue();
    assertThat(event.action()).isEqualTo("USER_REGISTER");
    assertThat(event.outcome()).isEqualTo("ALLOWED");
    assertThat(event.httpMethod()).isEqualTo("POST");
    assertThat(event.endpoint()).isEqualTo("/users");
    assertThat(event.targetType()).isEqualTo("user");
    assertThat(event.targetId()).isNull();
    assertThat(event.statusCode()).isEqualTo(201);
    assertThat(event.clientIp()).isEqualTo("198.51.100.20");
    assertThat(event.requestId()).isNotBlank();
  }

  private UserResponse buildCreatedUser() {
    return UserResponseTestBuilder.aPlayer()
        .withId("777")
        .withFullName("Audit User")
        .withUsername("audit_user")
        .withPersonalEmail("audit-user@example.local")
        .withEmail("audit-user@example.local")
        .build();
  }
}
