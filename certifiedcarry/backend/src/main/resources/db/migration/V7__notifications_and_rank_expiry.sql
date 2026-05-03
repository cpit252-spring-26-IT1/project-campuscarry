ALTER TABLE player_profiles
  ADD COLUMN IF NOT EXISTS rank_verified_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS rank_expires_at TIMESTAMPTZ,
  ADD COLUMN IF NOT EXISTS rank_expiry_reminder_sent_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_player_profiles_rank_expires_at
  ON player_profiles(rank_expires_at)
  WHERE rank_expires_at IS NOT NULL;

UPDATE player_profiles
SET
  rank_verified_at = COALESCE(rank_verified_at, updated_at, created_at, NOW()),
  rank_expires_at = COALESCE(
    rank_expires_at,
    COALESCE(rank_verified_at, updated_at, created_at, NOW()) + INTERVAL '30 days'
  ),
  rank_expiry_reminder_sent_at = NULL
WHERE (is_verified = TRUE OR rank_verification_status = 'APPROVED')
  AND rank_expires_at IS NULL;

CREATE TABLE IF NOT EXISTS user_notification_states (
  user_id BIGINT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  is_logged_in BOOLEAN NOT NULL DEFAULT FALSE,
  chat_unread_notified_since_last_login BOOLEAN NOT NULL DEFAULT FALSE,
  last_login_at TIMESTAMPTZ,
  last_logout_at TIMESTAMPTZ,
  last_activity_at TIMESTAMPTZ,
  last_chat_unread_notified_at TIMESTAMPTZ,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DROP TRIGGER IF EXISTS trg_user_notification_states_updated_at ON user_notification_states;
CREATE TRIGGER trg_user_notification_states_updated_at
BEFORE UPDATE ON user_notification_states
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();
