-- Badge catalogue. Unlock rules live in AchievementService.
INSERT INTO achievements (code, name, description, icon, xp_reward, sort_order) VALUES
    ('FIRST_VERDICT', 'First Verdict',  'Complete your first full review.',                              'gavel',        25, 1),
    ('CLEAN_RECORD',  'Clean Record',   'Score 90+ with zero proven bugs.',                              'shield-check', 50, 2),
    ('SPEED_DEMON',   'Speed Demon',    'Match the optimal solution''s speed at the largest input.',     'zap',          40, 3),
    ('COMEBACK',      'Comeback',       'Improve a problem by 30+ points over your first attempt.',      'trending-up',  40, 4),
    ('EDGE_LORD',     'Edge Lord',      'Pass every edge-case hidden test (at least 3) with no proven edge-case charge.', 'swords', 40, 5),
    ('STREAK_7',      '7-Day Streak',   'Practise 7 days in a row.',                                     'flame',        50, 6),
    ('STREAK_30',     '30-Day Streak',  'Practise 30 days in a row.',                                    'flame-kindling', 150, 7),
    ('TOPIC_MASTER',  'Topic Master',   'Average 85+ over at least 3 reviews in one topic.',             'crown',        75, 8),
    ('CASELOAD_10',   'Caseload: 10',   'Complete 10 full reviews.',                                     'briefcase',    30, 9),
    ('CASELOAD_50',   'Caseload: 50',   'Complete 50 full reviews.',                                     'landmark',    100, 10);
