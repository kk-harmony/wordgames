CREATE SCHEMA IF NOT EXISTS wrdgm;

CREATE TABLE secretword (
    id BIGSERIAL PRIMARY KEY,
    authentic VARCHAR(50) NOT NULL,
    imposed VARCHAR(50) NOT NULL
);

CREATE TABLE game (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    admin_user_id VARCHAR(255) NOT NULL,
    status VARCHAR(255),
    secret_word_id BIGINT REFERENCES secretword (id),
    impostor_user_id VARCHAR(255),
    outcome VARCHAR(255),
    current_round INTEGER NOT NULL DEFAULT 0,
    vote_reset_count INTEGER NOT NULL DEFAULT 0,
    current_turn_user_id VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE gamemember (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL REFERENCES game (id),
    user_id VARCHAR(255) NOT NULL,
    display_name VARCHAR(30),
    role VARCHAR(255),
    assigned_word VARCHAR(255),
    voted_for_user_id VARCHAR(255),
    turn_completed BOOLEAN NOT NULL DEFAULT FALSE,
    eliminated BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_gamemember_game_user UNIQUE (game_id, user_id)
);

CREATE INDEX idx_gamemember_game_id ON gamemember (game_id);

CREATE TABLE idempotency_record (
    id BIGSERIAL PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL,
    user_id VARCHAR(255) NOT NULL,
    operation VARCHAR(50) NOT NULL,
    game_id BIGINT NOT NULL,
    request_hash VARCHAR(255) NOT NULL,
    response_status INTEGER NOT NULL,
    response_body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_idempotency_user_key UNIQUE (user_id, idempotency_key)
);
