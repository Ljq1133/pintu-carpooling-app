-- Backfill legacy demo rows before making route coordinates mandatory.
UPDATE `passenger_order`
SET `start_lng` = COALESCE(`start_lng`, 121.5063770),
    `start_lat` = COALESCE(`start_lat`, 31.2820800),
    `end_lng` = COALESCE(`end_lng`, 121.4896200),
    `end_lat` = COALESCE(`end_lat`, 31.2582100);

UPDATE `driver_trip`
SET `start_lng` = COALESCE(`start_lng`, 121.5063770),
    `start_lat` = COALESCE(`start_lat`, 31.2820800),
    `end_lng` = COALESCE(`end_lng`, 121.4896200),
    `end_lat` = COALESCE(`end_lat`, 31.2582100);

ALTER TABLE `passenger_order`
    MODIFY COLUMN `start_lng` DECIMAL(10, 7) NOT NULL;
ALTER TABLE `passenger_order`
    MODIFY COLUMN `start_lat` DECIMAL(10, 7) NOT NULL;
ALTER TABLE `passenger_order`
    MODIFY COLUMN `end_lng` DECIMAL(10, 7) NOT NULL;
ALTER TABLE `passenger_order`
    MODIFY COLUMN `end_lat` DECIMAL(10, 7) NOT NULL;

ALTER TABLE `driver_trip`
    MODIFY COLUMN `start_lng` DECIMAL(10, 7) NOT NULL;
ALTER TABLE `driver_trip`
    MODIFY COLUMN `start_lat` DECIMAL(10, 7) NOT NULL;
ALTER TABLE `driver_trip`
    MODIFY COLUMN `end_lng` DECIMAL(10, 7) NOT NULL;
ALTER TABLE `driver_trip`
    MODIFY COLUMN `end_lat` DECIMAL(10, 7) NOT NULL;

CREATE TABLE IF NOT EXISTS `user_address` (
    `id` VARCHAR(64) PRIMARY KEY,
    `user_id` VARCHAR(32) NOT NULL,
    `address_type` VARCHAR(16) NOT NULL COMMENT 'HISTORY/FAVORITE',
    `label` VARCHAR(64) NULL,
    `name` VARCHAR(128) NOT NULL,
    `address` VARCHAR(255) NOT NULL,
    `lng` DECIMAL(10, 7) NOT NULL,
    `lat` DECIMAL(10, 7) NOT NULL,
    `last_used_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT NOT NULL DEFAULT 0,
    CONSTRAINT `fk_user_address_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    UNIQUE KEY `uk_user_address_type_coordinate` (`user_id`, `address_type`, `lng`, `lat`),
    KEY `idx_user_address_recent` (`user_id`, `address_type`, `last_used_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
