ALTER TABLE `match_request`
    ADD COLUMN `initiator_role` VARCHAR(16) NOT NULL DEFAULT 'PASSENGER' COMMENT 'PASSENGER/DRIVER';
ALTER TABLE `match_request`
    ADD COLUMN `expires_at` DATETIME NULL;

CREATE INDEX `idx_match_request_expiry`
    ON `match_request` (`status`, `expires_at`);
CREATE INDEX `idx_passenger_order_geo_candidate`
    ON `passenger_order` (`status`, `departure_time`, `passenger_count`, `start_lng`, `start_lat`);
CREATE INDEX `idx_driver_trip_geo_candidate`
    ON `driver_trip` (`status`, `departure_time`, `available_seats`, `start_lng`, `start_lat`);
