-- Ratings must be able to cross zero so every established-player result
-- applies the exact USATT exchange and remains zero-sum.
ALTER TABLE players
    DROP CONSTRAINT IF EXISTS players_rating_nonnegative_check;
