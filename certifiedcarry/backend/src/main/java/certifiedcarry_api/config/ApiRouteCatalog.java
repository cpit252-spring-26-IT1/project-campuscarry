package certifiedcarry_api.config;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.http.HttpMethod;

public final class ApiRouteCatalog {

  public static final String USERS = "/users";
  public static final String USERS_ME_DM_OPENNESS = "/users/me/dm-openness";
  public static final String AUTH_ME = "/auth/me";
  public static final String AUTH_SESSION_LOGIN = "/auth/session/login";
  public static final String AUTH_SESSION_LOGOUT = "/auth/session/logout";
  public static final String AUTH_SIGNUP_COMPLETE = "/auth/signup/complete";
  public static final String MEDIA_UPLOADS_PRESIGN = "/media/uploads/presign";
  public static final String PLAYER_PROFILES = "/player_profiles";
  public static final String LEADERBOARD = "/leaderboard";
  public static final String PENDING_RECRUITERS = "/pending_recruiters";
  public static final String PENDING_RANKS = "/pending_ranks";
  public static final String CHAT_THREADS = "/chat_threads";
  public static final String CHAT_MESSAGES = "/chat_messages";

  private static final List<EndpointPolicy> ENDPOINT_POLICIES =
      List.of(
          new EndpointPolicy(
              HttpMethod.POST,
              USERS,
              new RateLimitPolicy(15, 60),
              new AuditPolicy("USER_REGISTER", "user")),
          new EndpointPolicy(HttpMethod.POST, AUTH_SESSION_LOGIN, new RateLimitPolicy(30, 60), null),
          new EndpointPolicy(
              HttpMethod.POST, AUTH_SESSION_LOGOUT, new RateLimitPolicy(60, 60), null),
          new EndpointPolicy(
              HttpMethod.PATCH, USERS, null, new AuditPolicy("USER_UPDATE", "user")),
          new EndpointPolicy(
              HttpMethod.DELETE, USERS, null, new AuditPolicy("USER_DELETE", "user")),
          new EndpointPolicy(
              HttpMethod.POST,
              PENDING_RECRUITERS,
              new RateLimitPolicy(10, 60),
              new AuditPolicy("PENDING_RECRUITER_CREATE", "pending_recruiter")),
          new EndpointPolicy(
              HttpMethod.DELETE,
              PENDING_RECRUITERS,
              null,
              new AuditPolicy("PENDING_RECRUITER_DELETE", "pending_recruiter")),
          new EndpointPolicy(
              HttpMethod.POST,
              PENDING_RANKS,
              new RateLimitPolicy(10, 60),
              new AuditPolicy("PENDING_RANK_CREATE", "pending_rank")),
          new EndpointPolicy(
              HttpMethod.PATCH,
              PENDING_RANKS,
              new RateLimitPolicy(20, 60),
              new AuditPolicy("PENDING_RANK_UPDATE", "pending_rank")),
          new EndpointPolicy(
              HttpMethod.DELETE,
              PENDING_RANKS,
              null,
              new AuditPolicy("PENDING_RANK_DELETE", "pending_rank")),
          new EndpointPolicy(
              HttpMethod.POST,
              PLAYER_PROFILES,
              new RateLimitPolicy(20, 60),
              new AuditPolicy("PLAYER_PROFILE_CREATE", "player_profile")),
          new EndpointPolicy(
              HttpMethod.PATCH,
              PLAYER_PROFILES,
              new RateLimitPolicy(30, 60),
              new AuditPolicy("PLAYER_PROFILE_UPDATE", "player_profile")),
          new EndpointPolicy(
              HttpMethod.POST,
              LEADERBOARD,
              new RateLimitPolicy(20, 60),
              new AuditPolicy("LEADERBOARD_CREATE", "leaderboard_entry")),
          new EndpointPolicy(
              HttpMethod.PATCH,
              LEADERBOARD,
              new RateLimitPolicy(30, 60),
              new AuditPolicy("LEADERBOARD_UPDATE", "leaderboard_entry")),
          new EndpointPolicy(
              HttpMethod.DELETE,
              LEADERBOARD,
              null,
              new AuditPolicy("LEADERBOARD_DELETE", "leaderboard_entry")),
          new EndpointPolicy(
              HttpMethod.POST,
              CHAT_THREADS,
              new RateLimitPolicy(20, 60),
              new AuditPolicy("CHAT_THREAD_CREATE", "chat_thread")),
          new EndpointPolicy(
              HttpMethod.PATCH,
              CHAT_THREADS,
              new RateLimitPolicy(60, 60),
              new AuditPolicy("CHAT_THREAD_UPDATE", "chat_thread")),
          new EndpointPolicy(
              HttpMethod.POST,
              CHAT_MESSAGES,
              new RateLimitPolicy(30, 60),
              new AuditPolicy("CHAT_MESSAGE_CREATE", "chat_message")),
          new EndpointPolicy(
              HttpMethod.PATCH,
              CHAT_MESSAGES,
              new RateLimitPolicy(60, 60),
              new AuditPolicy("CHAT_MESSAGE_UPDATE", "chat_message")),
          new EndpointPolicy(
              HttpMethod.POST,
              MEDIA_UPLOADS_PRESIGN,
              new RateLimitPolicy(40, 60),
              new AuditPolicy("MEDIA_UPLOAD_PRESIGN", "media_asset")));

