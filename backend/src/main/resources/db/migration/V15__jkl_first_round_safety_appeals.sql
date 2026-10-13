-- First J/K/L completion slice: safety links, virtual phone, and admin permissions.

CREATE TABLE `virtual_phone_binding` (
    `id`                  VARCHAR(32)  NOT NULL,
    `order_id`            VARCHAR(32)  NOT NULL,
    `passenger_id`        VARCHAR(32)  NOT NULL,
    `driver_id`           VARCHAR(32)  NOT NULL,
    `virtual_number`      VARCHAR(32)  NOT NULL,
    `provider_binding_id` VARCHAR(64)  NOT NULL,
    `status`              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
    `bound_at`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `unbound_at`          DATETIME              DEFAULT NULL,
    `unbind_reason`       VARCHAR(255)          DEFAULT NULL,
    `created_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_virtual_phone_order_active` (`order_id`, `status`),
    KEY `idx_virtual_phone_participants` (`passenger_id`, `driver_id`, `status`),
    CONSTRAINT `fk_virtual_phone_order` FOREIGN KEY (`order_id`) REFERENCES `order_record` (`id`),
    CONSTRAINT `fk_virtual_phone_passenger` FOREIGN KEY (`passenger_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_virtual_phone_driver` FOREIGN KEY (`driver_id`) REFERENCES `user` (`id`),
    CONSTRAINT `chk_virtual_phone_status` CHECK (`status` IN ('ACTIVE', 'UNBOUND'))
);

CREATE TABLE `trip_share_token` (
    `id`         VARCHAR(32)  NOT NULL,
    `token`      VARCHAR(96)  NOT NULL,
    `order_id`   VARCHAR(32)  NOT NULL,
    `created_by` VARCHAR(32)  NOT NULL,
    `expires_at` DATETIME     NOT NULL,
    `revoked_at` DATETIME              DEFAULT NULL,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_trip_share_token` (`token`),
    KEY `idx_trip_share_order` (`order_id`, `expires_at`),
    CONSTRAINT `fk_trip_share_order` FOREIGN KEY (`order_id`) REFERENCES `order_record` (`id`),
    CONSTRAINT `fk_trip_share_creator` FOREIGN KEY (`created_by`) REFERENCES `user` (`id`)
);

ALTER TABLE `order_record`
    ADD COLUMN `admin_note` VARCHAR(1000) DEFAULT NULL;
