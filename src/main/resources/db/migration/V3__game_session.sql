CREATE TABLE gamesession (
    id BIGSERIAL PRIMARY KEY,
    join_code VARCHAR(5) NOT NULL,
    name VARCHAR(100) NOT NULL,
    admin_user_id VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    games_started_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_gamesession_join_code UNIQUE (join_code)
);

CREATE TABLE sessionmember (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES gamesession (id),
    user_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(30),
    role VARCHAR(255),
    score INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT uk_sessionmember_session_user UNIQUE (session_id, user_id)
);

CREATE INDEX idx_sessionmember_session_id ON sessionmember (session_id);

ALTER TABLE game ADD COLUMN session_id BIGINT REFERENCES gamesession (id) ON DELETE SET NULL;
CREATE INDEX idx_game_session_id ON game (session_id);

ALTER TABLE gamesession ADD COLUMN current_game_id BIGINT REFERENCES game (id) ON DELETE SET NULL;
