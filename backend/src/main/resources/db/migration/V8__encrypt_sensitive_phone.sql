DROP INDEX `uk_user_phone` ON `user`;

ALTER TABLE `user` MODIFY COLUMN `phone` VARCHAR(512) DEFAULT NULL;
ALTER TABLE `user` ADD COLUMN `phone_hash` VARCHAR(64) DEFAULT NULL;

CREATE UNIQUE INDEX `uk_user_phone_hash` ON `user` (`phone_hash`);
