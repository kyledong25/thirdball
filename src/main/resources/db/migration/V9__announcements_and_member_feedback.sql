CREATE TABLE announcements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    body TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT TRUE,
    published_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_announcements_published_at
    ON announcements(published, published_at DESC, id DESC);

CREATE TABLE member_feedback (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL REFERENCES players(id),
    subject VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_member_feedback_submitted_at
    ON member_feedback(submitted_at DESC, id DESC);
