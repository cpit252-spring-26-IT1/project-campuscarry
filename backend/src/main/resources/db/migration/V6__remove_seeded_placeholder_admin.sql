-- Remove the legacy placeholder admin account seeded by older baselines.
DELETE FROM users
WHERE role = 'ADMIN'
  AND email = 'admin@certifiedcarry.sa'
  AND firebase_uid = 'MANUAL_ADMIN_LINK_REQUIRED'
  AND registration_source = 'SYSTEM_SEED';
