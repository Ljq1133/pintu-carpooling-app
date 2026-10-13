CREATE TABLE `user_role` (
    `id`         VARCHAR(32) NOT NULL,
    `user_id`    VARCHAR(32) NOT NULL,
    `role`       VARCHAR(16) NOT NULL,
    `status`     VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role`),
    KEY `idx_user_role_status` (`role`, `status`),
    CONSTRAINT `fk_user_role_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `chk_user_role_name` CHECK (`role` IN ('passenger', 'driver', 'admin')),
    CONSTRAINT `chk_user_role_status` CHECK (`status` IN ('PENDING', 'ACTIVE', 'REJECTED'))
);

INSERT INTO `user_role` (`id`, `user_id`, `role`, `status`)
SELECT CONCAT('ur-', `id`), `id`, `role`, 'ACTIVE' FROM `user`;

INSERT INTO `user_role` (`id`, `user_id`, `role`, `status`)
VALUES ('ur-driver-passenger', 'driver-001', 'passenger', 'ACTIVE');

ALTER TABLE `auth_refresh_token`
    ADD COLUMN `active_role` VARCHAR(16) DEFAULT NULL;

