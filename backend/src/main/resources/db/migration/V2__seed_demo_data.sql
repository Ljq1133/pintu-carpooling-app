-- Demo data uses stable IDs so the frontend HTTP adapter and screenshots remain reproducible.

-- 演示用户：1 乘客 + 1 司机 + 1 管理员
INSERT IGNORE INTO `user` (`id`, `username`, `password`, `name`, `phone`, `role`, `credit`, `status`, `trip_count`, `saved_money`, `carbon_saved`)
VALUES
    ('passenger-001', 'passenger_demo', '123456', '赵同学', '138 **** 5678', 'passenger', 98, '正常', 32, 268.50, 16.20),
    ('driver-001',    'driver_demo',    '123456', '陈师傅', '138 **** 2678', 'driver',    96, '正常', 128, 0.00, 0.00),
    ('admin-001',     'admin_demo',     '123456', '平台管理员', '021 **** 0001', 'admin',    100, '正常', 0, 0.00, 0.00);

-- 演示车辆
INSERT IGNORE INTO `vehicle` (`id`, `driver_id`, `plate`, `model`, `color`, `seats`, `status`)
VALUES
    ('veh-001', 'driver-001', '沪A·P2T08', '比亚迪 秦 PLUS', '白色', 3, '已认证');

-- 演示司机行程
INSERT IGNORE INTO `driver_trip` (`id`, `driver_id`, `vehicle_id`, `start_location`, `end_location`, `departure_time`, `available_seats`, `price`, `overlap`, `status`)
VALUES
    ('trip-001', 'driver-001', 'veh-001', '同济科技园', 'SOHO',     '2026-06-13 08:30', 3, 18.60, 92, '已发布'),
    ('trip-002', 'driver-001', 'veh-001', '同济科技园', '五角场',   '2026-06-13 09:00', 2, 22.00, 78, '已发布'),
    ('trip-003', 'driver-001', 'veh-001', '中央科技园', '陆家嘴',   '2026-06-13 09:30', 3, 28.80, 85, '已发布');

-- 演示审核记录
INSERT IGNORE INTO `audit_record` (`id`, `target_type`, `target_id`, `name`, `plate`, `status`)
VALUES
    ('audit-user-001',    'user',    'driver-001', '陈师傅',    NULL,         '已通过'),
    ('audit-vehicle-001', 'vehicle', 'veh-001',    '比亚迪 秦 PLUS', '沪A·P2T08', '已通过');
