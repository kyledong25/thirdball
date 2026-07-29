CREATE TABLE players (
    id BIGSERIAL PRIMARY KEY,
    display_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    rating INTEGER NOT NULL DEFAULT 1200 CHECK (rating > 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE tournaments (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    location VARCHAR(200),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    max_participants INTEGER NOT NULL CHECK (max_participants >= 2),
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (ends_at > starts_at)
);

CREATE TABLE tournament_players (
    tournament_id BIGINT NOT NULL REFERENCES tournaments(id) ON DELETE CASCADE,
    player_id BIGINT NOT NULL REFERENCES players(id),
    PRIMARY KEY (tournament_id, player_id)
);

CREATE TABLE matches (
    id BIGSERIAL PRIMARY KEY,
    tournament_id BIGINT REFERENCES tournaments(id) ON DELETE CASCADE,
    player_one_id BIGINT NOT NULL REFERENCES players(id),
    player_two_id BIGINT NOT NULL REFERENCES players(id),
    winner_id BIGINT REFERENCES players(id),
    player_one_score INTEGER,
    player_two_score INTEGER,
    player_one_rating_before INTEGER,
    player_one_rating_after INTEGER,
    player_two_rating_before INTEGER,
    player_two_rating_after INTEGER,
    round_number INTEGER NOT NULL DEFAULT 1 CHECK (round_number >= 1),
    bracket_slot INTEGER,
    next_match_id BIGINT REFERENCES matches(id),
    next_match_player_slot INTEGER CHECK (next_match_player_slot IN (1, 2)),
    status VARCHAR(30) NOT NULL,
    completed_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (player_one_id <> player_two_id),
    CHECK ((player_one_score IS NULL AND player_two_score IS NULL) OR
           (player_one_score >= 0 AND player_two_score >= 0))
);

CREATE INDEX idx_matches_tournament_round ON matches(tournament_id, round_number, bracket_slot);
CREATE INDEX idx_matches_players ON matches(player_one_id, player_two_id);

CREATE TABLE practice_sessions (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    location VARCHAR(200) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,
    registration_deadline TIMESTAMPTZ,
    capacity INTEGER NOT NULL CHECK (capacity >= 1),
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (ends_at > starts_at)
);

CREATE TABLE practice_session_registrations (
    practice_session_id BIGINT NOT NULL REFERENCES practice_sessions(id) ON DELETE CASCADE,
    player_id BIGINT NOT NULL REFERENCES players(id),
    PRIMARY KEY (practice_session_id, player_id)
);
