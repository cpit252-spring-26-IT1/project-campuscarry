package certifiedcarry_api.shared;

import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public record ActorRequestContext(long userId, String backendUserRole, boolean isAdmin) {

  public ActorRequestContext {
    backendUserRole = normalizeRole(backendUserRole);
  }

  public String requireBackendUserRole() {
    if (backendUserRole.isBlank()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "backendUserRole is required.");
    }

    return backendUserRole;
  }

  private static String normalizeRole(String backendUserRole) {
    if (backendUserRole == null) {
      return "";
    }

    return backendUserRole.trim().toUpperCase(Locale.ROOT);
  }
}
