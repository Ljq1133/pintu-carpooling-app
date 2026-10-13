-- Complete domain foundation for authentication, negotiation, governance and safety.

-- Core integrity constraints.
ALTER TABLE `vehicle`
    ADD CONSTRAINT `fk_vehicle_driver` FOREIGN KEY (`driver_id`) REFERENCES `user` (`id`);
ALTER TABLE `passenger_order`
    ADD CONSTRAINT `fk_passenger_order_passenger` FOREIGN KEY (`passenger_id`) REFERENCES `user` (`id`);
ALTER TABLE `driver_trip`
    ADD CONSTRAINT `fk_driver_trip_driver` FOREIGN KEY (`driver_id`) REFERENCES `user` (`id`);
ALTER TABLE `driver_trip`
    ADD CONSTRAINT `fk_driver_trip_vehicle` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`);
ALTER TABLE `match_request`
    ADD CONSTRAINT `fk_match_request_passenger_order` FOREIGN KEY (`passenger_order_id`) REFERENCES `passenger_order` (`id`);
ALTER TABLE `match_request`
    ADD CONSTRAINT `fk_match_request_driver_trip` FOREIGN KEY (`driver_trip_id`) REFERENCES `driver_trip` (`id`);
ALTER TABLE `match_request`
    ADD CONSTRAINT `fk_match_request_passenger` FOREIGN KEY (`passenger_id`) REFERENCES `user` (`id`);
ALTER TABLE `match_request`
    ADD CONSTRAINT `fk_match_request_driver` FOREIGN KEY (`driver_id`) REFERENCES `user` (`id`);
ALTER TABLE `order_record`
    ADD CONSTRAINT `fk_order_passenger_order` FOREIGN KEY (`passenger_order_id`) REFERENCES `passenger_order` (`id`);
ALTER TABLE `order_record`
    ADD CONSTRAINT `fk_order_driver_trip` FOREIGN KEY (`driver_trip_id`) REFERENCES `driver_trip` (`id`);
ALTER TABLE `order_record`
    ADD CONSTRAINT `fk_order_match_request` FOREIGN KEY (`match_request_id`) REFERENCES `match_request` (`id`);
ALTER TABLE `order_record`
    ADD CONSTRAINT `fk_order_passenger` FOREIGN KEY (`passenger_id`) REFERENCES `user` (`id`);
ALTER TABLE `order_record`
    ADD CONSTRAINT `fk_order_driver` FOREIGN KEY (`driver_id`) REFERENCES `user` (`id`);
ALTER TABLE `payment`
    ADD CONSTRAINT `fk_payment_order` FOREIGN KEY (`order_id`) REFERENCES `order_record` (`id`);
ALTER TABLE `review`
    ADD CONSTRAINT `fk_review_order` FOREIGN KEY (`order_id`) REFERENCES `order_record` (`id`);
ALTER TABLE `review`
    ADD CONSTRAINT `fk_review_passenger` FOREIGN KEY (`passenger_id`) REFERENCES `user` (`id`);
ALTER TABLE `review`
    ADD CONSTRAINT `fk_review_driver` FOREIGN KEY (`driver_id`) REFERENCES `user` (`id`);
ALTER TABLE `credit_record`
    ADD CONSTRAINT `fk_credit_record_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`);
ALTER TABLE `credit_record`
    ADD CONSTRAINT `fk_credit_record_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`);

ALTER TABLE `user`
    ADD CONSTRAINT `chk_user_credit` CHECK (`credit` BETWEEN 0 AND 100);
ALTER TABLE `vehicle`
    ADD CONSTRAINT `chk_vehicle_seats` CHECK (`seats` BETWEEN 1 AND 9);
ALTER TABLE `passenger_order`
    ADD CONSTRAINT `chk_passenger_order_count` CHECK (`passenger_count` BETWEEN 1 AND 8);
ALTER TABLE `driver_trip`
    ADD CONSTRAINT `chk_driver_trip_seats` CHECK (`available_seats` BETWEEN 0 AND 8);
ALTER TABLE `driver_trip`
    ADD CONSTRAINT `chk_driver_trip_price` CHECK (`price` >= 0);
ALTER TABLE `driver_trip`
    ADD CONSTRAINT `chk_driver_trip_overlap` CHECK (`overlap` BETWEEN 0 AND 100);
ALTER TABLE `order_record`
    ADD CONSTRAINT `chk_order_passenger_count` CHECK (`passenger_count` BETWEEN 1 AND 8);
ALTER TABLE `order_record`
    ADD CONSTRAINT `chk_order_price` CHECK (`price` >= 0);
ALTER TABLE `payment`
    ADD CONSTRAINT `chk_payment_amount` CHECK (`amount` >= 0);
ALTER TABLE `review`
    ADD CONSTRAINT `chk_review_rating` CHECK (`rating` BETWEEN 1 AND 5);
ALTER TABLE `user`
    ADD CONSTRAINT `chk_user_role` CHECK (`role` IN ('passenger', 'driver', 'admin'));
ALTER TABLE `user`
    ADD CONSTRAINT `chk_user_status`
        CHECK (`status` IN ('正常', '观察', '限制发单', '限制接单', '已封禁'));
ALTER TABLE `vehicle`
    ADD CONSTRAINT `chk_vehicle_status` CHECK (`status` IN ('待审核', '已认证', '已驳回', '已禁用'));
ALTER TABLE `passenger_order`
    ADD CONSTRAINT `chk_passenger_order_status`
        CHECK (`status` IN ('待匹配', '协商中', '已确认', '履约中', '待付款', '已完成', '已取消', '已归档'));
ALTER TABLE `driver_trip`
    ADD CONSTRAINT `chk_driver_trip_status`
        CHECK (`status` IN ('已发布', '已满座', '履约中', '已完成', '已取消'));
ALTER TABLE `match_request`
    ADD CONSTRAINT `chk_match_request_status`
        CHECK (`status` IN ('待处理', '已同意', '已拒绝', '已失效', '已撤回'));
ALTER TABLE `order_record`
    ADD CONSTRAINT `chk_order_status`
        CHECK (`status` IN ('待确认', '已确认', '履约中', '待付款', '已完成', '已取消', '已归档'));
ALTER TABLE `order_record`
    ADD CONSTRAINT `chk_order_payment_status`
        CHECK (`payment_status` IN ('未支付', '已支付', '已退款'));
ALTER TABLE `order_record`
    ADD CONSTRAINT `chk_order_review_status`
        CHECK (`review_status` IN ('未评价', '部分评价', '已评价'));
ALTER TABLE `payment`
    ADD CONSTRAINT `chk_payment_status` CHECK (`status` IN ('待支付', '已支付', '已退款', '支付失败'));
ALTER TABLE `audit_record`
    ADD CONSTRAINT `chk_audit_status` CHECK (`status` IN ('待审核', '已通过', '已驳回'));

CREATE INDEX `idx_passenger_order_match`
    ON `passenger_order` (`status`, `departure_time`, `deleted`);
CREATE INDEX `idx_driver_trip_match`
    ON `driver_trip` (`status`, `departure_time`, `available_seats`, `deleted`);
CREATE INDEX `idx_match_request_participants`
    ON `match_request` (`passenger_id`, `driver_id`, `status`);
CREATE INDEX `idx_order_passenger_status`
    ON `order_record` (`passenger_id`, `status`, `created_at`);
CREATE INDEX `idx_order_driver_status`
    ON `order_record` (`driver_id`, `status`, `created_at`);
CREATE INDEX `idx_audit_status_time`
    ON `audit_record` (`status`, `submitted_at`);

-- Order lifecycle history.
CREATE TABLE `order_status_history` (
    `id`              VARCHAR(32)  NOT NULL,
    `order_id`        VARCHAR(32)  NOT NULL,
    `operator_id`     VARCHAR(32)           DEFAULT NULL,
    `operator_role`   VARCHAR(32)           DEFAULT NULL,
    `from_status`     VARCHAR(16)           DEFAULT NULL,
    `to_status`       VARCHAR(16)  NOT NULL,
    `reason`          VARCHAR(500)          DEFAULT NULL,
    `idempotency_key` VARCHAR(64)           DEFAULT NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_history_idempotency` (`idempotency_key`),
    KEY `idx_order_history_order_time` (`order_id`, `created_at`),
    CONSTRAINT `fk_order_history_order` FOREIGN KEY (`order_id`) REFERENCES `order_record` (`id`),
    CONSTRAINT `fk_order_history_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
);

-- Authentication and account lifecycle.
CREATE TABLE `auth_refresh_token` (
    `id`            VARCHAR(32)  NOT NULL,
    `user_id`       VARCHAR(32)  NOT NULL,
    `token_hash`    VARCHAR(128) NOT NULL,
    `expires_at`    DATETIME     NOT NULL,
    `revoked_at`    DATETIME              DEFAULT NULL,
    `device_info`   VARCHAR(255)           DEFAULT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_refresh_token_hash` (`token_hash`),
    KEY `idx_refresh_user_expiry` (`user_id`, `expires_at`),
    CONSTRAINT `fk_refresh_token_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `verification_code` (
    `id`             VARCHAR(32)  NOT NULL,
    `phone_hash`     VARCHAR(128) NOT NULL,
    `purpose`        VARCHAR(32)  NOT NULL,
    `code_hash`      VARCHAR(128) NOT NULL,
    `expires_at`     DATETIME     NOT NULL,
    `consumed_at`    DATETIME              DEFAULT NULL,
    `attempt_count`  INT          NOT NULL DEFAULT 0,
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_verification_phone_purpose` (`phone_hash`, `purpose`, `expires_at`),
    CONSTRAINT `chk_verification_attempts` CHECK (`attempt_count` >= 0)
);

