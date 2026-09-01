-- ============================================================
-- 模拟银行记账与理财系统 数据库初始化脚本
-- 数据库:MySQL 8.0+
-- 字符集:utf8mb4
-- ============================================================

DROP DATABASE IF EXISTS `bank_account`;
CREATE DATABASE `bank_account` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `bank_account`;

-- ------------------------------------------------------------
-- 1. 系统用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`        VARCHAR(32)  NOT NULL                COMMENT '用户名(登录名)',
    `password`        VARCHAR(128) NOT NULL                COMMENT '密码(BCrypt加密)',
    `real_name`       VARCHAR(32)  NOT NULL                COMMENT '真实姓名',
    `id_card`         VARCHAR(18)  NOT NULL                COMMENT '身份证号',
    `phone`           VARCHAR(20)  NOT NULL                COMMENT '手机号',
    `email`           VARCHAR(64)           DEFAULT NULL   COMMENT '邮箱',
    `status`          TINYINT      NOT NULL DEFAULT 1       COMMENT '状态:0-禁用,1-正常',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除:0-未删除,1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_phone` (`phone`),
    KEY `idx_id_card` (`id_card`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ------------------------------------------------------------
-- 2. 银行账户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `account`;
CREATE TABLE `account` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `account_no`      VARCHAR(20)     NOT NULL                COMMENT '银行账号(唯一)',
    `user_id`         BIGINT          NOT NULL                COMMENT '用户ID',
    `account_type`    TINYINT         NOT NULL                COMMENT '账户类型:1-活期,2-定期,3-理财',
    `balance`         DECIMAL(18,2)   NOT NULL DEFAULT 0.00   COMMENT '账户余额',
    `status`          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态:0-冻结,1-正常,2-销户',
    `currency`        VARCHAR(8)      NOT NULL DEFAULT 'CNY'  COMMENT '币种',
    `open_date`       DATE            NOT NULL                COMMENT '开户日期',
    `close_date`      DATE                     DEFAULT NULL   COMMENT '销户日期',
    `last_interest_date` DATE                  DEFAULT NULL   COMMENT '上次结息日期',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_account_no` (`account_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_account_type` (`account_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='银行账户表';

-- ------------------------------------------------------------
-- 3. 交易流水表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `transaction_record`;
CREATE TABLE `transaction_record` (
    `id`                BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `txn_no`            VARCHAR(32)     NOT NULL                COMMENT '交易流水号(唯一)',
    `account_id`        BIGINT          NOT NULL                COMMENT '账户ID',
    `account_no`        VARCHAR(20)     NOT NULL                COMMENT '账号(冗余,便于查询)',
    `txn_type`          TINYINT         NOT NULL                COMMENT '交易类型:1-存入,2-支取,3-转入,4-转出,5-利息,6-理财申购,7-理财赎回',
    `amount`            DECIMAL(18,2)   NOT NULL                COMMENT '交易金额',
    `balance_before`    DECIMAL(18,2)   NOT NULL                COMMENT '交易前余额',
    `balance_after`     DECIMAL(18,2)   NOT NULL                COMMENT '交易后余额',
    `counterparty_account` VARCHAR(20)           DEFAULT NULL   COMMENT '对方账号',
    `counterparty_name` VARCHAR(64)            DEFAULT NULL   COMMENT '对方户名',
    `remark`            VARCHAR(128)             DEFAULT NULL   COMMENT '备注',
    `txn_time`         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '交易时间',
    `create_time`      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_txn_no` (`txn_no`),
    KEY `idx_account_id` (`account_id`),
    KEY `idx_txn_time` (`txn_time`),
    KEY `idx_txn_type` (`txn_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易流水表';

-- ------------------------------------------------------------
-- 4. 理财产品表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `financial_product`;
CREATE TABLE `financial_product` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `product_code`    VARCHAR(32)     NOT NULL                COMMENT '产品代码',
    `product_name`    VARCHAR(64)     NOT NULL                COMMENT '产品名称',
    `product_type`    TINYINT         NOT NULL                COMMENT '产品类型:1-活期,2-定期,3-基金,4-债券',
    `annual_rate`     DECIMAL(6,4)    NOT NULL                COMMENT '年化收益率(如0.0450表示4.5%)',
    `min_amount`      DECIMAL(18,2)   NOT NULL DEFAULT 1000.00 COMMENT '最低申购金额',
    `increment_amount` DECIMAL(18,2)  NOT NULL DEFAULT 1000.00 COMMENT '递增金额',
    `term_days`       INT             NOT NULL DEFAULT 0      COMMENT '产品期限(天),0表示无固定期限',
    `risk_level`      TINYINT         NOT NULL DEFAULT 1       COMMENT '风险等级:1-低,2-中低,3-中,4-中高,5-高',
    `total_quota`     DECIMAL(18,2)   NOT NULL                COMMENT '产品总额度',
    `remaining_quota` DECIMAL(18,2)   NOT NULL                COMMENT '剩余额度',
    `raise_start_date` DATE           NOT NULL                COMMENT '募集开始日',
    `raise_end_date`  DATE            NOT NULL                COMMENT '募集结束日',
    `status`          TINYINT         NOT NULL DEFAULT 0      COMMENT '状态:0-待售,1-募集中,2-运作中,3-已结束',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`product_code`),
    KEY `idx_status` (`status`),
    KEY `idx_product_type` (`product_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='理财产品表';

-- ------------------------------------------------------------
-- 5. 理财持仓表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `product_holding`;
CREATE TABLE `product_holding` (
    `id`              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `holding_no`      VARCHAR(32)     NOT NULL                COMMENT '持仓编号',
    `user_id`         BIGINT          NOT NULL                COMMENT '用户ID',
    `product_id`      BIGINT          NOT NULL                COMMENT '产品ID',
    `account_id`      BIGINT          NOT NULL                COMMENT '扣款账户ID',
    `amount`          DECIMAL(18,2)   NOT NULL                COMMENT '持有金额',
    `expected_income` DECIMAL(18,2)            DEFAULT 0.00   COMMENT '预期收益',
    `actual_income`   DECIMAL(18,2)            DEFAULT 0.00   COMMENT '实际收益',
    `purchase_date`   DATE            NOT NULL                COMMENT '申购日期',
    `maturity_date`   DATE                     DEFAULT NULL   COMMENT '到期日期',
    `status`          TINYINT         NOT NULL DEFAULT 1       COMMENT '状态:1-持有中,2-已赎回,3-已到期',
    `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`         TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_holding_no` (`holding_no`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_product_id` (`product_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='理财持仓表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 用户数据(密码均为 BCrypt 加密的 "123456")
-- 实际生产中密码应通过注册接口生成
INSERT INTO `sys_user` (`username`,`password`,`real_name`,`id_card`,`phone`,`email`) VALUES
('zhangsan','$2a$10$7ec96oWfHL7nDOsRpejb7uda5WA3ALmj0rlTUSYzqN0Zd8pQEQDtW','张三','110101199001011234','13800138001','zhangsan@bank.com'),
('lisi','$2a$10$7ec96oWfHL7nDOsRpejb7uda5WA3ALmj0rlTUSYzqN0Zd8pQEQDtW','李四','110101199202022345','13800138002','lisi@bank.com'),
('wangwu','$2a$10$7ec96oWfHL7nDOsRpejb7uda5WA3ALmj0rlTUSYzqN0Zd8pQEQDtW','王五','110101199303033456','13800138003','wangwu@bank.com');

-- 账户数据
INSERT INTO `account` (`account_no`,`user_id`,`account_type`,`balance`,`open_date`) VALUES
('6222000011110001',1,1,50000.00,'2024-01-15'),
('6222000011110002',1,2,100000.00,'2024-01-15'),
('6222000022220001',2,1,35000.00,'2024-02-20'),
('6222000033330001',3,1,8000.00,'2024-03-10');

-- 理财产品数据
INSERT INTO `financial_product` (`product_code`,`product_name`,`product_type`,`annual_rate`,`min_amount`,`increment_amount`,`term_days`,`risk_level`,`total_quota`,`remaining_quota`,`raise_start_date`,`raise_end_date`,`status`) VALUES
('P2024001','稳健天天赢',1,0.0350,1000.00,1000.00,0,1,10000000.00,8500000.00,'2024-01-01','2024-12-31',2),
('P2024002','季季鑫定期90天',2,0.0450,10000.00,1000.00,90,2,5000000.00,3200000.00,'2024-01-01','2024-12-31',2),
('P2024003','年年丰365天',2,0.0520,50000.00,1000.00,365,3,3000000.00,2800000.00,'2024-01-01','2024-12-31',2),
('P2024004','成长混合基金',3,0.0750,1000.00,100.00,0,4,8000000.00,8000000.00,'2024-06-01','2024-09-30',1),
('P2024005','国债2024-1',4,0.0300,100.00,100.00,1095,1,10000000.00,10000000.00,'2024-04-01','2024-04-30',0);

-- ============================================================
-- 视图:用户账户一览
-- ============================================================
DROP VIEW IF EXISTS `v_user_account`;
CREATE VIEW `v_user_account` AS
SELECT
    u.id           AS user_id,
    u.username,
    u.real_name,
    u.phone,
    a.id           AS account_id,
    a.account_no,
    a.account_type,
    a.balance,
    a.status,
    a.open_date
FROM sys_user u
LEFT JOIN account a ON u.id = a.user_id AND a.deleted = 0
WHERE u.deleted = 0;
