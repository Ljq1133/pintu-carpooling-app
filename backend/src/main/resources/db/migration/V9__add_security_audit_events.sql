CREATE TABLE `security_audit_event` (
    `id`          VARCHAR(32)  NOT NULL,
    `user_id`     VARCHAR(32)           DEFAULT NULL,
    `event_type`  VARCHAR(64)  NOT NULL,
    `identity`    VARCHAR(128)          DEFAULT NULL,
    `ip_address`  VARCHAR(64)           DEFAULT NULL,
    `success`     TINYINT      NOT NULL DEFAULT 1,
    `detail`      VARCHAR(255)          DEFAULT NULL,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_security_event_type_time` (`event_type`, `created_at`),
    KEY `idx_security_event_user_time` (`user_id`, `created_at`),
    CONSTRAINT `fk_security_event_user`
        FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
);

