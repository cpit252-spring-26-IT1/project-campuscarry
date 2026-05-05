package certifiedcarry_api.audit.service;

import certifiedcarry_api.audit.model.AuditEventEntity;
import certifiedcarry_api.audit.repo.AuditEventRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditEventService {

  private final AuditEventRepository auditEventRepository;

  public AuditEventService(AuditEventRepository auditEventRepository) {
    this.auditEventRepository = auditEventRepository;
  }

  @Transactional
  public void recordEvent(AuditEventRecord eventRecord) {
    AuditEventEntity eventEntity = new AuditEventEntity();
    eventEntity.setRequestId(requiredValue(eventRecord.requestId(), "unknown-request", 128));
    eventEntity.setAction(requiredValue(eventRecord.action(), "UNKNOWN_ACTION", 128));

    String outcome = requiredValue(eventRecord.outcome(), "ERROR", 16);
    eventEntity.setOutcome(outcome.toUpperCase(Locale.ROOT));

    eventEntity.setHttpMethod(requiredValue(eventRecord.httpMethod(), "UNKNOWN", 10));
    eventEntity.setEndpoint(requiredValue(eventRecord.endpoint(), "/unknown", 255));
    eventEntity.setTargetType(normalize(eventRecord.targetType(), 64));
    eventEntity.setTargetId(normalize(eventRecord.targetId(), 128));
    eventEntity.setStatusCode(eventRecord.statusCode());
    eventEntity.setActorBackendUserId(eventRecord.actorBackendUserId());
    eventEntity.setActorFirebaseUid(normalize(eventRecord.actorFirebaseUid(), 191));
    eventEntity.setActorRole(normalize(eventRecord.actorRole(), 64));
    eventEntity.setClientIp(normalize(eventRecord.clientIp(), 64));
    eventEntity.setUserAgent(normalize(eventRecord.userAgent(), 512));

    auditEventRepository.save(eventEntity);
  }

  private String requiredValue(String value, String defaultValue, int maxLength) {
    String normalized = normalize(value, maxLength);
    return normalized == null ? defaultValue : normalized;
  }

  private String normalize(String value, int maxLength) {
    if (value == null) {
      return null;
    }

    String normalized = value.trim();
    if (normalized.isEmpty()) {
      return null;
    }

    if (normalized.length() <= maxLength) {
      return normalized;
    }

    return normalized.substring(0, maxLength);
  }
}