CREATE TABLE `password_reset_token` (
    `id`          VARCHAR(32)  NOT NULL,
    `user_id`     VARCHAR(32)  NOT NULL,
    `token_hash`  VARCHAR(128) NOT NULL,
    `expires_at`  DATETIME     NOT NULL,
    `consumed_at` DATETIME              DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_password_reset_hash` (`token_hash`),
    CONSTRAINT `fk_password_reset_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `login_failure` (
    `id`         VARCHAR(32)  NOT NULL,
    `username`   VARCHAR(64)  NOT NULL,
    `ip_address` VARCHAR(64)           DEFAULT NULL,
    `reason`     VARCHAR(255)          DEFAULT NULL,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_login_failure_identity_time` (`username`, `created_at`)
);

-- Identity and vehicle qualification workflow.
CREATE TABLE `identity_application` (
    `id`                 VARCHAR(32)  NOT NULL,
    `user_id`            VARCHAR(32)  NOT NULL,
    `real_name_cipher`   VARCHAR(512) NOT NULL,
    `id_card_cipher`     VARCHAR(512) NOT NULL,
    `id_card_masked`     VARCHAR(32)  NOT NULL,
    `liveness_result`    VARCHAR(32)           DEFAULT NULL,
    `status`             VARCHAR(16)  NOT NULL DEFAULT '待审核',
    `reject_reason`      VARCHAR(500)          DEFAULT NULL,
    `submitted_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `handled_at`         DATETIME              DEFAULT NULL,
    `deleted`            TINYINT      NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_identity_user_status` (`user_id`, `status`),
    CONSTRAINT `fk_identity_application_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `identity_attachment` (
    `id`             VARCHAR(32)  NOT NULL,
    `application_id` VARCHAR(32)  NOT NULL,
    `attachment_type` VARCHAR(32) NOT NULL,
    `storage_key`    VARCHAR(255) NOT NULL,
    `content_type`   VARCHAR(64)           DEFAULT NULL,
    `size_bytes`     BIGINT                DEFAULT NULL,
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_identity_attachment_application` (`application_id`),
    CONSTRAINT `fk_identity_attachment_application`
        FOREIGN KEY (`application_id`) REFERENCES `identity_application` (`id`) ON DELETE CASCADE
);

CREATE TABLE `vehicle_attachment` (
    `id`              VARCHAR(32)  NOT NULL,
    `vehicle_id`      VARCHAR(32)  NOT NULL,
    `attachment_type` VARCHAR(32)  NOT NULL,
    `storage_key`     VARCHAR(255) NOT NULL,
    `content_type`    VARCHAR(64)           DEFAULT NULL,
    `size_bytes`      BIGINT                DEFAULT NULL,
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_vehicle_attachment_vehicle` (`vehicle_id`),
    CONSTRAINT `fk_vehicle_attachment_vehicle`
        FOREIGN KEY (`vehicle_id`) REFERENCES `vehicle` (`id`) ON DELETE CASCADE
);

CREATE TABLE `audit_history` (
    `id`              VARCHAR(32) NOT NULL,
    `audit_record_id` VARCHAR(32) NOT NULL,
    `admin_id`        VARCHAR(32) NOT NULL,
    `from_status`     VARCHAR(16)          DEFAULT NULL,
    `to_status`       VARCHAR(16) NOT NULL,
    `reason`          VARCHAR(500)         DEFAULT NULL,
    `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_audit_history_record_time` (`audit_record_id`, `created_at`),
    CONSTRAINT `fk_audit_history_record` FOREIGN KEY (`audit_record_id`) REFERENCES `audit_record` (`id`),
    CONSTRAINT `fk_audit_history_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`)
);

-- Negotiation, chat and notification.
CREATE TABLE `conversation` (
    `id`            VARCHAR(32) NOT NULL,
    `order_id`      VARCHAR(32)          DEFAULT NULL,
    `match_request_id` VARCHAR(32)       DEFAULT NULL,
    `passenger_id`  VARCHAR(32) NOT NULL,
    `driver_id`     VARCHAR(32) NOT NULL,
    `status`        VARCHAR(16) NOT NULL DEFAULT '协商中',
    `closed_at`     DATETIME             DEFAULT NULL,
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_conversation_match_request` (`match_request_id`),
    KEY `idx_conversation_participants` (`passenger_id`, `driver_id`, `status`),
    CONSTRAINT `fk_conversation_order` FOREIGN KEY (`order_id`) REFERENCES `order_record` (`id`),
    CONSTRAINT `fk_conversation_match` FOREIGN KEY (`match_request_id`) REFERENCES `match_request` (`id`),
    CONSTRAINT `fk_conversation_passenger` FOREIGN KEY (`passenger_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_conversation_driver` FOREIGN KEY (`driver_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `chat_message` (
    `id`              VARCHAR(32) NOT NULL,
    `conversation_id` VARCHAR(32) NOT NULL,
    `sender_id`       VARCHAR(32) NOT NULL,
    `message_type`    VARCHAR(16) NOT NULL DEFAULT 'text',
    `content`         TEXT                 DEFAULT NULL,
    `attachment_key`  VARCHAR(255)         DEFAULT NULL,
    `client_message_id` VARCHAR(64)        DEFAULT NULL,
    `read_at`         DATETIME             DEFAULT NULL,
    `created_at`      DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_chat_client_message` (`conversation_id`, `client_message_id`),
    KEY `idx_chat_conversation_time` (`conversation_id`, `created_at`),
    KEY `idx_chat_sender_read` (`sender_id`, `read_at`),
    CONSTRAINT `fk_chat_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_chat_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `negotiation_proposal` (
    `id`              VARCHAR(32)    NOT NULL,
    `conversation_id` VARCHAR(32)    NOT NULL,
    `version`         INT            NOT NULL,
    `proposer_id`     VARCHAR(32)    NOT NULL,
    `price`           DECIMAL(10, 2) NOT NULL,
    `departure_time`  DATETIME       NOT NULL,
    `passenger_count` INT            NOT NULL,
    `pickup_location` VARCHAR(128)             DEFAULT NULL,
    `pickup_lng`      DECIMAL(10, 7)           DEFAULT NULL,
    `pickup_lat`      DECIMAL(10, 7)           DEFAULT NULL,
    `note`            VARCHAR(500)              DEFAULT NULL,
    `created_at`      DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_proposal_conversation_version` (`conversation_id`, `version`),
    CONSTRAINT `chk_proposal_version` CHECK (`version` > 0),
    CONSTRAINT `chk_proposal_price` CHECK (`price` >= 0),
    CONSTRAINT `chk_proposal_passenger_count` CHECK (`passenger_count` BETWEEN 1 AND 8),
    CONSTRAINT `fk_proposal_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversation` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_proposal_proposer` FOREIGN KEY (`proposer_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `proposal_confirmation` (
    `id`          VARCHAR(32) NOT NULL,
    `proposal_id` VARCHAR(32) NOT NULL,
    `user_id`     VARCHAR(32) NOT NULL,
    `role`        VARCHAR(16) NOT NULL,
    `confirmed_at` DATETIME   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_proposal_confirmation_user` (`proposal_id`, `user_id`),
    CONSTRAINT `fk_confirmation_proposal` FOREIGN KEY (`proposal_id`) REFERENCES `negotiation_proposal` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_confirmation_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `notification` (
    `id`            VARCHAR(32)  NOT NULL,
    `user_id`       VARCHAR(32)  NOT NULL,
    `type`          VARCHAR(32)  NOT NULL,
    `title`         VARCHAR(128) NOT NULL,
    `content`       VARCHAR(1000)         DEFAULT NULL,
    `biz_type`      VARCHAR(32)           DEFAULT NULL,
    `biz_id`        VARCHAR(32)           DEFAULT NULL,
    `read_at`       DATETIME              DEFAULT NULL,
    `send_status`   VARCHAR(16)  NOT NULL DEFAULT '待发送',
    `retry_count`   INT          NOT NULL DEFAULT 0,
    `next_retry_at` DATETIME              DEFAULT NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_notification_user_read_time` (`user_id`, `read_at`, `created_at`),
    KEY `idx_notification_retry` (`send_status`, `next_retry_at`),
    CONSTRAINT `chk_notification_retry` CHECK (`retry_count` >= 0),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);

-- Credit, blacklist and bans.
CREATE TABLE `credit_rule` (
    `id`          VARCHAR(32) NOT NULL,
    `code`        VARCHAR(64) NOT NULL,
    `name`        VARCHAR(128) NOT NULL,
    `event_type`  VARCHAR(64) NOT NULL,
    `delta`       INT         NOT NULL,
    `enabled`     TINYINT     NOT NULL DEFAULT 1,
    `version`     INT         NOT NULL DEFAULT 1,
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_credit_rule_code_version` (`code`, `version`)
);

ALTER TABLE `credit_record`
    ADD COLUMN `rule_id` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `credit_record`
    ADD COLUMN `event_type` VARCHAR(64) DEFAULT NULL;
ALTER TABLE `credit_record`
    ADD COLUMN `biz_type` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `credit_record`
    ADD COLUMN `biz_id` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `credit_record`
    ADD COLUMN `balance_after` INT DEFAULT NULL;
ALTER TABLE `credit_record`
    ADD CONSTRAINT `fk_credit_record_rule` FOREIGN KEY (`rule_id`) REFERENCES `credit_rule` (`id`);
CREATE INDEX `idx_credit_record_biz` ON `credit_record` (`biz_type`, `biz_id`);

CREATE TABLE `blacklist` (
    `id`          VARCHAR(32) NOT NULL,
    `owner_id`    VARCHAR(32) NOT NULL,
    `blocked_id`  VARCHAR(32) NOT NULL,
    `reason`      VARCHAR(255)         DEFAULT NULL,
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_blacklist_pair` (`owner_id`, `blocked_id`),
    CONSTRAINT `chk_blacklist_self` CHECK (`owner_id` <> `blocked_id`),
    CONSTRAINT `fk_blacklist_owner` FOREIGN KEY (`owner_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_blacklist_blocked` FOREIGN KEY (`blocked_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `ban_record` (
    `id`          VARCHAR(32) NOT NULL,
    `user_id`     VARCHAR(32) NOT NULL,
    `ban_type`    VARCHAR(32) NOT NULL,
    `reason`      VARCHAR(500)         DEFAULT NULL,
    `operator_id` VARCHAR(32)          DEFAULT NULL,
    `starts_at`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `ends_at`     DATETIME             DEFAULT NULL,
    `released_at` DATETIME             DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_ban_user_active` (`user_id`, `released_at`, `ends_at`),
    CONSTRAINT `fk_ban_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_ban_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
);

-- Appeals and arbitration.
CREATE TABLE `appeal_ticket` (
    `id`          VARCHAR(32) NOT NULL,
    `order_id`    VARCHAR(32) NOT NULL,
    `applicant_id` VARCHAR(32) NOT NULL,
    `assignee_id` VARCHAR(32)          DEFAULT NULL,
    `reason_type` VARCHAR(32) NOT NULL,
    `description` VARCHAR(2000)        DEFAULT NULL,
    `status`      VARCHAR(16) NOT NULL DEFAULT '待处理',
    `resolution`  VARCHAR(2000)        DEFAULT NULL,
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `closed_at`   DATETIME             DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_appeal_status_assignee` (`status`, `assignee_id`, `created_at`),
    CONSTRAINT `fk_appeal_order` FOREIGN KEY (`order_id`) REFERENCES `order_record` (`id`),
    CONSTRAINT `fk_appeal_applicant` FOREIGN KEY (`applicant_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_appeal_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `appeal_transition` (
    `id`          VARCHAR(32) NOT NULL,
    `appeal_id`   VARCHAR(32) NOT NULL,
    `operator_id` VARCHAR(32) NOT NULL,
    `from_status` VARCHAR(16)          DEFAULT NULL,
    `to_status`   VARCHAR(16) NOT NULL,
    `comment`     VARCHAR(1000)        DEFAULT NULL,
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_appeal_transition_time` (`appeal_id`, `created_at`),
    CONSTRAINT `fk_appeal_transition_ticket` FOREIGN KEY (`appeal_id`) REFERENCES `appeal_ticket` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_appeal_transition_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `appeal_evidence` (
    `id`          VARCHAR(32)  NOT NULL,
    `appeal_id`   VARCHAR(32)  NOT NULL,
    `uploader_id` VARCHAR(32)  NOT NULL,
    `storage_key` VARCHAR(255) NOT NULL,
    `content_type` VARCHAR(64)          DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_appeal_evidence_ticket` (`appeal_id`),
    CONSTRAINT `fk_appeal_evidence_ticket` FOREIGN KEY (`appeal_id`) REFERENCES `appeal_ticket` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_appeal_evidence_uploader` FOREIGN KEY (`uploader_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `arbitration_record` (
    `id`            VARCHAR(32) NOT NULL,
    `appeal_id`     VARCHAR(32) NOT NULL,
    `admin_id`      VARCHAR(32) NOT NULL,
    `action_type`   VARCHAR(32) NOT NULL,
    `amount`        DECIMAL(10, 2)      DEFAULT NULL,
    `credit_delta`  INT                 DEFAULT NULL,
    `remark`        VARCHAR(1000)       DEFAULT NULL,
    `confirmed_by`  VARCHAR(32)         DEFAULT NULL,
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_arbitration_appeal` (`appeal_id`, `created_at`),
    CONSTRAINT `fk_arbitration_appeal` FOREIGN KEY (`appeal_id`) REFERENCES `appeal_ticket` (`id`),
    CONSTRAINT `fk_arbitration_admin` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_arbitration_confirmer` FOREIGN KEY (`confirmed_by`) REFERENCES `user` (`id`)
);

-- Dynamic rule configuration and rollback.
CREATE TABLE `rule_config` (
    `id`             VARCHAR(32) NOT NULL,
    `rule_type`      VARCHAR(32) NOT NULL,
    `code`           VARCHAR(64) NOT NULL,
    `name`           VARCHAR(128) NOT NULL,
    `active_version` INT                  DEFAULT NULL,
    `status`         VARCHAR(16) NOT NULL DEFAULT '停用',
    `gray_percent`   INT         NOT NULL DEFAULT 100,
    `created_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_config_type_code` (`rule_type`, `code`),
    CONSTRAINT `chk_rule_gray_percent` CHECK (`gray_percent` BETWEEN 0 AND 100)
);

CREATE TABLE `rule_version` (
    `id`          VARCHAR(32) NOT NULL,
    `rule_id`     VARCHAR(32) NOT NULL,
    `version`     INT         NOT NULL,
    `content`     TEXT        NOT NULL,
    `status`      VARCHAR(16) NOT NULL DEFAULT '草稿',
    `created_by`  VARCHAR(32) NOT NULL,
    `published_at` DATETIME            DEFAULT NULL,
    `created_at`  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_rule_version_number` (`rule_id`, `version`),
    CONSTRAINT `chk_rule_version_positive` CHECK (`version` > 0),
    CONSTRAINT `fk_rule_version_config` FOREIGN KEY (`rule_id`) REFERENCES `rule_config` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_rule_version_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
);

CREATE TABLE `rule_rollback` (
    `id`            VARCHAR(32) NOT NULL,
    `rule_id`       VARCHAR(32) NOT NULL,
    `from_version`  INT         NOT NULL,
    `to_version`    INT         NOT NULL,
    `operator_id`   VARCHAR(32) NOT NULL,
    `reason`        VARCHAR(500)         DEFAULT NULL,
    `created_at`    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_rule_rollback_rule_time` (`rule_id`, `created_at`),
    CONSTRAINT `fk_rule_rollback_config` FOREIGN KEY (`rule_id`) REFERENCES `rule_config` (`id`),
    CONSTRAINT `fk_rule_rollback_operator` FOREIGN KEY (`operator_id`) REFERENCES `user` (`id`)
);

-- Location, trajectory and safety.
CREATE TABLE `location_report` (
    `id`          VARCHAR(32)   NOT NULL,
    `order_id`    VARCHAR(32)   NOT NULL,
    `user_id`     VARCHAR(32)   NOT NULL,
    `longitude`   DECIMAL(10,7) NOT NULL,
    `latitude`    DECIMAL(10,7) NOT NULL,
    `accuracy`    DECIMAL(8,2)           DEFAULT NULL,
    `speed`       DECIMAL(8,2)           DEFAULT NULL,
    `heading`     DECIMAL(8,2)           DEFAULT NULL,
    `reported_at` DATETIME      NOT NULL,
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_location_order_time` (`order_id`, `reported_at`),
    CONSTRAINT `chk_location_lng` CHECK (`longitude` BETWEEN -180 AND 180),
    CONSTRAINT `chk_location_lat` CHECK (`latitude` BETWEEN -90 AND 90),
    CONSTRAINT `fk_location_order` FOREIGN KEY (`order_id`) REFERENCES `order_record` (`id`),
    CONSTRAINT `fk_location_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);

CREATE TABLE `trajectory_point` (
    `id`          VARCHAR(32)   NOT NULL,
    `order_id`    VARCHAR(32)   NOT NULL,
    `sequence_no` INT           NOT NULL,
    `longitude`   DECIMAL(10,7) NOT NULL,
    `latitude`    DECIMAL(10,7) NOT NULL,
    `recorded_at` DATETIME      NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trajectory_order_sequence` (`order_id`, `sequence_no`),
    KEY `idx_trajectory_order_time` (`order_id`, `recorded_at`),
    CONSTRAINT `fk_trajectory_order` FOREIGN KEY (`order_id`) REFERENCES `order_record` (`id`)
);

CREATE TABLE `safety_alert` (
    `id`          VARCHAR(32)   NOT NULL,
    `order_id`    VARCHAR(32)            DEFAULT NULL,
    `user_id`     VARCHAR(32)   NOT NULL,
    `alert_type`  VARCHAR(32)   NOT NULL,
    `longitude`   DECIMAL(10,7)          DEFAULT NULL,
    `latitude`    DECIMAL(10,7)          DEFAULT NULL,
    `details`     VARCHAR(1000)           DEFAULT NULL,
    `status`      VARCHAR(16)   NOT NULL DEFAULT '待处理',
    `handled_by`  VARCHAR(32)            DEFAULT NULL,
    `handled_at`  DATETIME               DEFAULT NULL,
    `created_at`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_safety_alert_status_time` (`status`, `created_at`),
    CONSTRAINT `fk_safety_alert_order` FOREIGN KEY (`order_id`) REFERENCES `order_record` (`id`),
    CONSTRAINT `fk_safety_alert_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_safety_alert_handler` FOREIGN KEY (`handled_by`) REFERENCES `user` (`id`)
);

CREATE TABLE `emergency_contact` (
    `id`           VARCHAR(32)  NOT NULL,
    `user_id`      VARCHAR(32)  NOT NULL,
    `name`         VARCHAR(64)  NOT NULL,
    `phone_cipher` VARCHAR(512) NOT NULL,
    `phone_masked` VARCHAR(32)  NOT NULL,
    `relationship` VARCHAR(32)           DEFAULT NULL,
    `is_primary`   TINYINT      NOT NULL DEFAULT 0,
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_emergency_contact_user` (`user_id`, `is_primary`),
    CONSTRAINT `fk_emergency_contact_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE
);

-- Bidirectional review model. The legacy columns remain for compatibility until services migrate.
CREATE INDEX `idx_review_order_fk` ON `review` (`order_id`);
ALTER TABLE `review` DROP INDEX `uk_order`;
ALTER TABLE `review`
    ADD COLUMN `reviewer_id` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `review`
    ADD COLUMN `reviewee_id` VARCHAR(32) DEFAULT NULL;
ALTER TABLE `review`
    ADD COLUMN `tags` VARCHAR(500) DEFAULT NULL;
ALTER TABLE `review`
    ADD COLUMN `is_default` TINYINT NOT NULL DEFAULT 0;
ALTER TABLE `review`
    ADD CONSTRAINT `fk_review_reviewer` FOREIGN KEY (`reviewer_id`) REFERENCES `user` (`id`);
ALTER TABLE `review`
    ADD CONSTRAINT `fk_review_reviewee` FOREIGN KEY (`reviewee_id`) REFERENCES `user` (`id`);
ALTER TABLE `review`
    ADD CONSTRAINT `uk_review_order_reviewer` UNIQUE (`order_id`, `reviewer_id`);
CREATE INDEX `idx_review_reviewee_time` ON `review` (`reviewee_id`, `created_at`);
