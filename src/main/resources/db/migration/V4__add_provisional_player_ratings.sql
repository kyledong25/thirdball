-- Existing players were already assigned club ratings before provisional
-- initialization was introduced. New JPA-created players explicitly begin
-- unrated and are marked established after five rated-opponent results.
ALTER TABLE players
    ADD COLUMN rating_established BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE players
    ADD COLUMN provisional_match_count INTEGER NOT NULL DEFAULT 0
        CHECK (provisional_match_count >= 0);
