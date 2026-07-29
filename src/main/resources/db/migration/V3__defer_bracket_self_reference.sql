-- A bracket match may point to a later match in the same table. Deferring the
-- self-reference lets a full bracket be inserted atomically during imports.
ALTER TABLE matches
    DROP CONSTRAINT matches_next_match_id_fkey,
    ADD CONSTRAINT matches_next_match_id_fkey
        FOREIGN KEY (next_match_id)
        REFERENCES matches (id)
        DEFERRABLE INITIALLY DEFERRED;
