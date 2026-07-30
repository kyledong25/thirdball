CREATE TABLE member_match_result_proposals (
    id BIGSERIAL PRIMARY KEY,
    reporter_player_id BIGINT NOT NULL REFERENCES players(id),
    opponent_player_id BIGINT NOT NULL REFERENCES players(id),
    reporter_score INTEGER NOT NULL CHECK (reporter_score >= 0),
    opponent_score INTEGER NOT NULL CHECK (opponent_score >= 0),
    status VARCHAR(20) NOT NULL,
    official_match_id BIGINT UNIQUE REFERENCES matches(id),
    proposed_at TIMESTAMPTZ NOT NULL,
    responded_at TIMESTAMPTZ,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (reporter_player_id <> opponent_player_id),
    CHECK (reporter_score <> opponent_score)
);

CREATE INDEX idx_member_match_result_proposals_opponent
    ON member_match_result_proposals(opponent_player_id, status, proposed_at DESC);
CREATE INDEX idx_member_match_result_proposals_reporter
    ON member_match_result_proposals(reporter_player_id, status, proposed_at DESC);
