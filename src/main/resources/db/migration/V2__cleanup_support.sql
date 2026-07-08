ALTER TABLE game ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT NOW();

CREATE INDEX idx_game_status_created_at ON game (status, created_at);
CREATE INDEX idx_idempotency_record_created_at ON idempotency_record (created_at);
CREATE INDEX idx_idempotency_record_game_id ON idempotency_record (game_id);
