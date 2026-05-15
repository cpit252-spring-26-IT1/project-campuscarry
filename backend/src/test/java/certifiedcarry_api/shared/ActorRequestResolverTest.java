package certifiedcarry_api.shared;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

class ActorRequestResolverTest {

  @Test
  void requireActorBuildsContextForLinkedUser() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("backendUserId", "17");
    request.setAttribute("backendUserRole", " recruiter ");

    ActorRequestContext context = ActorRequestResolver.requireActor(nonAdminAuth(), request);

    assertEquals(17L, context.userId());
    assertEquals("RECRUITER", context.backendUserRole());
  }

  @Test
  void requireActorAllowingUnlinkedAdminReturnsSyntheticAdminContext() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("backendUserRole", "admin");

    ActorRequestContext context =
        ActorRequestResolver.requireActorAllowingUnlinkedAdmin(adminAuth(), request);

    assertEquals(0L, context.userId());
    assertEquals("ADMIN", context.backendUserRole());
    assertEquals(true, context.isAdmin());
  }

  @Test
  void requireActorRejectsMissingLinkedBackendUserForNonAdmin() {
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> ActorRequestResolver.requireActor(nonAdminAuth(), new MockHttpServletRequest()));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    assertEquals("Authenticated user is not linked to backend user data.", exception.getReason());
  }

  @Test
  void requireActorUserIdRejectsMissingLinkedBackendUserForAdmin() {
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> ActorRequestResolver.requireActorUserId(adminAuth(), new MockHttpServletRequest()));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    assertEquals("Admin account is not linked to backend user data.", exception.getReason());
  }

  @Test
  void requireActorWithRoleRejectsBlankRole() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute("backendUserId", "22");

    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class,
            () -> ActorRequestResolver.requireActorWithRole(nonAdminAuth(), request));

    assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    assertEquals("backendUserRole is required.", exception.getReason());
  }

  @Test
  void optionalBackendUserIdReturnsNullWhenUnset() {
    assertNull(ActorRequestResolver.optionalBackendUserId(new MockHttpServletRequest()));
  }

  private UsernamePasswordAuthenticationToken nonAdminAuth() {
    return new UsernamePasswordAuthenticationToken(
        "player", "n/a", java.util.List.of(new SimpleGrantedAuthority("ROLE_PLAYER")));
  }

  private UsernamePasswordAuthenticationToken adminAuth() {
    return new UsernamePasswordAuthenticationToken(
        "admin", "n/a", java.util.List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
  }
}
