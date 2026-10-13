-- PinTu core schema.
-- Flyway runs this migration once for a new database.

-- ------------------------------------------------------------
-- 用户表：乘客 / 司机 / 管理员统一表
-- ------------------------------------------------------------
CREATE TABLE `user` (
    `id`            VARCHAR(32)  NOT NULL COMMENT '用户 ID，沿用前端前缀，如 passenger-001 / driver-001 / admin-001',
    `username`      VARCHAR(64)  NOT NULL COMMENT '登录账号',
    `password`      VARCHAR(128) NOT NULL COMMENT '密码（MVP 阶段为明文，仅供演示）',
    `name`          VARCHAR(64)  NOT NULL COMMENT '姓名 / 昵称',
    `phone`         VARCHAR(32)            DEFAULT NULL COMMENT '手机号（可脱敏存储）',
    `role`          VARCHAR(16)  NOT NULL COMMENT '角色：passenger / driver / admin',
    `credit`        INT          NOT NULL DEFAULT 100 COMMENT '信用分（0~100）',
    `status`        VARCHAR(16)  NOT NULL DEFAULT '正常' COMMENT '账号状态：正常 / 观察 / 限制发单 / 限制接单 / 已封禁',
    `trip_count`    INT          NOT NULL DEFAULT 0 COMMENT '完成行程数',
    `saved_money`   DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '累计节省金额（元）',
    `carbon_saved`  DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '累计减碳数据（kg）',
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除 / 1 已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`)
);

-- ------------------------------------------------------------
-- 车辆表
-- ------------------------------------------------------------
CREATE TABLE `vehicle` (
    `id`         VARCHAR(32) NOT NULL COMMENT '车辆 ID，如 veh-001',
    `driver_id`  VARCHAR(32) NOT NULL COMMENT '所属司机 ID',
    `plate`      VARCHAR(32) NOT NULL COMMENT '车牌号',
    `model`      VARCHAR(64) NOT NULL COMMENT '车型',
    `color`      VARCHAR(32)          DEFAULT NULL COMMENT '颜色',
    `seats`      INT         NOT NULL DEFAULT 4 COMMENT '座位数',
    `status`     VARCHAR(16) NOT NULL DEFAULT '待审核' COMMENT '车辆审核状态：待审核 / 已认证 / 已驳回',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_plate` (`plate`),
  KEY `idx_vehicle_driver` (`driver_id`)
);

-- ------------------------------------------------------------
-- 乘客拼车需求表
-- ------------------------------------------------------------
CREATE TABLE `passenger_order` (
    `id`               VARCHAR(32) NOT NULL COMMENT '需求 ID，如 po-...',
    `passenger_id`     VARCHAR(32) NOT NULL COMMENT '乘客 ID',
    `start_location`   VARCHAR(128) NOT NULL COMMENT '出发地',
    `end_location`     VARCHAR(128) NOT NULL COMMENT '目的地',
    `departure_time`   VARCHAR(32) NOT NULL COMMENT '出发时间，格式 yyyy-MM-dd HH:mm',
    `passenger_count`  INT         NOT NULL DEFAULT 1 COMMENT '乘车人数',
    `note`             VARCHAR(255)          DEFAULT NULL COMMENT '备注',
    `status`           VARCHAR(16) NOT NULL DEFAULT '待匹配' COMMENT '需求状态：待匹配 / 协商中 / 已确认 / 履约中 / 待付款 / 已完成 / 已取消',
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_passenger_order_passenger` (`passenger_id`),
  KEY `idx_passenger_order_status` (`status`)
);

-- ------------------------------------------------------------
-- 司机行程表
-- ------------------------------------------------------------
CREATE TABLE `driver_trip` (
    `id`               VARCHAR(32) NOT NULL COMMENT '行程 ID，如 trip-...',
    `driver_id`        VARCHAR(32) NOT NULL COMMENT '司机 ID',
    `vehicle_id`       VARCHAR(32) NOT NULL COMMENT '车辆 ID',
    `start_location`   VARCHAR(128) NOT NULL COMMENT '出发地',
    `end_location`     VARCHAR(128) NOT NULL COMMENT '目的地',
    `departure_time`   VARCHAR(32) NOT NULL COMMENT '出发时间，格式 yyyy-MM-dd HH:mm',
    `available_seats`  INT         NOT NULL DEFAULT 1 COMMENT '剩余座位',
    `price`            DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '单人价格（元）',
    `overlap`          INT         NOT NULL DEFAULT 80 COMMENT '模拟顺路度（0-100）',
    `status`           VARCHAR(16) NOT NULL DEFAULT '已发布' COMMENT '行程状态：已发布 / 已满座 / 履约中 / 已完成 / 已取消',
    `created_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`          TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_driver_trip_driver` (`driver_id`),
  KEY `idx_driver_trip_status` (`status`)
);

-- ------------------------------------------------------------
-- 匹配请求表
-- ------------------------------------------------------------
CREATE TABLE `match_request` (
    `id`                 VARCHAR(32) NOT NULL COMMENT '请求 ID，如 req-...',
    `passenger_order_id` VARCHAR(32) NOT NULL COMMENT '乘客需求 ID',
    `driver_trip_id`     VARCHAR(32) NOT NULL COMMENT '司机行程 ID',
    `passenger_id`       VARCHAR(32) NOT NULL COMMENT '乘客 ID',
    `driver_id`          VARCHAR(32) NOT NULL COMMENT '司机 ID',
    `message`            VARCHAR(255)         DEFAULT NULL COMMENT '乘客留言',
    `status`             VARCHAR(16) NOT NULL DEFAULT '待处理' COMMENT '请求状态：待处理 / 已同意 / 已拒绝 / 已失效',
    `reject_reason`      VARCHAR(255)         DEFAULT NULL COMMENT '拒绝原因',
    `order_id`           VARCHAR(32)          DEFAULT NULL COMMENT '同意后生成的正式订单 ID',
    `handled_at`         DATETIME             DEFAULT NULL COMMENT '处理时间',
    `created_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_match_request_passenger_order` (`passenger_order_id`),
  KEY `idx_match_request_driver_trip` (`driver_trip_id`),
  KEY `idx_match_request_status` (`status`)
);

