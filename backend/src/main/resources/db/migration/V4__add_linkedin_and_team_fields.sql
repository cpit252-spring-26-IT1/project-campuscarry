ALTER TABLE users
  ADD COLUMN IF NOT EXISTS linkedin_url TEXT;

ALTER TABLE pending_recruiters
  ADD COLUMN IF NOT EXISTS linkedin_url TEXT;

UPDATE pending_recruiters pr
SET linkedin_url = u.linkedin_url
FROM users u
WHERE pr.user_id = u.id
  AND pr.linkedin_url IS NULL
  AND u.linkedin_url IS NOT NULL;

ALTER TABLE player_profiles
  ADD COLUMN IF NOT EXISTS is_with_team BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN IF NOT EXISTS team_name TEXT;

ALTER TABLE player_profiles
  DROP CONSTRAINT IF EXISTS chk_player_profiles_team_affiliation;

ALTER TABLE player_profiles
  ADD CONSTRAINT chk_player_profiles_team_affiliation CHECK (
    (is_with_team = FALSE AND team_name IS NULL)
    OR
    (is_with_team = TRUE AND team_name IS NOT NULL AND btrim(team_name) <> '')
  );
