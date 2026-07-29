-- Third Ball demo data. Safe to run more than once: existing players,
-- tournaments, registrations, matches, and practice blocks are preserved.
BEGIN;

INSERT INTO players (display_name, email, rating, active, created_at, updated_at, version) VALUES
    ('Avery Kim', 'avery.kim@thirdball.demo', 1682, TRUE, NOW(), NOW(), 0),
    ('Rohan Shah', 'rohan.shah@thirdball.demo', 1638, TRUE, NOW(), NOW(), 0),
    ('Maya Chen', 'maya.chen@thirdball.demo', 1609, TRUE, NOW(), NOW(), 0),
    ('Diego Alvarez', 'diego.alvarez@thirdball.demo', 1584, TRUE, NOW(), NOW(), 0),
    ('Jordan Patel', 'jordan.patel@thirdball.demo', 1561, TRUE, NOW(), NOW(), 0),
    ('Noah Williams', 'noah.williams@thirdball.demo', 1532, TRUE, NOW(), NOW(), 0),
    ('Sofia Rodriguez', 'sofia.rodriguez@thirdball.demo', 1518, TRUE, NOW(), NOW(), 0),
    ('Ethan Park', 'ethan.park@thirdball.demo', 1496, TRUE, NOW(), NOW(), 0),
    ('Priya Nair', 'priya.nair@thirdball.demo', 1477, TRUE, NOW(), NOW(), 0),
    ('Lucas Martin', 'lucas.martin@thirdball.demo', 1453, TRUE, NOW(), NOW(), 0),
    ('Hannah Brooks', 'hannah.brooks@thirdball.demo', 1436, TRUE, NOW(), NOW(), 0),
    ('Owen Murphy', 'owen.murphy@thirdball.demo', 1412, TRUE, NOW(), NOW(), 0),
    ('Leah Thompson', 'leah.thompson@thirdball.demo', 1398, TRUE, NOW(), NOW(), 0),
    ('Marcus Reed', 'marcus.reed@thirdball.demo', 1371, TRUE, NOW(), NOW(), 0),
    ('Zoe Foster', 'zoe.foster@thirdball.demo', 1355, TRUE, NOW(), NOW(), 0),
    ('Caleb Wright', 'caleb.wright@thirdball.demo', 1337, TRUE, NOW(), NOW(), 0),
    ('Nina Desai', 'nina.desai@thirdball.demo', 1311, TRUE, NOW(), NOW(), 0),
    ('Isaac Green', 'isaac.green@thirdball.demo', 1289, TRUE, NOW(), NOW(), 0),
    ('Grace Liu', 'grace.liu@thirdball.demo', 1258, TRUE, NOW(), NOW(), 0),
    ('Ben Carter', 'ben.carter@thirdball.demo', 1229, TRUE, NOW(), NOW(), 0),
    ('Elena Morales', 'elena.morales@thirdball.demo', 1194, TRUE, NOW(), NOW(), 0),
    ('Theo Jackson', 'theo.jackson@thirdball.demo', 1168, TRUE, NOW(), NOW(), 0),
    ('Amira Hassan', 'amira.hassan@thirdball.demo', 1134, TRUE, NOW(), NOW(), 0),
    ('Sam Walker', 'sam.walker@thirdball.demo', 1089, TRUE, NOW(), NOW(), 0),
    ('Claire Nguyen', 'claire.nguyen@thirdball.demo', 1052, TRUE, NOW(), NOW(), 0),
    ('Miles Cooper', 'miles.cooper@thirdball.demo', 1018, TRUE, NOW(), NOW(), 0)
ON CONFLICT (email) DO NOTHING;

INSERT INTO tournaments (name, description, location, starts_at, ends_at, max_participants, status, created_at)
SELECT 'Winter Championship 2026', 'A completed club singles championship.', 'Student Recreation Center', '2026-01-24 10:00:00-06', '2026-01-24 18:00:00-06', 16, 'COMPLETED', NOW()
WHERE NOT EXISTS (SELECT 1 FROM tournaments WHERE name = 'Winter Championship 2026');

