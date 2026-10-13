ALTER TABLE `admin_role_permission` DROP CONSTRAINT `chk_admin_permission_code`;

ALTER TABLE `admin_role_permission`
    ADD CONSTRAINT `chk_admin_permission_code`
        CHECK (`permission` IN (
            'AUDIT_REVIEW',
            'ORDER_VIEW',
            'CREDIT_MANAGE',
            'APPEAL_MANAGE',
            'ARBITRATION_MANAGE',
            'SAFETY_MONITOR',
            'RULE_MANAGE',
            'SUPPORT_MANAGE',
            'USER_RESTRICT'
        ));

INSERT IGNORE INTO `admin_role_permission` (`id`, `sub_role`, `permission`)
VALUES
    ('arp-super-appeal', 'SUPER_ADMIN', 'APPEAL_MANAGE'),
    ('arp-super-arbitration', 'SUPER_ADMIN', 'ARBITRATION_MANAGE'),
    ('arp-super-safety', 'SUPER_ADMIN', 'SAFETY_MONITOR'),
    ('arp-super-rule', 'SUPER_ADMIN', 'RULE_MANAGE'),
    ('arp-super-support', 'SUPER_ADMIN', 'SUPPORT_MANAGE'),
    ('arp-super-restrict', 'SUPER_ADMIN', 'USER_RESTRICT'),
    ('arp-ops-safety', 'OPS', 'SAFETY_MONITOR'),
    ('arp-ops-rule', 'OPS', 'RULE_MANAGE'),
    ('arp-ops-restrict', 'OPS', 'USER_RESTRICT'),
    ('arp-cs-appeal', 'CUSTOMER_SERVICE', 'APPEAL_MANAGE'),
    ('arp-cs-arbitration', 'CUSTOMER_SERVICE', 'ARBITRATION_MANAGE'),
    ('arp-cs-support', 'CUSTOMER_SERVICE', 'SUPPORT_MANAGE');
