-- ============================================================
-- PinTu 演示数据重置脚本
-- 执行时机：完整演示前手工执行；不会被应用启动自动运行
-- ============================================================
-- 使用方式：
--   docker compose exec mysql mysql -uroot -p$DB_PASSWORD pintu < scripts/reset-demo-data.sql
--   或本地：mysql -uroot -p pintu < scripts/reset-demo-data.sql
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清除业务数据（保留账号 / 车辆 / 演示行程）
TRUNCATE TABLE `payment`;
TRUNCATE TABLE `review`;
TRUNCATE TABLE `order_status_history`;
TRUNCATE TABLE `order_record`;
TRUNCATE TABLE `match_request`;
TRUNCATE TABLE `passenger_order`;
TRUNCATE TABLE `notification`;
TRUNCATE TABLE `chat_message`;
TRUNCATE TABLE `conversation`;
TRUNCATE TABLE `negotiation_proposal`;
TRUNCATE TABLE `proposal_confirmation`;

-- 重置演示账号信用与状态
UPDATE `user` SET credit = 98, status = '正常' WHERE id = 'passenger-001';
UPDATE `user` SET credit = 96, status = '正常' WHERE id = 'driver-001';

-- 重置演示行程剩余座位
UPDATE `driver_trip` SET available_seats = 3, status = '已发布' WHERE id IN ('trip-001', 'trip-003');
UPDATE `driver_trip` SET available_seats = 2, status = '已发布' WHERE id = 'trip-002';

SET FOREIGN_KEY_CHECKS = 1;

-- 输出确认
SELECT '演示数据已重置' AS message;
