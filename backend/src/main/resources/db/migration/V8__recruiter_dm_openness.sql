DO $$ BEGIN
  CREATE TYPE recruiter_dm_openness_enum AS ENUM (
    'OPEN_ALL_PLAYERS',
    'OPEN_VERIFIED_PLAYERS',
    'CLOSED'
  );
EXCEPTION WHEN duplicate_object THEN NULL; END $$;

ALTER TABLE users
  ADD COLUMN IF NOT EXISTS recruiter_dm_openness recruiter_dm_openness_enum NOT NULL DEFAULT 'CLOSED';

UPDATE users
SET recruiter_dm_openness = 'CLOSED'
WHERE recruiter_dm_openness IS NULL;