  private ApiRouteCatalog() {}

  public static String apiPath(String path) {
    return "/api" + path;
  }

  public static String recursive(String path) {
    return path + "/**";
  }

  public static boolean isUserRegistrationPath(String rawPath) {
    return hasPathSuffix(rawPath, USERS);
  }

  public static boolean isSignupCompletionPath(String rawPath) {
    return hasPathSuffix(rawPath, AUTH_SIGNUP_COMPLETE);
  }

  public static String resolveRequestPath(HttpServletRequest request) {
    String requestUri = normalizePath(request.getRequestURI());
    String contextPath = request.getContextPath();

    if (contextPath != null
        && !contextPath.isBlank()
        && requestUri.startsWith(contextPath)
        && requestUri.length() > contextPath.length()) {
      return requestUri.substring(contextPath.length());
    }

    return requestUri;
  }

  public static EndpointPolicy resolveRateLimitedPolicy(HttpMethod method, String requestPath) {
    return ENDPOINT_POLICIES.stream()
        .filter(policy -> policy.rateLimitPolicy() != null && policy.matches(method, requestPath))
        .findFirst()
        .orElse(null);
  }

  public static EndpointPolicy resolveAuditedPolicy(HttpMethod method, String requestPath) {
    return ENDPOINT_POLICIES.stream()
        .filter(policy -> policy.auditPolicy() != null && policy.matches(method, requestPath))
        .findFirst()
        .orElse(null);
  }

  private static boolean hasPathSuffix(String rawPath, String pathSuffix) {
    if (rawPath == null || rawPath.isBlank()) {
      return false;
    }

    String normalizedPath = rawPath.replaceAll("/{2,}", "/");
    return normalizedPath.equals(pathSuffix)
        || normalizedPath.equals(pathSuffix + "/")
        || normalizedPath.endsWith(pathSuffix)
        || normalizedPath.endsWith(pathSuffix + "/");
  }

  private static String normalizePath(String path) {
    if (path == null || path.isBlank()) {
      return "/";
    }

    return path;
  }

  public record RateLimitPolicy(int maxRequests, int windowSeconds) {}

  public record AuditPolicy(String action, String targetType) {}

  public record EndpointPolicy(
      HttpMethod method,
      String pathPrefix,
      RateLimitPolicy rateLimitPolicy,
      AuditPolicy auditPolicy) {

    public boolean matches(HttpMethod requestMethod, String requestPath) {
      if (method != requestMethod) {
        return false;
      }

      return requestPath.equals(pathPrefix) || requestPath.startsWith(pathPrefix + "/");
    }

    public String extractTargetId(String requestPath) {
      if (!requestPath.startsWith(pathPrefix + "/")) {
        return null;
      }

      String suffix = requestPath.substring(pathPrefix.length() + 1);
      if (suffix.isBlank()) {
        return null;
      }

      int slashIndex = suffix.indexOf('/');
      if (slashIndex < 0) {
        return suffix;
      }

      return suffix.substring(0, slashIndex);
    }
  }
}
