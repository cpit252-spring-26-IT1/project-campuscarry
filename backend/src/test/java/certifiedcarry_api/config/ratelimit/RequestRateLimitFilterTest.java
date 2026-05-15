package certifiedcarry_api.config.ratelimit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class RequestRateLimitFilterTest {

  private final RequestRateLimitFilter filter = new RequestRateLimitFilter();

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void passesThroughWhenRequestDoesNotMatchKnownRateLimitedPolicy() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/health");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
    assertSame(request, chain.getRequest());
  }

  @Test
  void rateLimitsRequestsPerBackendUserIdBucket() throws Exception {
    for (int attempt = 0; attempt < 30; attempt++) {
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/session/login");
      request.setAttribute("backendUserId", "13");
      MockHttpServletResponse response = new MockHttpServletResponse();

      filter.doFilter(request, response, new MockFilterChain());
      assertEquals(200, response.getStatus());
    }

    MockHttpServletRequest blockedRequest = new MockHttpServletRequest("POST", "/auth/session/login");
    blockedRequest.setAttribute("backendUserId", "13");
    MockHttpServletResponse blockedResponse = new MockHttpServletResponse();

    filter.doFilter(blockedRequest, blockedResponse, new MockFilterChain());

    assertEquals(429, blockedResponse.getStatus());
    assertEquals("60", blockedResponse.getHeader("Retry-After"));
    assertTrue(blockedResponse.getContentAsString().contains("rate_limit_exceeded"));

    MockHttpServletRequest differentUserRequest =
        new MockHttpServletRequest("POST", "/auth/session/login");
    differentUserRequest.setAttribute("backendUserId", "14");
    MockHttpServletResponse differentUserResponse = new MockHttpServletResponse();

    filter.doFilter(differentUserRequest, differentUserResponse, new MockFilterChain());
    assertEquals(200, differentUserResponse.getStatus());
  }

  @Test
  void fallsBackToPrincipalAndForwardedIpWhenBackendUserIdIsMissing() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("player13", "n/a"));

    MockHttpServletRequest principalRequest = new MockHttpServletRequest("POST", "/auth/session/logout");
    MockHttpServletResponse principalResponse = new MockHttpServletResponse();
    MockFilterChain principalChain = new MockFilterChain();

    filter.doFilter(principalRequest, principalResponse, principalChain);

    assertEquals(200, principalResponse.getStatus());
    assertSame(principalRequest, principalChain.getRequest());

    SecurityContextHolder.clearContext();

    MockHttpServletRequest forwardedRequest = new MockHttpServletRequest("POST", "/users");
    forwardedRequest.addHeader("X-Forwarded-For", "198.51.100.12, 203.0.113.9");
    MockHttpServletResponse forwardedResponse = new MockHttpServletResponse();
    MockFilterChain forwardedChain = new MockFilterChain();

    filter.doFilter(forwardedRequest, forwardedResponse, forwardedChain);

    assertEquals(200, forwardedResponse.getStatus());
    assertSame(forwardedRequest, forwardedChain.getRequest());
  }

  @Test
  void usesRemoteAddressFallbackAndSurvivesCleanupSweep() throws Exception {
    for (int attempt = 0; attempt < 500; attempt++) {
      MockHttpServletRequest request = new MockHttpServletRequest("POST", "/auth/session/login");
      request.setAttribute("backendUserId", String.valueOf(1000 + attempt));

      filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
    }

    MockHttpServletRequest remoteAddressRequest = new MockHttpServletRequest("POST", "/users");
    remoteAddressRequest.setRemoteAddr("203.0.113.50");
    MockHttpServletResponse remoteAddressResponse = new MockHttpServletResponse();
    MockFilterChain remoteAddressChain = new MockFilterChain();

    filter.doFilter(remoteAddressRequest, remoteAddressResponse, remoteAddressChain);

    assertEquals(200, remoteAddressResponse.getStatus());
    assertSame(remoteAddressRequest, remoteAddressChain.getRequest());
  }

  @Test
  void ignoresUnsupportedHttpMethodsBeforeRateLimiting() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("BREW", "/auth/session/login");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
    assertSame(request, chain.getRequest());
  }

  @Test
  void fallsBackToUnknownIpWhenNoUserIdentityHeadersOrRemoteAddressExist() throws Exception {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new AnonymousAuthenticationToken(
                "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")));

    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users");
    request.setRemoteAddr("");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain chain = new MockFilterChain();

    filter.doFilter(request, response, chain);

    assertEquals(200, response.getStatus());
    assertSame(request, chain.getRequest());
  }

  @Test
  void resolveHelpersHandleUnsupportedMethodsAndAuthenticatedPrincipals() {
    MockHttpServletRequest unsupportedMethod = new MockHttpServletRequest();
    unsupportedMethod.setRequestURI("/auth/session/login");
    unsupportedMethod.setMethod(null);
    assertNull(ReflectionTestUtils.invokeMethod(filter, "resolveRateLimitedPolicy", unsupportedMethod));

    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                "player13", "n/a", AuthorityUtils.createAuthorityList("ROLE_USER")));
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/users");

    assertEquals(
        "principal:player13",
        ReflectionTestUtils.invokeMethod(filter, "resolveClientIdentity", request));
  }
}
