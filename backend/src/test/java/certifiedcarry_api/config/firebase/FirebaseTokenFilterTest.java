package certifiedcarry_api.config.firebase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import certifiedcarry_api.user.model.UserEntity;
import certifiedcarry_api.user.model.UserRole;
import certifiedcarry_api.user.repo.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class FirebaseTokenFilterTest {

  @AfterEach
  void clearSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void requestsWithoutBearerHeadersPassThroughUnchanged() throws ServletException, IOException {
    UserRepository userRepository = mock(UserRepository.class);
    FirebaseTokenFilter filter = new FirebaseTokenFilter(userRepository);

    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    filter.doFilter(request, response, filterChain);

    assertEquals(200, response.getStatus());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertNull(request.getAttribute("firebaseUid"));
    assertEquals(request, filterChain.getRequest());
  }

  @Test
  void blankBearerTokensReturnUnauthorized() throws ServletException, IOException {
    UserRepository userRepository = mock(UserRepository.class);
    FirebaseTokenFilter filter = new FirebaseTokenFilter(userRepository);

    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer   ");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, new MockFilterChain());

    assertEquals(401, response.getStatus());
    assertTrue(response.getContentAsString().contains("Missing Firebase ID token."));
  }

  @Test
  void validTokensPopulateSecurityContextAndRequestAttributes() throws Exception {
    UserRepository userRepository = mock(UserRepository.class);
    FirebaseTokenFilter filter = new FirebaseTokenFilter(userRepository);
    FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
    FirebaseToken firebaseToken = mock(FirebaseToken.class);
    UserEntity adminUser = new UserEntity();
    adminUser.setId(7L);
    adminUser.setRole(UserRole.ADMIN);

    when(firebaseToken.getUid()).thenReturn(" firebase-uid ");
    when(firebaseToken.getClaims())
        .thenReturn(Map.of("email", "admin@example.com", "email_verified", "true"));
    when(firebaseAuth.verifyIdToken("abc")).thenReturn(firebaseToken);
    when(userRepository.findByFirebaseUid("firebase-uid")).thenReturn(Optional.of(adminUser));

    try (MockedStatic<FirebaseAuth> firebaseAuthStatic = Mockito.mockStatic(FirebaseAuth.class)) {
      firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

      MockHttpServletRequest request = new MockHttpServletRequest();
      request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer abc");
      MockHttpServletResponse response = new MockHttpServletResponse();
      MockFilterChain filterChain = new MockFilterChain();

      filter.doFilter(request, response, filterChain);

      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      assertEquals(" firebase-uid ", authentication.getPrincipal());
      assertTrue(
          authentication.getAuthorities().stream()
              .anyMatch(authority -> "ROLE_FIREBASE_AUTHENTICATED".equals(authority.getAuthority())));
      assertTrue(
          authentication.getAuthorities().stream()
              .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())));
      assertEquals(" firebase-uid ", request.getAttribute("firebaseUid"));
      assertEquals("admin@example.com", request.getAttribute("firebaseEmail"));
      assertEquals(true, request.getAttribute("firebaseEmailVerified"));
      assertEquals("7", request.getAttribute("backendUserId"));
      assertEquals("ADMIN", request.getAttribute("backendUserRole"));
      assertEquals(request, filterChain.getRequest());
    }
  }

  @Test
  void invalidTokensReturnUnauthorizedAndBlankBackendUidSkipsRepositoryLookup() throws Exception {
    UserRepository userRepository = mock(UserRepository.class);
    FirebaseTokenFilter filter = new FirebaseTokenFilter(userRepository);
    FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
    FirebaseToken firebaseToken = mock(FirebaseToken.class);
    FirebaseAuthException firebaseAuthException = mock(FirebaseAuthException.class);

    when(firebaseToken.getUid()).thenReturn("   ");
    when(firebaseToken.getClaims()).thenReturn(Map.of("email_verified", false));
    when(firebaseAuth.verifyIdToken("blank")).thenReturn(firebaseToken);
    when(userRepository.findByFirebaseUid("")).thenReturn(Optional.empty());
    when(firebaseAuth.verifyIdToken("bad")).thenThrow(firebaseAuthException);

    try (MockedStatic<FirebaseAuth> firebaseAuthStatic = Mockito.mockStatic(FirebaseAuth.class)) {
      firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);

      MockHttpServletRequest blankUidRequest = new MockHttpServletRequest();
      blankUidRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer blank");
      MockHttpServletResponse blankUidResponse = new MockHttpServletResponse();
      filter.doFilter(blankUidRequest, blankUidResponse, new MockFilterChain());
      assertFalse((Boolean) blankUidRequest.getAttribute("firebaseEmailVerified"));
      assertNull(blankUidRequest.getAttribute("backendUserId"));

      MockHttpServletRequest invalidRequest = new MockHttpServletRequest();
      invalidRequest.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad");
      MockHttpServletResponse invalidResponse = new MockHttpServletResponse();
      filter.doFilter(invalidRequest, invalidResponse, new MockFilterChain());

      assertEquals(401, invalidResponse.getStatus());
      assertTrue(invalidResponse.getContentAsString().contains("Invalid Firebase ID token."));
    }
  }
}
