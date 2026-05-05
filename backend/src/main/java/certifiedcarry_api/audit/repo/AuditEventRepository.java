package certifiedcarry_api.audit.repo;

import certifiedcarry_api.audit.model.AuditEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {}
