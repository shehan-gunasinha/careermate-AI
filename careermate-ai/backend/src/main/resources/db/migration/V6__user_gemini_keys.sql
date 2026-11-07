-- Per-user Gemini API keys. Values are encrypted by the backend with AES-GCM; the hint is a masked suffix only.

ALTER TABLE users ADD COLUMN gemini_api_key_encrypted TEXT;
ALTER TABLE users ADD COLUMN gemini_api_key_hint VARCHAR(20);
ALTER TABLE users ADD COLUMN gemini_api_key_updated_at TIMESTAMPTZ;
