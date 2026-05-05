package certifiedcarry_api.audit.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
public class AuditEventEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "occurred_at", insertable = false, updatable = false)
  private OffsetDateTime occurredAt;

  @Column(name = "request_id", nullable = false)
  private String requestId;

  @Column(name = "action", nullable = false)
  private String action;

  @Column(name = "outcome", nullable = false)
  private String outcome;

  @Column(name = "http_method", nullable = false)
  private String httpMethod;

  @Column(name = "endpoint", nullable = false)
  private String endpoint;

  @Column(name = "target_type")
  private String targetType;

  @Column(name = "target_id")
  private String targetId;

  @Column(name = "status_code", nullable = false)
  private Integer statusCode;

  @Column(name = "actor_backend_user_id")
  private Long actorBackendUserId;

  @Column(name = "actor_firebase_uid")
  private String actorFirebaseUid;

  @Column(name = "actor_role")
  private String actorRole;

  @Column(name = "client_ip")
  private String clientIp;

  @Column(name = "user_agent")
  private String userAgent;
}
