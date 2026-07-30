ALTER TABLE players
    ADD COLUMN graduation_year INTEGER CHECK (graduation_year BETWEEN 2000 AND 2100),
    ADD COLUMN skill_level VARCHAR(30),
    ADD COLUMN phone VARCHAR(30),
    ADD COLUMN dues_paid BOOLEAN NOT NULL DEFAULT FALSE;

-- Generated brackets create future-round placeholders that receive a winner
-- only after both feeder matches are decided.
ALTER TABLE matches
    ALTER COLUMN player_one_id DROP NOT NULL,
    ALTER COLUMN player_two_id DROP NOT NULL;
