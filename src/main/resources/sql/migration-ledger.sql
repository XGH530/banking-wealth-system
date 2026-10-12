-- ============================================================
-- 复式记账 + 请求幂等 迁移脚本
--   适用: 已有数据的 bank_account 库(MySQL 5.7 / 8.0 均可)
--   执行: mysql -uroot -p bank_account < migration-ledger.sql
--   幂等: 可重复执行(建表用 IF NOT EXISTS,补期初用 NOT EXISTS 校验)
-- ============================================================

-- ------------------------------------------------------------
-- 1. 会计分录表(复式记账明细账)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `account_entry` (
    `id`            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `entry_no`      VARCHAR(32)   NOT NULL                COMMENT '分录号(唯一)',
    `txn_no`        VARCHAR(32)   NOT NULL                COMMENT '关联业务流水号(同一笔业务的借贷两条分录相同)',
    `account_id`    BIGINT        NOT NULL                COMMENT '账户ID(0 表示银行内部账户)',
    `account_no`    VARCHAR(20)   NOT NULL                COMMENT '账号(冗余)',
    `direction`     TINYINT       NOT NULL                COMMENT '方向:1-借 2-贷',
    `amount`        DECIMAL(18,2) NOT NULL                COMMENT '金额(正数,方向由 direction 表示)',
    `balance_after` DECIMAL(18,2) NOT NULL DEFAULT 0.00   COMMENT '该腿记账后账户余额(内部账户为 0)',
    `biz_type`      TINYINT       NOT NULL                COMMENT '业务类型:0-期初 1-存入 2-支取 3-转入 4-转出 5-利息 6-申购 7-赎回 8-冲正',
    `entry_time`    DATETIME      NOT NULL                COMMENT '记账时间',
    `remark`        VARCHAR(255)           DEFAULT NULL   COMMENT '摘要',
    `reverse_of`    VARCHAR(32)            DEFAULT NULL   COMMENT '冲正关系:本分录是冲正哪一笔流水产生的',
    `is_reversed`   TINYINT       NOT NULL DEFAULT 0      COMMENT '原分录是否已被冲正:0-否 1-是',
    `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_entry_no` (`entry_no`),
    KEY `idx_txn_no` (`txn_no`),
    KEY `idx_account_time` (`account_id`, `entry_time`),
    KEY `idx_entry_time` (`entry_time`),
    KEY `idx_biz_type` (`biz_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会计分录表(复式记账)';

-- ------------------------------------------------------------
-- 2. 幂等记录表(请求级去重)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `idempotent_record` (
    `idem_key`    VARCHAR(64) NOT NULL                COMMENT '幂等键(客户端生成的唯一请求ID)',
    `biz_type`    VARCHAR(32) NOT NULL                COMMENT '业务类型:DEPOSIT/WITHDRAW/TRANSFER',
    `txn_no`      VARCHAR(32)          DEFAULT NULL   COMMENT '首次执行生成的流水号(重放时用它找回结果)',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`idem_key`),
    KEY `idx_txn_no` (`txn_no`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请求幂等记录表';

-- ------------------------------------------------------------
-- 3. 为存量账户补"期初余额"分录
--    记账规则:客户存款是银行负债,余额增加记贷方,所以期初余额记贷方
--    补了这一步,「账户余额 = Σ贷 - Σ借」这个等式对存量数据也成立,
--    之后的余额对账才有意义(否则历史数据全是"不平")。
-- ------------------------------------------------------------
-- 注意:期初余额同样要"两条腿"——借 内部账户 / 贷 客户账户,
-- 只写客户一侧的话试算平衡立刻就不相等了(这是复式记账最容易踩的坑)。
INSERT INTO `account_entry`
    (`entry_no`, `txn_no`, `account_id`, `account_no`, `direction`, `amount`, `balance_after`,
     `biz_type`, `entry_time`, `remark`, `is_reversed`)
SELECT CONCAT('EO', a.id), CONCAT('OPEN', a.id), a.id, a.account_no,
       2, a.balance, a.balance, 0, a.open_date, '期初余额(迁移补录)', 0
FROM `account` a
WHERE a.deleted = 0
  AND a.balance > 0
  AND NOT EXISTS (
      SELECT 1 FROM `account_entry` e WHERE e.account_id = a.id AND e.biz_type = 0
  );

-- 期初的另一条腿:借 银行内部账户
INSERT INTO `account_entry`
    (`entry_no`, `txn_no`, `account_id`, `account_no`, `direction`, `amount`, `balance_after`,
     `biz_type`, `entry_time`, `remark`, `is_reversed`)
SELECT CONCAT('EOS', a.id), CONCAT('OPEN', a.id), 0, 'SYSTEM',
       1, a.balance, 0, 0, a.open_date, '期初余额-内部账户(迁移补录)', 0
FROM `account` a
WHERE a.deleted = 0
  AND a.balance > 0
  AND EXISTS (SELECT 1 FROM `account_entry` e WHERE e.txn_no = CONCAT('OPEN', a.id) AND e.direction = 2)
  AND NOT EXISTS (SELECT 1 FROM `account_entry` e2 WHERE e2.txn_no = CONCAT('OPEN', a.id) AND e2.direction = 1);

-- ------------------------------------------------------------
-- 4. 执行结果自检
-- ------------------------------------------------------------
SELECT 'account_entry'  AS tbl, COUNT(*) AS rows_now FROM `account_entry`
UNION ALL
SELECT 'idempotent_record', COUNT(*) FROM `idempotent_record`;