package certifiedcarry_api.config.audit;

import certifiedcarry_api.audit.service.AuditEventRecord;
import certifiedcarry_api.audit.service.AuditEventService;
import certifiedcarry_api.config.ApiRouteCatalog;
import certifiedcarry_api.config.ApiRouteCatalog.AuditPolicy;
import certifiedcarry_api.config.ApiRouteCatalog.EndpointPolicy;
import certifiedcarry_api.shared.RequestAttributeParser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuditLoggingFilter extends OncePerRequestFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLoggingFilter.class);

  private static final String REQUEST_ID_HEADER = "X-Request-Id";

  private final ObjectProvider<AuditEventService> auditEventServiceProvider;

  public AuditLoggingFilter(ObjectProvider<AuditEventService> auditEventServiceProvider) {
    this.auditEventServiceProvider = auditEventServiceProvider;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return resolveAuditedPolicy(request) == null;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    EndpointPolicy policy = resolveAuditedPolicy(request);
    if (policy == null) {
      filterChain.doFilter(request, response);
      return;
    }

    String requestId = resolveOrCreateRequestId(request);
    response.setHeader(REQUEST_ID_HEADER, requestId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      recordAuditEvent(policy, request, response, requestId);
    }
  }

  private EndpointPolicy resolveAuditedPolicy(HttpServletRequest request) {
    HttpMethod requestMethod;
    try {
      requestMethod = HttpMethod.valueOf(request.getMethod());
    } catch (IllegalArgumentException exception) {
      return null;
    }

    return ApiRouteCatalog.resolveAuditedPolicy(
        requestMethod, ApiRouteCatalog.resolveRequestPath(request));
  }

  private void recordAuditEvent(
      EndpointPolicy policy,
      HttpServletRequest request,
      HttpServletResponse response,
      String requestId) {
    AuditEventService auditEventService = auditEventServiceProvider.getIfAvailable();
    if (auditEventService == null) {
      return;
    }

    String requestPath = ApiRouteCatalog.resolveRequestPath(request);
    AuditPolicy auditPolicy = policy.auditPolicy();

    AuditEventRecord eventRecord =
        new AuditEventRecord(
            requestId,
            auditPolicy.action(),
            resolveOutcome(response.getStatus()),
            request.getMethod(),
            requestPath,
            auditPolicy.targetType(),
            policy.extractTargetId(requestPath),
            response.getStatus(),
            RequestAttributeParser.attributeAsLong(request.getAttribute("backendUserId")),
            RequestAttributeParser.attributeAsString(request.getAttribute("firebaseUid")),
            RequestAttributeParser.attributeAsString(request.getAttribute("backendUserRole")),
            resolveClientIp(request),
            request.getHeader("User-Agent"));

    try {
      auditEventService.recordEvent(eventRecord);
    } catch (RuntimeException exception) {
      LOGGER.warn(
          "Failed to record audit event for requestId={} and action={}",
          requestId,
          auditPolicy.action(),
          exception);
    }
  }

  private String resolveOutcome(int statusCode) {
    if (statusCode == 401 || statusCode == 403 || statusCode == 429) {
      return "DENIED";
    }

    if (statusCode >= 200 && statusCode < 400) {
      return "ALLOWED";
    }

    return "ERROR";
  }

  private String resolveOrCreateRequestId(HttpServletRequest request) {
    String requestId = RequestAttributeParser.attributeAsString(request.getHeader(REQUEST_ID_HEADER));
    if (requestId != null) {
      return requestId;
    }

    return UUID.randomUUID().toString();
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
    if (remoteAddress == null || remoteAddress.isBlank()) {
      return null;
    }

    return remoteAddress;
  }
}
