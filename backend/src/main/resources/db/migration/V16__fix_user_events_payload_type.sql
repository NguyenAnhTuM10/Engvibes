ALTER TABLE user_events ALTER COLUMN payload TYPE TEXT USING payload::TEXT;
