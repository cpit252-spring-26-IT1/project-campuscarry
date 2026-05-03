package certifiedcarry_api.config.ratelimit;

import certifiedcarry_api.config.ApiRouteCatalog;
import certifiedcarry_api.config.ApiRouteCatalog.EndpointPolicy;
import certifiedcarry_api.config.ApiRouteCatalog.RateLimitPolicy;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestRateLimitFilter extends OncePerRequestFilter {

  private static final int CLEANUP_INTERVAL = 500;

  private final Map<String, WindowState> counters = new ConcurrentHashMap<>();
  private final AtomicInteger processedRequests = new AtomicInteger();

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    EndpointPolicy policy = resolveRateLimitedPolicy(request);
    if (policy == null) {
      filterChain.doFilter(request, response);
      return;
    }

    long now = System.currentTimeMillis();
    RateLimitPolicy rateLimitPolicy = policy.rateLimitPolicy();
    String bucketKey = buildBucketKey(request, policy);
    if (!tryAcquire(bucketKey, rateLimitPolicy, now)) {
      writeRateLimitResponse(response, rateLimitPolicy.windowSeconds());
      return;
    }

    maybeCleanup(now);
    filterChain.doFilter(request, response);
  }

  private EndpointPolicy resolveRateLimitedPolicy(HttpServletRequest request) {
    HttpMethod method;
    try {
      method = HttpMethod.valueOf(request.getMethod());
    } catch (IllegalArgumentException exception) {
      return null;
    }

    return ApiRouteCatalog.resolveRateLimitedPolicy(method, ApiRouteCatalog.resolveRequestPath(request));
  }

  private String buildBucketKey(HttpServletRequest request, EndpointPolicy policy) {
    return policy.method().name()
        + ":"
        + policy.pathPrefix()
        + ":"
        + resolveClientIdentity(request);
  }

  private String resolveClientIdentity(HttpServletRequest request) {
    Object backendUserId = request.getAttribute("backendUserId");
    if (backendUserId != null) {
      String normalizedBackendUserId = String.valueOf(backendUserId).trim();
      if (!normalizedBackendUserId.isEmpty()) {
        return "user:" + normalizedBackendUserId;
      }
    }

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      String principal = String.valueOf(authentication.getName()).trim();
      if (!principal.isEmpty()) {
        return "principal:" + principal;
      }
    }

    return "ip:" + resolveClientIp(request);
  }

  private String resolveClientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      String firstIp = forwardedFor.split(",")[0].trim();
      if (!firstIp.isEmpty()) {
        return firstIp;
      }
    }

    String remoteAddress = request.getRemoteAddr();
    if (remoteAddress != null && !remoteAddress.isBlank()) {
      return remoteAddress;
    }

    return "unknown";
  }

  private boolean tryAcquire(String bucketKey, RateLimitPolicy rule, long nowMillis) {
    long windowMillis = rule.windowSeconds() * 1000L;

    WindowState state =
        counters.compute(
            bucketKey,
            (key, currentState) -> {
              if (currentState == null
                  || nowMillis - currentState.windowStartMillis() >= currentState.windowSizeMillis()) {
                return new WindowState(nowMillis, 1, windowMillis);
              }

              return new WindowState(
                  currentState.windowStartMillis(), currentState.count() + 1, currentState.windowSizeMillis());
            });

    return state.count() <= rule.maxRequests();
  }

  private void maybeCleanup(long nowMillis) {
    if (processedRequests.incrementAndGet() % CLEANUP_INTERVAL != 0) {
      return;
    }

    counters.entrySet().removeIf(entry -> nowMillis - entry.getValue().windowStartMillis() >= entry.getValue().windowSizeMillis());
  }

  private void writeRateLimitResponse(HttpServletResponse response, int retryAfterSeconds)
      throws IOException {
    response.setStatus(429);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding(StandardCharsets.UTF_8.name());
    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
    response
        .getWriter()
        .write(
            "{\"error\":\"Too many requests.\",\"code\":\"rate_limit_exceeded\",\"retryAfterSeconds\":"
                + retryAfterSeconds
                + "}");
  }

  private record WindowState(long windowStartMillis, int count, long windowSizeMillis) {}
}
