package certifiedcarry_api.audit.service;

public record AuditEventRecord(
    String requestId,
    String action,
    String outcome,
    String httpMethod,
    String endpoint,
    String targetType,
    String targetId,
    int statusCode,
    Long actorBackendUserId,
    String actorFirebaseUid,
    String actorRole,
    String clientIp,
    String userAgent) {}
