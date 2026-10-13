CREATE TABLE `admin_role_assignment` (
    `id`         VARCHAR(32) NOT NULL,
    `admin_id`   VARCHAR(32) NOT NULL,
    `sub_role`   VARCHAR(32) NOT NULL,
    `status`     VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `deleted`    TINYINT     NOT NULL DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_role_assignment` (`admin_id`, `sub_role`),
    KEY `idx_admin_role_status` (`sub_role`, `status`),
    CONSTRAINT `fk_admin_role_user` FOREIGN KEY (`admin_id`) REFERENCES `user` (`id`),
    CONSTRAINT `chk_admin_sub_role` CHECK (`sub_role` IN ('SUPER_ADMIN', 'AUDITOR', 'OPS', 'CUSTOMER_SERVICE')),
    CONSTRAINT `chk_admin_role_status` CHECK (`status` IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE `admin_role_permission` (
    `id`         VARCHAR(32) NOT NULL,
    `sub_role`   VARCHAR(32) NOT NULL,
    `permission` VARCHAR(32) NOT NULL,
    `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_admin_role_permission` (`sub_role`, `permission`),
    KEY `idx_admin_permission` (`permission`),
    CONSTRAINT `chk_admin_permission_role` CHECK (`sub_role` IN ('SUPER_ADMIN', 'AUDITOR', 'OPS', 'CUSTOMER_SERVICE')),
    CONSTRAINT `chk_admin_permission_code` CHECK (`permission` IN ('AUDIT_REVIEW', 'ORDER_VIEW', 'CREDIT_MANAGE'))
);

INSERT IGNORE INTO `admin_role_assignment` (`id`, `admin_id`, `sub_role`, `status`)
VALUES ('ara-admin-001-super', 'admin-001', 'SUPER_ADMIN', 'ACTIVE');

INSERT IGNORE INTO `admin_role_permission` (`id`, `sub_role`, `permission`)
VALUES
    ('arp-super-audit', 'SUPER_ADMIN', 'AUDIT_REVIEW'),
    ('arp-super-order', 'SUPER_ADMIN', 'ORDER_VIEW'),
    ('arp-super-credit', 'SUPER_ADMIN', 'CREDIT_MANAGE'),
    ('arp-auditor-audit', 'AUDITOR', 'AUDIT_REVIEW'),
    ('arp-ops-order', 'OPS', 'ORDER_VIEW'),
    ('arp-ops-credit', 'OPS', 'CREDIT_MANAGE'),
    ('arp-cs-order', 'CUSTOMER_SERVICE', 'ORDER_VIEW');
