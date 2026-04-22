ALTER TABLE `user`
    ADD COLUMN `role` VARCHAR(16) NOT NULL DEFAULT 'USER' COMMENT '角色：USER=普通用户 ADMIN=管理员' AFTER `status`;

UPDATE `user`
SET `role` = 'USER'
WHERE `role` IS NULL OR `role` = '';

-- 为初始管理员授予 ADMIN 角色时，按实际账号执行其一：
-- UPDATE `user` SET `role` = 'ADMIN' WHERE `id` = <ADMIN_USER_ID>;
-- UPDATE `user` SET `role` = 'ADMIN' WHERE `username` = '<ADMIN_USERNAME>' AND `delete_version` = 0 AND `is_deleted` = 0;
