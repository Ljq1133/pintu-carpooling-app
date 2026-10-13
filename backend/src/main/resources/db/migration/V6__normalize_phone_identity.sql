UPDATE `user` SET `phone` = '13800005678' WHERE `id` = 'passenger-001';
UPDATE `user` SET `phone` = '13800002678' WHERE `id` = 'driver-001';
UPDATE `user` SET `phone` = '02100000001' WHERE `id` = 'admin-001';

CREATE UNIQUE INDEX `uk_user_phone` ON `user` (`phone`);
