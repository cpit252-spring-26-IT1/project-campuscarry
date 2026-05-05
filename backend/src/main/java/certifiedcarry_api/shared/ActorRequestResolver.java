package certifiedcarry_api.shared;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public final class ActorRequestResolver {

  private static final String BACKEND_USER_ID_ATTRIBUTE = "backendUserId";
  private static final String BACKEND_USER_ROLE_ATTRIBUTE = "backendUserRole";

  private ActorRequestResolver() {}

  public static ActorRequestContext requireActor(
      Authentication authentication, HttpServletRequest request) {
    AuthGuards.requireAuthenticated(authentication);
    boolean isAdmin = AuthGuards.hasAdminRole(authentication);

    return new ActorRequestContext(
        requireLinkedBackendUserId(request, isAdmin),
        RequestAttributeParser.attributeAsString(request.getAttribute(BACKEND_USER_ROLE_ATTRIBUTE)),
        isAdmin);
  }

  public static ActorRequestContext requireActorAllowingUnlinkedAdmin(
      Authentication authentication, HttpServletRequest request) {
    AuthGuards.requireAuthenticated(authentication);
    boolean isAdmin = AuthGuards.hasAdminRole(authentication);
    Long backendUserId = optionalBackendUserId(request);

    if (backendUserId == null && isAdmin) {
      return new ActorRequestContext(
          0L,
          RequestAttributeParser.attributeAsString(request.getAttribute(BACKEND_USER_ROLE_ATTRIBUTE)),
          true);
    }

    return requireActor(authentication, request);
  }

  public static ActorRequestContext requireActorWithRole(
      Authentication authentication, HttpServletRequest request) {
    ActorRequestContext actor = requireActor(authentication, request);
    actor.requireBackendUserRole();
    return actor;
  }

  public static long requireActorUserId(
      Authentication authentication, HttpServletRequest request) {
    return requireActor(authentication, request).userId();
  }

  public static Long optionalBackendUserId(HttpServletRequest request) {
    return RequestAttributeParser.attributeAsLong(request.getAttribute(BACKEND_USER_ID_ATTRIBUTE));
  }

  private static long requireLinkedBackendUserId(HttpServletRequest request, boolean isAdmin) {
    Object backendUserId = request.getAttribute(BACKEND_USER_ID_ATTRIBUTE);
    if (backendUserId == null) {
      if (isAdmin) {
        throw new ResponseStatusException(
            HttpStatus.FORBIDDEN, "Admin account is not linked to backend user data.");
      }

      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "Authenticated user is not linked to backend user data.");
    }

    return RequestValueParser.parseNumericStringLong(
        backendUserId, BACKEND_USER_ID_ATTRIBUTE, HttpErrors::badRequest);
  }
}
