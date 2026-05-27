-- ============================================================
-- WeChat Room Booking System — Seed Data (Development)
-- ============================================================

-- -----------------------------------------------------------
-- 1 organization
-- -----------------------------------------------------------
INSERT INTO organization (id, name, contact, phone, address, status)
VALUES (1, '光谷创新中心', '张经理', '027-88886666', '武汉市洪山区光谷大道1号', 'ACTIVE');
SELECT setval('organization_id_seq', 1, true);

-- -----------------------------------------------------------
-- 2 venues
-- -----------------------------------------------------------
INSERT INTO venue (id, organization_id, name, address, phone, status)
VALUES
  (1, 1, '光谷总店', '武汉市洪山区关山大道1号', '027-88880001', 'ACTIVE'),
  (2, 1, '汉口分店', '武汉市江汉区解放大道100号', '027-88880002', 'ACTIVE');
SELECT setval('venue_id_seq', 2, true);

-- -----------------------------------------------------------
-- 5 rooms
-- -----------------------------------------------------------
INSERT INTO room (id, venue_id, name, description, capacity, area_sqm, floor,
                  price_per_hour, price_per_halfday, price_per_day, status, sort_order)
VALUES
  (1, 1, '会议室A', '小型讨论室，适合6人团队',      6,  15.0,  2, 30.00,  120.00,  200.00, 'AVAILABLE', 1),
  (2, 1, '会议室B', '中型会议室，可容纳10人',      10,  30.0,  3, 50.00,  200.00,  350.00, 'AVAILABLE', 2),
  (3, 1, '路演厅',  '大型路演厅，配投影与音响',    30,  80.0,  1, 120.00, 500.00,  900.00, 'AVAILABLE', 3),
  (4, 2, '汉口小会议室', '适合6-8人讨论',          8,  18.0,  5, 40.00,  150.00,  260.00, 'AVAILABLE', 1),
  (5, 2, '汉口多功能厅', '可容纳20人，配视频会议', 20,  55.0,  6, 90.00,  380.00,  650.00, 'AVAILABLE', 2);
SELECT setval('room_id_seq', 5, true);

-- -----------------------------------------------------------
-- 6 amenities
-- -----------------------------------------------------------
INSERT INTO amenity (id, name, icon, category, sort_order)
VALUES
  (1, 'Projector',       'projector',     'AV', 1),
  (2, 'Whiteboard',      'whiteboard',    'Furniture', 2),
  (3, 'Video Conference', 'video-camera', 'AV', 3),
  (4, 'WiFi',            'wifi',          'Network', 4),
  (5, 'Coffee Machine',  'coffee',        'Beverage', 5),
  (6, 'Sound System',    'speaker',       'AV', 6);
SELECT setval('amenity_id_seq', 6, true);

-- -----------------------------------------------------------
-- Map amenities to rooms (room_amenity junction)
-- -----------------------------------------------------------
-- Room 1 (会议室A): Projector, Whiteboard, WiFi
INSERT INTO room_amenity (room_id, amenity_id) VALUES (1, 1), (1, 2), (1, 4);

-- Room 2 (会议室B): Projector, Whiteboard, Video Conference, WiFi
INSERT INTO room_amenity (room_id, amenity_id) VALUES (2, 1), (2, 2), (2, 3), (2, 4);

-- Room 3 (路演厅): Projector, Whiteboard, Video Conference, WiFi, Sound System
INSERT INTO room_amenity (room_id, amenity_id) VALUES (3, 1), (3, 2), (3, 3), (3, 4), (3, 6);

-- Room 4 (汉口小会议室): Whiteboard, WiFi, Coffee Machine
INSERT INTO room_amenity (room_id, amenity_id) VALUES (4, 2), (4, 4), (4, 5);

-- Room 5 (汉口多功能厅): Projector, Whiteboard, Video Conference, WiFi, Coffee Machine, Sound System
INSERT INTO room_amenity (room_id, amenity_id) VALUES (5, 1), (5, 2), (5, 3), (5, 4), (5, 5), (5, 6);

-- -----------------------------------------------------------
-- 1 availability_rule per room (weekdays 8:00-22:00, Mon-Fri)
-- -----------------------------------------------------------
INSERT INTO availability_rule (room_id, day_of_week, open_time, close_time, is_active)
SELECT r.id, d.day, '08:00'::TIME, '22:00'::TIME, true
FROM room r
CROSS JOIN (VALUES (1),(2),(3),(4),(5)) AS d(day);
