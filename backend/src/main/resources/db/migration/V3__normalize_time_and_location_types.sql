-- Normalize the core route fields to the report's DateTime and coordinate model.

ALTER TABLE `passenger_order`
    MODIFY COLUMN `departure_time` DATETIME NOT NULL;
ALTER TABLE `passenger_order`
    ADD COLUMN `start_lng` DECIMAL(10, 7) NULL COMMENT '起点经度';
ALTER TABLE `passenger_order`
    ADD COLUMN `start_lat` DECIMAL(10, 7) NULL COMMENT '起点纬度';
ALTER TABLE `passenger_order`
    ADD COLUMN `end_lng` DECIMAL(10, 7) NULL COMMENT '终点经度';
ALTER TABLE `passenger_order`
    ADD COLUMN `end_lat` DECIMAL(10, 7) NULL COMMENT '终点纬度';

ALTER TABLE `driver_trip`
    MODIFY COLUMN `departure_time` DATETIME NOT NULL;
ALTER TABLE `driver_trip`
    ADD COLUMN `start_lng` DECIMAL(10, 7) NULL COMMENT '起点经度';
ALTER TABLE `driver_trip`
    ADD COLUMN `start_lat` DECIMAL(10, 7) NULL COMMENT '起点纬度';
ALTER TABLE `driver_trip`
    ADD COLUMN `end_lng` DECIMAL(10, 7) NULL COMMENT '终点经度';
ALTER TABLE `driver_trip`
    ADD COLUMN `end_lat` DECIMAL(10, 7) NULL COMMENT '终点纬度';

ALTER TABLE `order_record`
    MODIFY COLUMN `departure_time` DATETIME NOT NULL;
ALTER TABLE `order_record`
    ADD COLUMN `start_lng` DECIMAL(10, 7) NULL COMMENT '起点经度';
ALTER TABLE `order_record`
    ADD COLUMN `start_lat` DECIMAL(10, 7) NULL COMMENT '起点纬度';
ALTER TABLE `order_record`
    ADD COLUMN `end_lng` DECIMAL(10, 7) NULL COMMENT '终点经度';
ALTER TABLE `order_record`
    ADD COLUMN `end_lat` DECIMAL(10, 7) NULL COMMENT '终点纬度';
ALTER TABLE `order_record`
    ADD COLUMN `actual_departure` DATETIME NULL COMMENT '实际出发时间';
ALTER TABLE `order_record`
    ADD COLUMN `actual_arrival` DATETIME NULL COMMENT '实际到达时间';
