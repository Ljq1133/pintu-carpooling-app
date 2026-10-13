-- Demo password remains 123456, stored only as a BCrypt hash.
UPDATE `user`
SET `password` = '$2b$10$26dwYzEywYMrYowFi9Ht..VbSDK710tcSRyPs4uMLmbD1rdi6ucmK'
WHERE `username` IN ('passenger_demo', 'driver_demo', 'admin_demo');