INSERT INTO tournaments (name, description, location, starts_at, ends_at, max_participants, status, created_at)
SELECT 'Spring Smash 2026', 'A completed spring singles event.', 'Student Recreation Center', '2026-04-11 10:00:00-05', '2026-04-11 18:00:00-05', 16, 'COMPLETED', NOW()
WHERE NOT EXISTS (SELECT 1 FROM tournaments WHERE name = 'Spring Smash 2026');

INSERT INTO tournaments (name, description, location, starts_at, ends_at, max_participants, status, created_at)
SELECT 'Fall Open 2026', 'Open singles tournament. Best of five games throughout.', 'Rec Center Court A', '2026-09-19 09:00:00-05', '2026-09-19 19:00:00-05', 32, 'REGISTRATION_OPEN', NOW()
WHERE NOT EXISTS (SELECT 1 FROM tournaments WHERE name = 'Fall Open 2026');

INSERT INTO tournament_players (tournament_id, player_id)
SELECT tournament.id, player.id
FROM tournaments tournament
JOIN players player ON player.email IN (
    'avery.kim@thirdball.demo', 'rohan.shah@thirdball.demo', 'maya.chen@thirdball.demo', 'diego.alvarez@thirdball.demo',
    'jordan.patel@thirdball.demo', 'noah.williams@thirdball.demo', 'sofia.rodriguez@thirdball.demo', 'ethan.park@thirdball.demo'
)
WHERE tournament.name = 'Winter Championship 2026'
ON CONFLICT DO NOTHING;

INSERT INTO tournament_players (tournament_id, player_id)
SELECT tournament.id, player.id
FROM tournaments tournament
JOIN players player ON player.email IN (
    'maya.chen@thirdball.demo', 'jordan.patel@thirdball.demo', 'sofia.rodriguez@thirdball.demo', 'priya.nair@thirdball.demo',
    'hannah.brooks@thirdball.demo', 'leah.thompson@thirdball.demo', 'zoe.foster@thirdball.demo', 'nina.desai@thirdball.demo'
)
WHERE tournament.name = 'Spring Smash 2026'
ON CONFLICT DO NOTHING;

INSERT INTO tournament_players (tournament_id, player_id)
SELECT tournament.id, player.id
FROM tournaments tournament
JOIN players player ON player.email LIKE '%@thirdball.demo'
WHERE tournament.name = 'Fall Open 2026'
ON CONFLICT DO NOTHING;

-- A few completed matches make the completed-event history and bracket visual meaningful.
INSERT INTO matches (tournament_id, player_one_id, player_two_id, winner_id, player_one_score, player_two_score,
                     player_one_rating_before, player_one_rating_after, player_two_rating_before, player_two_rating_after,
                     round_number, bracket_slot, status, completed_at, version)
SELECT tournament.id, first_player.id, second_player.id, first_player.id, 3, 1, 1666, 1682, 1554, 1538, 1, 1, 'COMPLETED', '2026-01-24 11:05:00-06', 0
FROM tournaments tournament
JOIN players first_player ON first_player.email = 'avery.kim@thirdball.demo'
JOIN players second_player ON second_player.email = 'jordan.patel@thirdball.demo'
WHERE tournament.name = 'Winter Championship 2026'
  AND NOT EXISTS (SELECT 1 FROM matches existing WHERE existing.tournament_id = tournament.id AND existing.round_number = 1 AND existing.bracket_slot = 1);

INSERT INTO matches (tournament_id, player_one_id, player_two_id, winner_id, player_one_score, player_two_score,
                     player_one_rating_before, player_one_rating_after, player_two_rating_before, player_two_rating_after,
                     round_number, bracket_slot, status, completed_at, version)
SELECT tournament.id, first_player.id, second_player.id, first_player.id, 3, 2, 1593, 1609, 1624, 1608, 1, 2, 'COMPLETED', '2026-01-24 11:05:00-06', 0
FROM tournaments tournament
JOIN players first_player ON first_player.email = 'maya.chen@thirdball.demo'
JOIN players second_player ON second_player.email = 'rohan.shah@thirdball.demo'
WHERE tournament.name = 'Winter Championship 2026'
  AND NOT EXISTS (SELECT 1 FROM matches existing WHERE existing.tournament_id = tournament.id AND existing.round_number = 1 AND existing.bracket_slot = 2);