-- ------------------------------------------------------------
-- 正式订单表（双方确认后形成）
-- ------------------------------------------------------------
CREATE TABLE `order_record` (
    `id`                 VARCHAR(32) NOT NULL COMMENT '订单 ID，如 order-...',
    `passenger_order_id` VARCHAR(32) NOT NULL COMMENT '关联乘客需求',
    `driver_trip_id`     VARCHAR(32) NOT NULL COMMENT '关联司机行程',
    `match_request_id`   VARCHAR(32)          DEFAULT NULL COMMENT '关联匹配请求',
    `passenger_id`       VARCHAR(32) NOT NULL COMMENT '乘客 ID',
    `driver_id`          VARCHAR(32) NOT NULL COMMENT '司机 ID',
    `start_location`     VARCHAR(128) NOT NULL COMMENT '出发地',
    `end_location`       VARCHAR(128) NOT NULL COMMENT '目的地',
    `departure_time`     VARCHAR(32) NOT NULL COMMENT '出发时间',
    `passenger_count`    INT         NOT NULL DEFAULT 1 COMMENT '乘车人数',
    `price`              DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '订单金额（元）',
    `status`             VARCHAR(16) NOT NULL DEFAULT '已确认' COMMENT '订单状态：待确认 / 已确认 / 履约中 / 待付款 / 已完成 / 已取消',
    `payment_status`     VARCHAR(16) NOT NULL DEFAULT '未支付' COMMENT '支付状态：未支付 / 已支付',
    `review_status`      VARCHAR(16) NOT NULL DEFAULT '未评价' COMMENT '评价状态：未评价 / 已评价',
    `cancel_reason`      VARCHAR(255)         DEFAULT NULL COMMENT '取消原因',
    `created_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted`            TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_passenger_order` (`passenger_order_id`),
    UNIQUE KEY `uk_match_request` (`match_request_id`),
  KEY `idx_order_record_passenger` (`passenger_id`),
  KEY `idx_order_record_driver` (`driver_id`),
  KEY `idx_order_record_status` (`status`)
);

-- ------------------------------------------------------------
-- 支付记录表
-- ------------------------------------------------------------
CREATE TABLE `payment` (
    `id`         VARCHAR(32) NOT NULL COMMENT '支付 ID，如 pay-...',
    `order_id`   VARCHAR(32) NOT NULL COMMENT '订单 ID',
    `amount`     DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '金额（元）',
    `method`     VARCHAR(32) NOT NULL DEFAULT 'mock_balance' COMMENT '支付方式（MVP 模拟）',
    `status`     VARCHAR(16) NOT NULL DEFAULT '已支付' COMMENT '支付状态',
    `paid_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '支付时间',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_payment_order` (`order_id`)
);

-- ------------------------------------------------------------
-- 评价表
-- ------------------------------------------------------------
CREATE TABLE `review` (
    `id`           VARCHAR(32) NOT NULL COMMENT '评价 ID，如 review-...',
    `order_id`     VARCHAR(32) NOT NULL COMMENT '订单 ID',
    `passenger_id` VARCHAR(32) NOT NULL COMMENT '乘客 ID',
    `driver_id`    VARCHAR(32) NOT NULL COMMENT '司机 ID',
    `rating`       INT         NOT NULL DEFAULT 5 COMMENT '评分 1~5',
    `comment`      VARCHAR(500)         DEFAULT NULL COMMENT '评价内容',
    `created_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`      TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order` (`order_id`)
);

-- ------------------------------------------------------------
-- 审核记录表（实名 / 车辆）
-- ------------------------------------------------------------
CREATE TABLE `audit_record` (
    `id`         VARCHAR(32) NOT NULL COMMENT '审核记录 ID',
    `target_type` VARCHAR(16) NOT NULL COMMENT '审核对象类型：user / vehicle',
    `target_id`  VARCHAR(32) NOT NULL COMMENT '审核对象 ID',
    `name`       VARCHAR(64)          DEFAULT NULL COMMENT '展示名称（用户名或车牌）',
    `plate`      VARCHAR(32)          DEFAULT NULL COMMENT '车牌（仅车辆审核）',
    `status`     VARCHAR(16) NOT NULL DEFAULT '待审核' COMMENT '审核状态：待审核 / 已通过 / 已驳回',
    `reason`     VARCHAR(255)         DEFAULT NULL COMMENT '驳回原因',
    `submitted_at` DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `handled_at` DATETIME             DEFAULT NULL,
    `deleted`    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_audit_record_target` (`target_type`, `target_id`)
);

-- ------------------------------------------------------------
-- 信用分变动记录表
-- ------------------------------------------------------------
CREATE TABLE `credit_record` (
    `id`         VARCHAR(32) NOT NULL COMMENT '信用变动 ID',
    `user_id`    VARCHAR(32) NOT NULL COMMENT '用户 ID',
    `delta`      INT         NOT NULL COMMENT '变动值，正为加分，负为扣分',
    `reason`     VARCHAR(255)         DEFAULT NULL COMMENT '变动原因',
    `operator_id` VARCHAR(32)         DEFAULT NULL COMMENT '操作人（管理员 ID）',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
  KEY `idx_credit_record_user` (`user_id`)
);
