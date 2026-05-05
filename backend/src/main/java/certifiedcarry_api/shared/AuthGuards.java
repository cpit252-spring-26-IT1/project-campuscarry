package certifiedcarry_api.shared;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public final class AuthGuards {

  private AuthGuards() {
  }

  public static void requireAuthenticated(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required.");
    }
  }

  public static boolean hasAdminRole(Authentication authentication) {
    if (authentication == null) {
      return false;
    }

    return authentication.getAuthorities().stream()
        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
  }
}
