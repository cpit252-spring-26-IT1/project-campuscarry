-- Flyway baseline migration generated from backend/db/schema.sql
-- Source: /Users/saudalshehri/Desktop/CertifiedCarry/backend/db/schema.sql


CREATE EXTENSION IF NOT EXISTS citext;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================
-- Enums
-- =========================
DO $$ BEGIN
  CREATE TYPE user_role_enum AS ENUM ('PLAYER', 'RECRUITER', 'ADMIN');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE user_status_enum AS ENUM ('PENDING', 'APPROVED', 'DECLINED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE rank_verification_status_enum AS ENUM ('NOT_SUBMITTED', 'PENDING', 'APPROVED', 'DECLINED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE pending_rank_status_enum AS ENUM ('PENDING', 'APPROVED', 'DECLINED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE registration_source_enum AS ENUM ('WEB_REGISTRATION', 'SYSTEM_SEED', 'ADMIN_PANEL');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE consent_source_enum AS ENUM ('REGISTER_PAGE', 'RECONSENT_GATE', 'SYSTEM_SEED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

DO $$ BEGIN
  CREATE TYPE consent_event_type_enum AS ENUM ('REGISTER_CONSENT_ACCEPTED', 'RECONSENT_ACCEPTED', 'SYSTEM_SEED_ACCEPTED');
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

-- =========================
-- Shared trigger function
-- =========================
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = NOW();
  RETURN NEW;
END;
$$;

-- =========================
-- Game catalog
-- =========================
CREATE TABLE IF NOT EXISTS games (
  name TEXT PRIMARY KEY, -- exact frontend value: Valorant, LoL, EA FC, Rocket League, Overwatch 2
  is_role_driven BOOLEAN NOT NULL DEFAULT FALSE,
  uses_competitive_modes BOOLEAN NOT NULL DEFAULT FALSE,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

DROP TRIGGER IF EXISTS trg_games_updated_at ON games;
CREATE TRIGGER trg_games_updated_at
BEFORE UPDATE ON games
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS game_aliases (
  alias_name TEXT PRIMARY KEY,
  game_name TEXT NOT NULL REFERENCES games(name) ON UPDATE CASCADE ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS game_roles (
  game_name TEXT NOT NULL REFERENCES games(name) ON UPDATE CASCADE ON DELETE CASCADE,
  role_name TEXT NOT NULL,
  sort_order INTEGER NOT NULL CHECK (sort_order > 0),
  PRIMARY KEY (game_name, role_name)
);

CREATE TABLE IF NOT EXISTS game_modes (
  game_name TEXT NOT NULL REFERENCES games(name) ON UPDATE CASCADE ON DELETE CASCADE,
  mode_name TEXT NOT NULL,
  sort_order INTEGER NOT NULL CHECK (sort_order > 0),
  PRIMARY KEY (game_name, mode_name)
);

CREATE TABLE IF NOT EXISTS game_ranks (
  game_name TEXT NOT NULL REFERENCES games(name) ON UPDATE CASCADE ON DELETE CASCADE,
  rank_name TEXT NOT NULL,
  sort_order INTEGER NOT NULL CHECK (sort_order > 0),
  rank_weight INTEGER NOT NULL CHECK (rank_weight > 0),
  rating_label TEXT NOT NULL DEFAULT 'MMR',
  requires_rating_value BOOLEAN NOT NULL DEFAULT TRUE,
  PRIMARY KEY (game_name, rank_name)
);

-- =========================
-- Users + consent
-- =========================
CREATE TABLE IF NOT EXISTS users (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  full_name TEXT NOT NULL,

  -- PLAYER fields
  username CITEXT,
  personal_email CITEXT,

  -- RECRUITER / ADMIN fields
  email CITEXT,
  firebase_uid TEXT,
  organization_name TEXT,
  linkedin_url TEXT,

  password_hash TEXT NOT NULL,

  role user_role_enum NOT NULL,
  status user_status_enum NOT NULL DEFAULT 'PENDING',
  decline_reason TEXT,
  declined_at TIMESTAMPTZ,

  registration_source registration_source_enum NOT NULL DEFAULT 'WEB_REGISTRATION',

  legal_consent_accepted BOOLEAN NOT NULL DEFAULT TRUE CHECK (legal_consent_accepted = TRUE),
  legal_consent_accepted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  legal_consent_locale VARCHAR(10) NOT NULL DEFAULT 'en',
  legal_consent_source consent_source_enum NOT NULL DEFAULT 'REGISTER_PAGE',
  terms_version_accepted VARCHAR(100) NOT NULL,
  privacy_version_accepted VARCHAR(100) NOT NULL,

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CHECK (
    (role = 'PLAYER'
      AND username IS NOT NULL
      AND personal_email IS NOT NULL
      AND email IS NULL
      AND organization_name IS NULL
      AND linkedin_url IS NULL)
    OR
    (role = 'RECRUITER'
      AND username IS NULL
      AND personal_email IS NULL
      AND email IS NOT NULL
      AND organization_name IS NOT NULL
      AND linkedin_url IS NOT NULL)
    OR
    (role = 'ADMIN'
      AND username IS NULL
      AND personal_email IS NULL
      AND email IS NOT NULL
      AND organization_name IS NULL
      AND linkedin_url IS NULL)
  ),
  CHECK (role <> 'ADMIN' OR status = 'APPROVED')
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username ON users (username) WHERE username IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_personal_email ON users (personal_email) WHERE personal_email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_email ON users (email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_firebase_uid ON users (firebase_uid) WHERE firebase_uid IS NOT NULL;

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

-- Hard DB guard: admin cannot be created from website flow
CREATE OR REPLACE FUNCTION enforce_users_role_policy()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    IF NEW.role = 'ADMIN' AND NEW.registration_source <> 'SYSTEM_SEED' THEN
      RAISE EXCEPTION 'ADMIN users can only be inserted with registration_source = SYSTEM_SEED';
    END IF;
  ELSIF TG_OP = 'UPDATE' THEN
    IF OLD.role <> 'ADMIN' AND NEW.role = 'ADMIN' THEN
      RAISE EXCEPTION 'Promoting an existing user to ADMIN is blocked at DB level';
    END IF;

    IF OLD.role = 'ADMIN' AND NEW.role <> 'ADMIN' THEN
      RAISE EXCEPTION 'Demoting ADMIN is blocked at DB level';
    END IF;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_users_role_policy ON users;
CREATE TRIGGER trg_users_role_policy
BEFORE INSERT OR UPDATE ON users
FOR EACH ROW
EXECUTE FUNCTION enforce_users_role_policy();

CREATE TABLE IF NOT EXISTS user_consent_events (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  event_type consent_event_type_enum NOT NULL,
  accepted_at TIMESTAMPTZ NOT NULL,
  terms_version VARCHAR(100) NOT NULL,
  privacy_version VARCHAR(100) NOT NULL,
  locale VARCHAR(10) NOT NULL,
  source consent_source_enum NOT NULL,
  request_id TEXT,
  ip_hash TEXT,
  user_agent_hash TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_consent_events_user_id ON user_consent_events(user_id);
CREATE INDEX IF NOT EXISTS idx_user_consent_events_accepted_at ON user_consent_events(accepted_at DESC);

CREATE OR REPLACE FUNCTION log_initial_consent_event()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  INSERT INTO user_consent_events (
    user_id, event_type, accepted_at, terms_version, privacy_version, locale, source
  )
  VALUES (
    NEW.id,
    CASE
      WHEN NEW.registration_source = 'SYSTEM_SEED' THEN 'SYSTEM_SEED_ACCEPTED'::consent_event_type_enum
      ELSE 'REGISTER_CONSENT_ACCEPTED'::consent_event_type_enum
    END,
    NEW.legal_consent_accepted_at,
    NEW.terms_version_accepted,
    NEW.privacy_version_accepted,
    NEW.legal_consent_locale,
    NEW.legal_consent_source
  );

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_users_log_initial_consent ON users;
CREATE TRIGGER trg_users_log_initial_consent
AFTER INSERT ON users
FOR EACH ROW
EXECUTE FUNCTION log_initial_consent_event();

-- =========================
-- Profiles / approval flow
-- =========================
CREATE TABLE IF NOT EXISTS player_profiles (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,

  username CITEXT NOT NULL,
  profile_image TEXT NOT NULL DEFAULT '',
  game TEXT REFERENCES games(name) ON UPDATE CASCADE,
  rank TEXT,

  allow_player_chats BOOLEAN NOT NULL DEFAULT TRUE,
  is_with_team BOOLEAN NOT NULL DEFAULT FALSE,
  team_name TEXT,
  rocket_league_modes TEXT[] NOT NULL DEFAULT '{}'::TEXT[],
  primary_rocket_league_mode TEXT,

  in_game_roles TEXT[] NOT NULL DEFAULT '{}'::TEXT[],
  in_game_role TEXT, -- legacy compatibility

  rating_value NUMERIC(10,2),
  rating_label TEXT NOT NULL DEFAULT 'MMR',
  proof_image TEXT NOT NULL DEFAULT '',
  bio TEXT NOT NULL DEFAULT '',
  clips_url TEXT NOT NULL DEFAULT '',

  rank_verification_status rank_verification_status_enum NOT NULL DEFAULT 'NOT_SUBMITTED',
  decline_reason TEXT NOT NULL DEFAULT '',
  declined_at TIMESTAMPTZ,
  is_verified BOOLEAN NOT NULL DEFAULT FALSE,
  submitted_at TIMESTAMPTZ,

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_player_profiles_rank
    FOREIGN KEY (game, rank) REFERENCES game_ranks(game_name, rank_name) ON UPDATE CASCADE,

  CONSTRAINT chk_player_profiles_game_rank_pair
    CHECK (
      (game IS NULL AND rank IS NULL)
      OR
      (game IS NOT NULL AND rank IS NOT NULL)
    ),

  CONSTRAINT chk_player_profiles_team_affiliation
    CHECK (
      (is_with_team = FALSE AND team_name IS NULL)
      OR
      (is_with_team = TRUE AND team_name IS NOT NULL AND btrim(team_name) <> '')
    )
);

DROP TRIGGER IF EXISTS trg_player_profiles_updated_at ON player_profiles;
CREATE TRIGGER trg_player_profiles_updated_at
BEFORE UPDATE ON player_profiles
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION assert_player_profile_user_role()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_role user_role_enum;
BEGIN
  SELECT role INTO v_role FROM users WHERE id = NEW.user_id;
  IF v_role IS DISTINCT FROM 'PLAYER' THEN
    RAISE EXCEPTION 'player_profiles.user_id must reference a PLAYER user';
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_player_profiles_user_role ON player_profiles;
CREATE TRIGGER trg_player_profiles_user_role
BEFORE INSERT OR UPDATE ON player_profiles
FOR EACH ROW
EXECUTE FUNCTION assert_player_profile_user_role();

CREATE TABLE IF NOT EXISTS pending_recruiters (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
  full_name TEXT NOT NULL,
  email CITEXT NOT NULL,
  linkedin_url TEXT NOT NULL,
  organization_name TEXT NOT NULL,
  submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  legal_consent_accepted_at TIMESTAMPTZ NOT NULL,
  legal_consent_locale VARCHAR(10) NOT NULL,
  terms_version_accepted VARCHAR(100) NOT NULL,
  privacy_version_accepted VARCHAR(100) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pending_recruiters_submitted_at ON pending_recruiters(submitted_at DESC);

CREATE OR REPLACE FUNCTION assert_pending_recruiter_user_role()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  v_role user_role_enum;
BEGIN
  SELECT role INTO v_role FROM users WHERE id = NEW.user_id;
  IF v_role IS DISTINCT FROM 'RECRUITER' THEN
    RAISE EXCEPTION 'pending_recruiters.user_id must reference a RECRUITER user';
  END IF;
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_pending_recruiters_user_role ON pending_recruiters;
CREATE TRIGGER trg_pending_recruiters_user_role
BEFORE INSERT OR UPDATE ON pending_recruiters
FOR EACH ROW
EXECUTE FUNCTION assert_pending_recruiter_user_role();

CREATE TABLE IF NOT EXISTS pending_ranks (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  username CITEXT NOT NULL,
  full_name TEXT NOT NULL,
  game TEXT NOT NULL REFERENCES games(name) ON UPDATE CASCADE,
  claimed_rank TEXT NOT NULL,

  in_game_roles TEXT[] NOT NULL DEFAULT '{}'::TEXT[],
  in_game_role TEXT, -- legacy compatibility
  rating_value NUMERIC(10,2),
  rating_label TEXT NOT NULL DEFAULT 'MMR',

  rocket_league_modes TEXT[] NOT NULL DEFAULT '{}'::TEXT[],
  primary_rocket_league_mode TEXT,

  proof_image TEXT NOT NULL,
  status pending_rank_status_enum NOT NULL DEFAULT 'PENDING',

  submitted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  resolved_at TIMESTAMPTZ,
  decline_reason TEXT,
  edited_after_decline BOOLEAN NOT NULL DEFAULT FALSE,
  edited_at TIMESTAMPTZ,

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_pending_ranks_rank
    FOREIGN KEY (game, claimed_rank) REFERENCES game_ranks(game_name, rank_name) ON UPDATE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_pending_ranks_user_id ON pending_ranks(user_id);
CREATE INDEX IF NOT EXISTS idx_pending_ranks_status ON pending_ranks(status);
CREATE INDEX IF NOT EXISTS idx_pending_ranks_submitted_at ON pending_ranks(submitted_at DESC);

DROP TRIGGER IF EXISTS trg_pending_ranks_updated_at ON pending_ranks;
CREATE TRIGGER trg_pending_ranks_updated_at
BEFORE UPDATE ON pending_ranks
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS leaderboard_entries (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  username CITEXT NOT NULL,
  game TEXT NOT NULL REFERENCES games(name) ON UPDATE CASCADE,
  rank TEXT NOT NULL,
  role TEXT, -- in-game role label
  rating_value NUMERIC(10,2),
  rating_label TEXT NOT NULL DEFAULT 'MMR',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT fk_leaderboard_rank
    FOREIGN KEY (game, rank) REFERENCES game_ranks(game_name, rank_name) ON UPDATE CASCADE,

  CONSTRAINT ux_leaderboard_user_game UNIQUE (user_id, game)
);

CREATE INDEX IF NOT EXISTS idx_leaderboard_game_rank ON leaderboard_entries(game, rank);
CREATE INDEX IF NOT EXISTS idx_leaderboard_username ON leaderboard_entries(username);

-- =========================
-- Chat
-- =========================
CREATE TABLE IF NOT EXISTS chat_threads (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  participant_user_id_1 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  participant_user_id_2 BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,

  initiated_by_id BIGINT NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
  initiated_by_role user_role_enum NOT NULL,

  last_sender_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
  last_message_at TIMESTAMPTZ,
  last_message_preview TEXT NOT NULL DEFAULT '',

  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

  CONSTRAINT chk_chat_thread_distinct_participants CHECK (participant_user_id_1 <> participant_user_id_2),
  CONSTRAINT chk_chat_thread_sorted_participants CHECK (participant_user_id_1 < participant_user_id_2),
  CONSTRAINT ux_chat_thread_pair UNIQUE (participant_user_id_1, participant_user_id_2)
);

CREATE INDEX IF NOT EXISTS idx_chat_threads_last_message_at ON chat_threads(last_message_at DESC NULLS LAST);

DROP TRIGGER IF EXISTS trg_chat_threads_updated_at ON chat_threads;
CREATE TRIGGER trg_chat_threads_updated_at
BEFORE UPDATE ON chat_threads
FOR EACH ROW
EXECUTE FUNCTION set_updated_at();

CREATE TABLE IF NOT EXISTS chat_messages (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  thread_id BIGINT NOT NULL REFERENCES chat_threads(id) ON DELETE CASCADE,
  sender_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  body TEXT NOT NULL CHECK (LENGTH(BTRIM(body)) > 0),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  read_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_chat_messages_thread_created_at ON chat_messages(thread_id, created_at);
CREATE INDEX IF NOT EXISTS idx_chat_messages_recipient_read_at ON chat_messages(recipient_id, read_at);

CREATE OR REPLACE FUNCTION assert_chat_message_participants()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
  p1 BIGINT;
  p2 BIGINT;
BEGIN
  SELECT participant_user_id_1, participant_user_id_2
  INTO p1, p2
  FROM chat_threads
  WHERE id = NEW.thread_id;

  IF NOT FOUND THEN
    RAISE EXCEPTION 'chat thread % does not exist', NEW.thread_id;
  END IF;

  IF NEW.sender_id = NEW.recipient_id THEN
    RAISE EXCEPTION 'sender_id and recipient_id cannot be the same';
  END IF;

  IF NEW.sender_id NOT IN (p1, p2) OR NEW.recipient_id NOT IN (p1, p2) THEN
    RAISE EXCEPTION 'sender/recipient must be participants of thread %', NEW.thread_id;
  END IF;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_chat_messages_participants ON chat_messages;
CREATE TRIGGER trg_chat_messages_participants
BEFORE INSERT OR UPDATE ON chat_messages
FOR EACH ROW
EXECUTE FUNCTION assert_chat_message_participants();

CREATE OR REPLACE FUNCTION sync_chat_thread_summary()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
  UPDATE chat_threads
  SET
    last_sender_id = NEW.sender_id,
    last_message_at = NEW.created_at,
    last_message_preview = LEFT(NEW.body, 180),
    updated_at = NOW()
  WHERE id = NEW.thread_id;

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_chat_messages_sync_thread ON chat_messages;
CREATE TRIGGER trg_chat_messages_sync_thread
AFTER INSERT ON chat_messages
FOR EACH ROW
EXECUTE FUNCTION sync_chat_thread_summary();

-- =========================
-- Seed game catalog
-- =========================
INSERT INTO games (name, is_role_driven, uses_competitive_modes)
VALUES
  ('Valorant', TRUE, FALSE),
  ('LoL', TRUE, FALSE),
  ('EA FC', FALSE, FALSE),
  ('Rocket League', FALSE, TRUE),
  ('Overwatch 2', TRUE, FALSE)
ON CONFLICT (name) DO NOTHING;

INSERT INTO game_aliases (alias_name, game_name)
VALUES
  ('League of Legends', 'LoL')
ON CONFLICT (alias_name) DO NOTHING;

INSERT INTO game_roles (game_name, role_name, sort_order)
VALUES
  ('Valorant', 'Duelist', 1),
  ('Valorant', 'Initiator', 2),
  ('Valorant', 'Controller', 3),
  ('Valorant', 'Sentinel', 4),

  ('LoL', 'Top Laner', 1),
  ('LoL', 'Jungler', 2),
  ('LoL', 'Mid Laner', 3),
  ('LoL', 'Bot Laner (ADC)', 4),
  ('LoL', 'Support', 5),

  ('Overwatch 2', 'Tank', 1),
  ('Overwatch 2', 'Damage', 2),
  ('Overwatch 2', 'Support', 3)
ON CONFLICT (game_name, role_name) DO NOTHING;

INSERT INTO game_modes (game_name, mode_name, sort_order)
VALUES
  ('Rocket League', '1v1', 1),
  ('Rocket League', '2v2', 2),
  ('Rocket League', '3v3', 3)
ON CONFLICT (game_name, mode_name) DO NOTHING;

INSERT INTO game_ranks (game_name, rank_name, sort_order, rank_weight, rating_label, requires_rating_value)
VALUES
  -- Valorant
  ('Valorant', 'Immortal 1', 1, 1, 'MMR', TRUE),
  ('Valorant', 'Immortal 2', 2, 2, 'MMR', TRUE),
  ('Valorant', 'Immortal 3', 3, 3, 'MMR', TRUE),
  ('Valorant', 'Radiant', 4, 4, 'MMR', TRUE),

  -- LoL
  ('LoL', 'Grandmaster', 1, 1, 'MMR', TRUE),
  ('LoL', 'Challenger', 2, 2, 'MMR', TRUE),

  -- EA FC
  ('EA FC', 'Division 1', 1, 1, 'MMR', FALSE),
  ('EA FC', 'Elite', 2, 2, 'Skill Rating', TRUE),

  -- Rocket League
  ('Rocket League', 'Grand Champion 1', 1, 1, 'MMR', TRUE),
  ('Rocket League', 'Grand Champion 2', 2, 2, 'MMR', TRUE),
  ('Rocket League', 'Grand Champion 3', 3, 3, 'MMR', TRUE),
  ('Rocket League', 'Supersonic Legend', 4, 4, 'MMR', TRUE),

  -- Overwatch 2
  ('Overwatch 2', 'Top 500', 1, 3, 'Rank', FALSE),
  ('Overwatch 2', 'Champion', 2, 2, 'Rank', FALSE),
  ('Overwatch 2', 'Grandmaster', 3, 1, 'Rank', FALSE)
ON CONFLICT (game_name, rank_name) DO NOTHING;

-- =========================
-- Seed mandatory admin account (manual Firebase UID linking required)
-- =========================
INSERT INTO users (
  full_name,
  username,
  personal_email,
  email,
  firebase_uid,
  organization_name,
  password_hash,
  role,
  status,
  registration_source,
  legal_consent_accepted,
  legal_consent_accepted_at,
  legal_consent_locale,
  legal_consent_source,
  terms_version_accepted,
  privacy_version_accepted
)
VALUES (
  'CertifiedCarry Admin',
  NULL,
  NULL,
  'admin@certifiedcarry.sa',
  'MANUAL_ADMIN_LINK_REQUIRED',
  NULL,
  crypt(md5(random()::text || clock_timestamp()::text), gen_salt('bf', 12)),
  'ADMIN',
  'APPROVED',
  'SYSTEM_SEED',
  TRUE,
  NOW(),
  'en',
  'SYSTEM_SEED',
  'cc-terms-2026-04-04',
  'cc-privacy-2026-04-04'
)
ON CONFLICT DO NOTHING;

