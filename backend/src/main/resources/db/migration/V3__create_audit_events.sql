CREATE TABLE IF NOT EXISTS audit_events (
  id BIGSERIAL PRIMARY KEY,
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  request_id VARCHAR(128) NOT NULL,
  action VARCHAR(128) NOT NULL,
  outcome VARCHAR(16) NOT NULL,
  http_method VARCHAR(10) NOT NULL,
  endpoint VARCHAR(255) NOT NULL,
  target_type VARCHAR(64),
  target_id VARCHAR(128),
  status_code INTEGER NOT NULL,
  actor_backend_user_id BIGINT,
  actor_firebase_uid VARCHAR(191),
  actor_role VARCHAR(64),
  client_ip VARCHAR(64),
  user_agent VARCHAR(512),
  CONSTRAINT ck_audit_events_outcome CHECK (outcome IN ('ALLOWED', 'DENIED', 'ERROR'))
);

CREATE INDEX IF NOT EXISTS ix_audit_events_occurred_at
  ON audit_events (occurred_at DESC);

CREATE INDEX IF NOT EXISTS ix_audit_events_outcome
  ON audit_events (outcome);

CREATE INDEX IF NOT EXISTS ix_audit_events_actor_backend_user_id
  ON audit_events (actor_backend_user_id);

CREATE INDEX IF NOT EXISTS ix_audit_events_action
  ON audit_events (action);
