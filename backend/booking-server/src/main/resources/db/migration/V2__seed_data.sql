-- ============================================================
-- Flyway Migration V2 — Seed Development Data
-- ============================================================

-- organization
INSERT INTO organization (id, name, contact, phone, address, status)
VALUES (1, '光谷创新中心', '张经理', '027-88886666', '武汉市洪山区光谷大道1号', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;
SELECT setval('organization_id_seq', GREATEST(1, (SELECT max(id) FROM organization)), true);

-- venues
INSERT INTO venue (id, organization_id, name, address, phone, status)
VALUES
  (1, 1, '光谷总店', '武汉市洪山区关山大道1号', '027-88880001', 'ACTIVE'),
  (2, 1, '汉口分店', '武汉市江汉区解放大道100号', '027-88880002', 'ACTIVE')
ON CONFLICT (id) DO NOTHING;
SELECT setval('venue_id_seq', GREATEST(2, (SELECT max(id) FROM venue)), true);

-- rooms
INSERT INTO room (id, venue_id, name, description, capacity, area_sqm, floor,
                  price_per_hour, price_per_halfday, price_per_day, status, sort_order)
VALUES
  (1, 1, '会议室A', '小型讨论室，适合6人团队',      6,  15.0,  2, 30.00,  120.00,  200.00, 'AVAILABLE', 1),
  (2, 1, '会议室B', '中型会议室，可容纳10人',      10,  30.0,  3, 50.00,  200.00,  350.00, 'AVAILABLE', 2),
  (3, 1, '路演厅',  '大型路演厅，配投影与音响',    30,  80.0,  1, 120.00, 500.00,  900.00, 'AVAILABLE', 3),
  (4, 2, '汉口小会议室', '适合6-8人讨论',          8,  18.0,  5, 40.00,  150.00,  260.00, 'AVAILABLE', 1),
  (5, 2, '汉口多功能厅', '可容纳20人，配视频会议', 20,  55.0,  6, 90.00,  380.00,  650.00, 'AVAILABLE', 2)
ON CONFLICT (id) DO NOTHING;
SELECT setval('room_id_seq', GREATEST(5, (SELECT max(id) FROM room)), true);

-- amenities
INSERT INTO amenity (id, name, icon, category, sort_order)
VALUES
  (1, 'Projector',       'projector',     'AV', 1),
  (2, 'Whiteboard',      'whiteboard',    'Furniture', 2),
  (3, 'Video Conference', 'video-camera', 'AV', 3),
  (4, 'WiFi',            'wifi',          'Network', 4),
  (5, 'Coffee Machine',  'coffee',        'Beverage', 5),
  (6, 'Sound System',    'speaker',       'AV', 6)
ON CONFLICT (id) DO NOTHING;
SELECT setval('amenity_id_seq', GREATEST(6, (SELECT max(id) FROM amenity)), true);

-- room_amenity mappings (INSERT only if not exists, using ON CONFLICT on (room_id, amenity_id))
INSERT INTO room_amenity (room_id, amenity_id) VALUES
  (1, 1), (1, 2), (1, 4),
  (2, 1), (2, 2), (2, 3), (2, 4),
  (3, 1), (3, 2), (3, 3), (3, 4), (3, 6),
  (4, 2), (4, 4), (4, 5),
  (5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 6)
ON CONFLICT (room_id, amenity_id) DO NOTHING;
SELECT setval('room_amenity_id_seq', GREATEST(21, (SELECT max(id) FROM room_amenity)), true);

-- availability_rules (8:00-22:00 Mon-Fri for all rooms)
INSERT INTO availability_rule (room_id, day_of_week, open_time, close_time, is_active)
SELECT r.id, d.day, '08:00'::TIME, '22:00'::TIME, true
FROM room r
CROSS JOIN (VALUES (1),(2),(3),(4),(5)) AS d(day)
ON CONFLICT (room_id, day_of_week) DO NOTHING;
SELECT setval('availability_rule_id_seq',
  GREATEST((SELECT max(id) FROM availability_rule), 25), true);
