ALTER TABLE user_profiles ADD COLUMN avatar_data BYTEA;
ALTER TABLE user_profiles ADD COLUMN avatar_content_type VARCHAR(100);
ALTER TABLE user_profiles ADD COLUMN avatar_updated_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE user_profiles DROP COLUMN avatar_path;
