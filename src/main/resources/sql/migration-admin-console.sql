-- ============================================================
-- 后台管理控制台 迁移脚本 (在已有库上执行)
--   1) sys_user 增加 role 列(USER / ADMIN)
--   2) 新增管理员账号 admin / 123456
-- 执行: mysql -uroot -p bank_account < migration-admin-console.sql
-- 说明: ADD COLUMN 重复执行会报 "Duplicate column name"，属正常，忽略即可
-- ============================================================

ALTER TABLE `sys_user`
    ADD COLUMN `role` VARCHAR(16) NOT NULL DEFAULT 'USER'
    COMMENT '角色:USER-普通用户,ADMIN-管理员' AFTER `status`;

-- 管理员账号: admin / 123456 (密码为 BCrypt)
INSERT INTO `sys_user` (`username`,`password`,`real_name`,`id_card`,`phone`,`email`,`status`,`role`)
SELECT 'admin','$2a$10$7ec96oWfHL7nDOsRpejb7uda5WA3ALmj0rlTUSYzqN0Zd8pQEQDtW',
       '系统管理员','110101199001010000','13800138000','admin@bank.com',1,'ADMIN'
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `username` = 'admin');

-- 可选: 把 zhangsan 也提为管理员(演示方便)
-- UPDATE `sys_user` SET `role` = 'ADMIN' WHERE `username` = 'zhangsan';