INSERT INTO matches (tournament_id, player_one_id, player_two_id, winner_id, player_one_score, player_two_score,
                     player_one_rating_before, player_one_rating_after, player_two_rating_before, player_two_rating_after,
                     round_number, bracket_slot, status, completed_at, version)
SELECT tournament.id, first_player.id, second_player.id, first_player.id, 3, 0, 1592, 1609, 1460, 1443, 1, 1, 'COMPLETED', '2026-04-11 11:20:00-05', 0
FROM tournaments tournament
JOIN players first_player ON first_player.email = 'maya.chen@thirdball.demo'
JOIN players second_player ON second_player.email = 'hannah.brooks@thirdball.demo'
WHERE tournament.name = 'Spring Smash 2026'
  AND NOT EXISTS (SELECT 1 FROM matches existing WHERE existing.tournament_id = tournament.id AND existing.round_number = 1 AND existing.bracket_slot = 1);

INSERT INTO practice_sessions (title, description, location, starts_at, ends_at, registration_deadline, capacity, created_at)
SELECT 'Wednesday Open Practice', 'Open tables, casual ladders, and match play for all levels.', 'Rec Center Court A', '2026-08-05 18:00:00-05', '2026-08-05 21:00:00-05', '2026-08-05 17:00:00-05', 48, NOW()
WHERE NOT EXISTS (SELECT 1 FROM practice_sessions WHERE title = 'Wednesday Open Practice' AND starts_at = '2026-08-05 18:00:00-05');

INSERT INTO practice_sessions (title, description, location, starts_at, ends_at, registration_deadline, capacity, created_at)
SELECT 'Advanced Footwork Clinic', 'Small-group drills focused on recovery, placement, and third-ball attack patterns.', 'Rec Center Court B', '2026-08-12 18:30:00-05', '2026-08-12 20:30:00-05', '2026-08-12 16:00:00-05', 18, NOW()
WHERE NOT EXISTS (SELECT 1 FROM practice_sessions WHERE title = 'Advanced Footwork Clinic' AND starts_at = '2026-08-12 18:30:00-05');

INSERT INTO practice_sessions (title, description, location, starts_at, ends_at, registration_deadline, capacity, created_at)
SELECT 'New Member Welcome Week', 'A multi-day orientation block with equipment setup, coaching, and doubles round robins.', 'Student Recreation Center', '2026-08-24 17:00:00-05', '2026-08-27 20:00:00-05', '2026-08-23 23:00:00-05', 60, NOW()
WHERE NOT EXISTS (SELECT 1 FROM practice_sessions WHERE title = 'New Member Welcome Week' AND starts_at = '2026-08-24 17:00:00-05');

INSERT INTO practice_session_registrations (practice_session_id, player_id)
SELECT session.id, player.id
FROM practice_sessions session
JOIN players player ON player.email IN (
    'avery.kim@thirdball.demo', 'maya.chen@thirdball.demo', 'jordan.patel@thirdball.demo', 'priya.nair@thirdball.demo',
    'leah.thompson@thirdball.demo', 'nina.desai@thirdball.demo', 'grace.liu@thirdball.demo', 'ben.carter@thirdball.demo'
)
WHERE session.title = 'Wednesday Open Practice'
ON CONFLICT DO NOTHING;

INSERT INTO practice_session_registrations (practice_session_id, player_id)
SELECT session.id, player.id
FROM practice_sessions session
JOIN players player ON player.email IN (
    'avery.kim@thirdball.demo', 'rohan.shah@thirdball.demo', 'maya.chen@thirdball.demo', 'diego.alvarez@thirdball.demo',
    'jordan.patel@thirdball.demo', 'noah.williams@thirdball.demo'
)
WHERE session.title = 'Advanced Footwork Clinic'
ON CONFLICT DO NOTHING;

COMMIT;
