ALTER TABLE users
  ADD COLUMN IF NOT EXISTS firebase_uid TEXT;

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_firebase_uid
  ON users (firebase_uid)
  WHERE firebase_uid IS NOT NULL;
