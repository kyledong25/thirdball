ALTER TABLE players DROP CONSTRAINT IF EXISTS players_rating_check;
ALTER TABLE players ADD CONSTRAINT players_rating_nonnegative_check CHECK (rating >= 0);
