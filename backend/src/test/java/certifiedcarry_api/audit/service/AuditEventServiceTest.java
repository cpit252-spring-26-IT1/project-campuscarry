package certifiedcarry_api.audit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;

import certifiedcarry_api.audit.model.AuditEventEntity;
import certifiedcarry_api.audit.repo.AuditEventRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AuditEventServiceTest {

  @Test
  void recordEventAppliesDefaultsUppercasingAndLengthLimits() {
    AuditEventRepository repository = org.mockito.Mockito.mock(AuditEventRepository.class);
    AuditEventService service = new AuditEventService(repository);

    service.recordEvent(
        new AuditEventRecord(
            "   ",
            null,
            " success ",
            "",
            null,
            "x".repeat(80),
            "   ",
            204,
            7L,
            "f".repeat(220),
            " recruiter ",
            " 10.0.0.5 ",
            "u".repeat(600)));

    ArgumentCaptor<AuditEventEntity> captor = ArgumentCaptor.forClass(AuditEventEntity.class);
    verify(repository).save(captor.capture());

    AuditEventEntity entity = captor.getValue();
    assertEquals("unknown-request", entity.getRequestId());
    assertEquals("UNKNOWN_ACTION", entity.getAction());
    assertEquals("SUCCESS", entity.getOutcome());
    assertEquals("UNKNOWN", entity.getHttpMethod());
    assertEquals("/unknown", entity.getEndpoint());
    assertEquals("x".repeat(64), entity.getTargetType());
    assertNull(entity.getTargetId());
    assertEquals(204, entity.getStatusCode());
    assertEquals(7L, entity.getActorBackendUserId());
    assertEquals("f".repeat(191), entity.getActorFirebaseUid());
    assertEquals("recruiter", entity.getActorRole());
    assertEquals("10.0.0.5", entity.getClientIp());
    assertEquals("u".repeat(512), entity.getUserAgent());
  }
}
