-- Second J/K/L completion slice: account settings, support, retention and admin governance.

ALTER TABLE `location_report`
    ADD COLUMN `client_report_id` VARCHAR(64) DEFAULT NULL;
ALTER TABLE `location_report`
    ADD COLUMN `batch_id` VARCHAR(64) DEFAULT NULL;
ALTER TABLE `location_report`
    ADD COLUMN `report_interval_seconds` INT DEFAULT NULL;
ALTER TABLE `location_report`
    ADD COLUMN `retry_count` INT NOT NULL DEFAULT 0;
CREATE INDEX `idx_location_report_batch` ON `location_report` (`order_id`, `batch_id`);
CREATE UNIQUE INDEX `uk_location_client_report` ON `location_report` (`order_id`, `user_id`, `client_report_id`);

CREATE TABLE `account_preference` (
    `user_id`          VARCHAR(32) NOT NULL,
    `dark_mode`        TINYINT     NOT NULL DEFAULT 0,
    `font_scale`       VARCHAR(16) NOT NULL DEFAULT 'standard',
    `locale`           VARCHAR(16) NOT NULL DEFAULT 'zh-CN',
    `protocol_version` VARCHAR(32)          DEFAULT '2026-06',
    `updated_at`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_account_preference_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `chk_account_preference_font` CHECK (`font_scale` IN ('compact', 'standard', 'large')),
    CONSTRAINT `chk_account_preference_locale` CHECK (`locale` IN ('zh-CN', 'en-US'))
);

CREATE TABLE `account_binding` (
    `id`             VARCHAR(32)  NOT NULL,
    `user_id`        VARCHAR(32)  NOT NULL,
    `binding_type`   VARCHAR(32)  NOT NULL,
    `provider`       VARCHAR(64)           DEFAULT NULL,
    `account_label`  VARCHAR(128)          DEFAULT NULL,
    `account_value`  VARCHAR(255) NOT NULL,
    `masked_value`   VARCHAR(128) NOT NULL,
    `status`         VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    `created_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_binding_type_value` (`user_id`, `binding_type`, `account_value`),
    KEY `idx_account_binding_user_status` (`user_id`, `status`),
    CONSTRAINT `fk_account_binding_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `chk_account_binding_type` CHECK (`binding_type` IN ('PHONE', 'PAYMENT', 'SOCIAL')),
    CONSTRAINT `chk_account_binding_status` CHECK (`status` IN ('ACTIVE', 'UNBOUND'))
);

CREATE TABLE `support_session` (
    `id`           VARCHAR(32)  NOT NULL,
    `user_id`      VARCHAR(32)  NOT NULL,
    `assignee_id`  VARCHAR(32)           DEFAULT NULL,
    `subject`      VARCHAR(128) NOT NULL,
    `status`       VARCHAR(16)  NOT NULL DEFAULT 'OPEN',
    `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `closed_at`    DATETIME              DEFAULT NULL,
    PRIMARY KEY (`id`),
    KEY `idx_support_session_user` (`user_id`, `created_at`),
    KEY `idx_support_session_status` (`status`, `assignee_id`, `created_at`),
    CONSTRAINT `fk_support_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_support_session_assignee` FOREIGN KEY (`assignee_id`) REFERENCES `user` (`id`),
    CONSTRAINT `chk_support_session_status` CHECK (`status` IN ('OPEN', 'CLAIMED', 'CLOSED'))
);

CREATE TABLE `support_message` (
    `id`                VARCHAR(32) NOT NULL,
    `session_id`        VARCHAR(32) NOT NULL,
    `sender_id`         VARCHAR(32) NOT NULL,
    `sender_role`       VARCHAR(16) NOT NULL,
    `message_type`      VARCHAR(16) NOT NULL DEFAULT 'text',
    `content`           TEXT                 DEFAULT NULL,
    `attachment_key`    VARCHAR(255)         DEFAULT NULL,
    `client_message_id` VARCHAR(64)          DEFAULT NULL,
    `created_at`        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_support_client_message` (`session_id`, `client_message_id`),
    KEY `idx_support_message_time` (`session_id`, `created_at`),
    CONSTRAINT `fk_support_message_session` FOREIGN KEY (`session_id`) REFERENCES `support_session` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_support_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `user` (`id`),
    CONSTRAINT `chk_support_message_type` CHECK (`message_type` IN ('text', 'image'))
);

CREATE TABLE `data_retention_policy` (
    `id`             VARCHAR(32)  NOT NULL,
    `data_type`      VARCHAR(32)  NOT NULL,
    `retention_days` INT          NOT NULL,
    `delete_mode`    VARCHAR(32)  NOT NULL DEFAULT 'ARCHIVE_THEN_DELETE',
    `description`    VARCHAR(500)          DEFAULT NULL,
    `updated_by`     VARCHAR(32)           DEFAULT NULL,
    `updated_at`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_retention_data_type` (`data_type`),
    CONSTRAINT `chk_retention_days` CHECK (`retention_days` > 0),
    CONSTRAINT `fk_retention_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `user` (`id`)
);

INSERT INTO `data_retention_policy`
    (`id`, `data_type`, `retention_days`, `delete_mode`, `description`)
VALUES
    ('ret-location', 'LOCATION', 30, 'ARCHIVE_THEN_DELETE', 'Precise trip locations are retained briefly after trip completion.'),
    ('ret-chat', 'CHAT', 180, 'ARCHIVE_THEN_DELETE', 'Negotiation and support messages are retained for dispute handling.'),
    ('ret-attachment', 'ATTACHMENT', 180, 'DELETE_OBJECT', 'Evidence and support attachments follow dispute retention.'),
    ('ret-audit', 'AUDIT', 365, 'KEEP_AUDIT', 'Security and admin audit events are retained for traceability.');
