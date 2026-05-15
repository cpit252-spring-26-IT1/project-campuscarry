package certifiedcarry_api.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import certifiedcarry_api.config.ApiRouteCatalog.EndpointPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiRouteCatalogTest {

  @Test
  void helperMethodsRecognizeExpectedPathsAndPrefixes() {
    assertEquals("/api/users", ApiRouteCatalog.apiPath(ApiRouteCatalog.USERS));
    assertEquals("/chat_messages/**", ApiRouteCatalog.recursive(ApiRouteCatalog.CHAT_MESSAGES));
    assertTrue(ApiRouteCatalog.isUserRegistrationPath("/api/users/"));
    assertTrue(ApiRouteCatalog.isSignupCompletionPath("/v1/auth/signup/complete"));
    assertFalse(ApiRouteCatalog.isUserRegistrationPath("   "));
  }

  @Test
  void resolveRequestPathStripsContextPathWhenPresent() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/cc/api/chat_messages/9");
    request.setContextPath("/cc");

    assertEquals("/api/chat_messages/9", ApiRouteCatalog.resolveRequestPath(request));

    MockHttpServletRequest noContext = new MockHttpServletRequest("GET", "/auth/me");
    assertEquals("/auth/me", ApiRouteCatalog.resolveRequestPath(noContext));

    MockHttpServletRequest blankPath = new MockHttpServletRequest();
    blankPath.setRequestURI("");
    assertEquals("/", ApiRouteCatalog.resolveRequestPath(blankPath));
  }

  @Test
  void resolvesPoliciesAndExtractsTargetIdsFromMatchedRoutes() {
    EndpointPolicy rateLimited =
        ApiRouteCatalog.resolveRateLimitedPolicy(HttpMethod.POST, "/chat_messages/77");
    assertNotNull(rateLimited);
    assertEquals(HttpMethod.POST, rateLimited.method());
    assertEquals(30, rateLimited.rateLimitPolicy().maxRequests());
    assertTrue(rateLimited.matches(HttpMethod.POST, "/chat_messages/77/replies"));
    assertFalse(rateLimited.matches(HttpMethod.PATCH, "/chat_messages/77"));
    assertEquals("77", rateLimited.extractTargetId("/chat_messages/77/replies"));
    assertNull(rateLimited.extractTargetId("/chat_messages"));
    assertNull(rateLimited.extractTargetId("/chat_messages/"));

    EndpointPolicy audited =
        ApiRouteCatalog.resolveAuditedPolicy(HttpMethod.PATCH, "/leaderboard/44");
    assertNotNull(audited);
    assertEquals("LEADERBOARD_UPDATE", audited.auditPolicy().action());
    assertEquals("leaderboard_entry", audited.auditPolicy().targetType());

    assertNull(ApiRouteCatalog.resolveRateLimitedPolicy(HttpMethod.GET, "/not-mapped"));
    assertNull(ApiRouteCatalog.resolveAuditedPolicy(HttpMethod.GET, "/not-mapped"));
  }
}
