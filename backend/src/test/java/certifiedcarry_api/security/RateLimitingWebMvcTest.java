package certifiedcarry_api.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static java.util.Map.entry;

import certifiedcarry_api.chat.api.ChatMessageController;
import certifiedcarry_api.chat.service.ChatService;
import certifiedcarry_api.config.SecurityConfig;
import certifiedcarry_api.config.ratelimit.RequestRateLimitFilter;
import certifiedcarry_api.auth.api.AuthController;
import certifiedcarry_api.notification.service.NotificationOrchestratorService;
import certifiedcarry_api.support.UserResponseTestBuilder;
import certifiedcarry_api.user.api.CreateUserRequest;
import certifiedcarry_api.user.api.UserController;
import certifiedcarry_api.user.api.UserResponse;
import certifiedcarry_api.user.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {UserController.class, ChatMessageController.class, AuthController.class})
@Import({SecurityConfig.class, RequestRateLimitFilter.class})
class RateLimitingWebMvcTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private UserService userService;

  @MockitoBean private ChatService chatService;

  @MockitoBean private NotificationOrchestratorService notificationOrchestratorService;

  @Test
  void registrationIsRateLimitedPerIpAddress() throws Exception {
    when(userService.createUser(any(CreateUserRequest.class), any(), any(), any()))
        .thenReturn(buildCreatedUser());

    String requestBody =
        objectMapper.writeValueAsString(
            Map.ofEntries(
                entry("fullName", "Load Test User"),
                entry("username", "load_user"),
                entry("personalEmail", "load-user@example.local"),
                entry("email", "load-user@example.local"),
                entry("password", "password-123"),
                entry("role", "PLAYER"),
                entry("legalConsentAccepted", true),
                entry("legalConsentAcceptedAt", "2026-04-04T00:00:00Z"),
                entry("legalConsentLocale", "en"),
                entry("legalConsentSource", "REGISTER_PAGE"),
                entry("termsVersionAccepted", "cc-terms-2026-04-04"),
                entry("privacyVersionAccepted", "cc-privacy-2026-04-04")));

    for (int attempt = 0; attempt < 15; attempt++) {
      mockMvc
          .perform(
              post("/users")
                  .header("X-Forwarded-For", "198.51.100.10")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(requestBody))
          .andExpect(status().isCreated());
    }

    mockMvc
        .perform(
            post("/users")
                .header("X-Forwarded-For", "198.51.100.10")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "60"))
        .andExpect(jsonPath("$.code").value("rate_limit_exceeded"));
  }

  @Test
  void chatMessageLimitUsesBackendUserIdentityBuckets() throws Exception {
    when(chatService.createChatMessageForActor(anyMap(), anyLong(), anyBoolean()))
        .thenReturn(Map.of("id", "1"));

    String user13RequestBody =
        objectMapper.writeValueAsString(
            Map.of("threadId", "7", "senderId", "13", "recipientId", "21", "body", "hello"));

    for (int attempt = 0; attempt < 30; attempt++) {
      mockMvc
          .perform(
              post("/chat_messages")
                  .with(user("player13").roles("FIREBASE_AUTHENTICATED"))
                  .requestAttr("backendUserId", "13")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(user13RequestBody))
          .andExpect(status().isCreated());
    }

    mockMvc
        .perform(
            post("/chat_messages")
                .with(user("player13").roles("FIREBASE_AUTHENTICATED"))
                .requestAttr("backendUserId", "13")
                .contentType(MediaType.APPLICATION_JSON)
                .content(user13RequestBody))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "60"))
        .andExpect(jsonPath("$.code").value("rate_limit_exceeded"));

    String user14RequestBody =
        objectMapper.writeValueAsString(
            Map.of("threadId", "7", "senderId", "14", "recipientId", "21", "body", "hello"));

    mockMvc
        .perform(
            post("/chat_messages")
                .with(user("player14").roles("FIREBASE_AUTHENTICATED"))
                .requestAttr("backendUserId", "14")
                .contentType(MediaType.APPLICATION_JSON)
                .content(user14RequestBody))
        .andExpect(status().isCreated());
  }

  @Test
  void sessionLoginEndpointIsRateLimitedPerBackendUser() throws Exception {
    for (int attempt = 0; attempt < 30; attempt++) {
      mockMvc
          .perform(
              post("/auth/session/login")
                  .with(user("player13").roles("FIREBASE_AUTHENTICATED"))
                  .requestAttr("backendUserId", "13"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.state").value("LOGGED_IN"));
    }

    mockMvc
        .perform(
            post("/auth/session/login")
                .with(user("player13").roles("FIREBASE_AUTHENTICATED"))
                .requestAttr("backendUserId", "13"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "60"))
        .andExpect(jsonPath("$.code").value("rate_limit_exceeded"));

    mockMvc
        .perform(
            post("/auth/session/login")
                .with(user("player14").roles("FIREBASE_AUTHENTICATED"))
                .requestAttr("backendUserId", "14"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("LOGGED_IN"));

    verify(notificationOrchestratorService, times(30)).markUserLoggedIn(13L);
    verify(notificationOrchestratorService, times(1)).markUserLoggedIn(14L);
  }

  @Test
  void sessionLogoutEndpointIsRateLimitedPerBackendUser() throws Exception {
    for (int attempt = 0; attempt < 60; attempt++) {
      mockMvc
          .perform(
              post("/auth/session/logout")
                  .with(user("player13").roles("FIREBASE_AUTHENTICATED"))
                  .requestAttr("backendUserId", "13"))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.state").value("LOGGED_OUT"));
    }

    mockMvc
        .perform(
            post("/auth/session/logout")
                .with(user("player13").roles("FIREBASE_AUTHENTICATED"))
                .requestAttr("backendUserId", "13"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "60"))
        .andExpect(jsonPath("$.code").value("rate_limit_exceeded"));

    mockMvc
        .perform(
            post("/auth/session/logout")
                .with(user("player14").roles("FIREBASE_AUTHENTICATED"))
                .requestAttr("backendUserId", "14"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.state").value("LOGGED_OUT"));

    verify(notificationOrchestratorService, times(60)).markUserLoggedOut(13L);
    verify(notificationOrchestratorService, times(1)).markUserLoggedOut(14L);
  }

  private UserResponse buildCreatedUser() {
        return UserResponseTestBuilder.aPlayer()
                .withId("501")
                .withFullName("Load Test User")
                .withUsername("load_user")
                .withPersonalEmail("load-user@example.local")
                .withEmail("load-user@example.local")
                .build();
  }
}
