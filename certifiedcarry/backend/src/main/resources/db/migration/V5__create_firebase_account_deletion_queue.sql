CREATE TABLE IF NOT EXISTS firebase_account_deletion_queue (
  id BIGSERIAL PRIMARY KEY,
  backend_user_id BIGINT,
  firebase_uid VARCHAR(191),
  email VARCHAR(320),
  attempts INTEGER NOT NULL DEFAULT 0,
  last_error TEXT,
  next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT ck_firebase_account_deletion_queue_identifier
    CHECK (firebase_uid IS NOT NULL OR email IS NOT NULL),
  CONSTRAINT ck_firebase_account_deletion_queue_attempts
    CHECK (attempts >= 0)
);

CREATE INDEX IF NOT EXISTS ix_firebase_account_deletion_queue_next_attempt
  ON firebase_account_deletion_queue (next_attempt_at);

CREATE INDEX IF NOT EXISTS ix_firebase_account_deletion_queue_backend_user_id
  ON firebase_account_deletion_queue (backend_user_id);