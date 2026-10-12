-- ============================================================
-- 大规模造数脚本(用于验证索引、分页、聚合与对账在数据量下的表现)
--
-- 生成内容:
--   @user_count    个用户(sys_user) 与同名账户(account)，账号前缀 99
--   每账户一笔"期初余额"分录(account_entry, biz_type=0)
--   @txn_count     条交易流水(transaction_record) + 每条两条分录(account_entry)
--   @holding_count 条理财持仓(product_holding)
--   最后把账户余额重算为「期初 + Σ贷 - Σ借」，保证余额对账自洽
--
-- 执行方式:
--   mysql -uroot -p bank_account < seed-large-data.sql
--   (Navicat: 打开本文件 -> 全选执行，约 1-3 分钟)
--
-- 清理方式(见文件末尾注释):
--   所有生成数据都有特征前缀: 用户名 demo_ / 账号 99 / 流水号 T / 分录号 E
-- ============================================================

-- ---------------- 规模参数(按机器性能调整) ----------------
SET @user_count    = 100000;      -- 用户与账户数量
SET @txn_count     = 1000000;     -- 交易流水条数(会生成 2 倍数量的分录)
SET @holding_count = 5000;        -- 理财持仓条数
SET @open_balance  = 20000.00;    -- 每个新账户的期初余额
SET @days_back     = 180;         -- 流水时间跨度(天)

-- ---------------- 数字辅助表 1..1000000 ----------------
DROP TABLE IF EXISTS `tmp_seq`;
CREATE TABLE `tmp_seq` (`n` INT NOT NULL, PRIMARY KEY (`n`)) ENGINE=InnoDB;
INSERT INTO `tmp_seq` (`n`)
SELECT a.i + b.i*10 + c.i*100 + d.i*1000 + e.i*10000 + f.i*100000 + 1
FROM (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) a,
     (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) b,
     (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) c,
     (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) d,
     (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) e,
     (SELECT 0 i UNION ALL SELECT 1 UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
      UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9) f;

-- ---------------- 1. 用户 ----------------
INSERT INTO `sys_user` (`username`,`password`,`real_name`,`id_card`,`phone`,`email`,`status`,`role`)
SELECT CONCAT('demo_', s.n),
       '$2a$10$7ec96oWfHL7nDOsRpejb7uda5WA3ALmj0rlTUSYzqN0Zd8pQEQDtW',
       CONCAT('压测用户', s.n),
       CONCAT('1101011990', LPAD(s.n, 8, '0')),
       CONCAT('139', LPAD(s.n, 8, '0')),
       CONCAT('demo', s.n, '@bank.com'),
       1, 'USER'
FROM `tmp_seq` s
WHERE s.n <= @user_count;

-- ---------------- 2. 账户(账号 = 99 + 14 位序号) ----------------
INSERT INTO `account` (`account_no`,`user_id`,`account_type`,`balance`,`status`,`currency`,
                       `open_date`,`create_time`,`update_time`,`deleted`)
SELECT CONCAT('99', LPAD(s.n, 14, '0')), u.id, 1, @open_balance, 1, 'CNY',
       DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND()*900) DAY), NOW(), NOW(), 0
FROM `tmp_seq` s
JOIN `sys_user` u ON u.username = CONCAT('demo_', s.n)
WHERE s.n <= @user_count;

-- ---------------- 3. 期初余额分录(贷方) ----------------
-- 客户侧(贷) + 内部账户侧(借) 各一条,保证借贷平衡
INSERT INTO `account_entry` (`entry_no`,`txn_no`,`account_id`,`account_no`,`direction`,`amount`,
                             `balance_after`,`biz_type`,`entry_time`,`remark`,`is_reversed`)
SELECT CONCAT('EO', a.id), CONCAT('OPEN', a.id), a.id, a.account_no,
       2, @open_balance, @open_balance, 0, NOW(), '期初余额(造数)', 0
FROM `account` a
WHERE a.account_no LIKE '99%';

INSERT INTO `account_entry` (`entry_no`,`txn_no`,`account_id`,`account_no`,`direction`,`amount`,
                             `balance_after`,`biz_type`,`entry_time`,`remark`,`is_reversed`)
SELECT CONCAT('EOS', a.id), CONCAT('OPEN', a.id), 0, 'SYSTEM',
       1, @open_balance, 0, 0, NOW(), '期初余额-内部账户(造数)', 0
FROM `account` a
WHERE a.account_no LIKE '99%';

-- ---------------- 4. 交易流水 ----------------
-- 注意:这里用「乘法散列」而不是 RAND() 来挑账户。
-- 如果写成 ON a.account_no = CONCAT('99', LPAD(1+FLOOR(RAND()*@user_count),14,'0')),
-- 由于条件是非确定性的,同一行可能同时匹配到多个账户,生成重复流水号而报唯一键冲突。
INSERT INTO `transaction_record` (`txn_no`,`account_id`,`account_no`,`txn_type`,`amount`,
                                  `balance_before`,`balance_after`,`remark`,`txn_time`,`create_time`)
SELECT CONCAT('T', LPAD(s.n, 12, '0')), a.id, a.account_no,
       IF(s.n % 3 = 0, 2, 1),                                   -- 每三条里一条支取,其余存入
       ROUND(10 + RAND() * 990, 2),                             -- 金额随机(SELECT 列表里的 RAND() 每行只算一次)
       0, 0, '压测数据',
       DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * @days_back) DAY), NOW()
FROM `tmp_seq` s
JOIN `account` a
  ON a.account_no = CONCAT('99', LPAD(1 + MOD(s.n * 2654435761, @user_count), 14, '0'))
WHERE s.n <= @txn_count;

-- ---------------- 5. 流水分录(客户腿 + 内部账户腿) ----------------
INSERT INTO `account_entry` (`entry_no`,`txn_no`,`account_id`,`account_no`,`direction`,`amount`,
                             `balance_after`,`biz_type`,`entry_time`,`remark`,`is_reversed`)
SELECT CONCAT('E', t.id, 'C'), t.txn_no, t.account_id, t.account_no,
       IF(t.txn_type = 1, 2, 1), t.amount, 0, t.txn_type, t.txn_time, '历史导入', 0
FROM `transaction_record` t
WHERE t.txn_no LIKE 'T%';

INSERT INTO `account_entry` (`entry_no`,`txn_no`,`account_id`,`account_no`,`direction`,`amount`,
                             `balance_after`,`biz_type`,`entry_time`,`remark`,`is_reversed`)
SELECT CONCAT('E', t.id, 'S'), t.txn_no, 0, 'SYSTEM',
       IF(t.txn_type = 1, 1, 2), t.amount, 0, t.txn_type, t.txn_time, '历史导入', 0
FROM `transaction_record` t
WHERE t.txn_no LIKE 'T%';

-- ---------------- 6. 理财持仓 ----------------
INSERT INTO `product_holding` (`holding_no`,`user_id`,`product_id`,`account_id`,`amount`,
                               `expected_income`,`actual_income`,`purchase_date`,`maturity_date`,
                               `status`,`create_time`,`update_time`,`deleted`)
SELECT CONCAT('H', LPAD(s.n, 10, '0')), a.user_id, p.id, a.id,
       ROUND(1000 + RAND() * 20000, 2),
       ROUND(50 + RAND() * 800, 2), 0,
       DATE_SUB(CURDATE(), INTERVAL FLOOR(RAND()*180) DAY), NULL, 1, NOW(), NOW(), 0
FROM `tmp_seq` s
JOIN `account` a
  ON a.account_no = CONCAT('99', LPAD(1 + MOD(s.n * 7919, @user_count), 14, '0'))
JOIN `financial_product` p
  ON p.product_code = ELT(1 + MOD(s.n, 5), 'P2024001','P2024002','P2024003','P2024004','P2024005')
WHERE s.n <= @holding_count;

-- ---------------- 7. 用分录重算余额 ----------------
-- 余额必须严格等于「该账户所有分录的 Σ贷 - Σ借」。
-- 注意:期初余额本身就是一条贷方分录,已经包含在 net 里,不能再额外加一次 @open_balance,
--      否则每个账户都会多出 2 万,余额对账会全部不平(这里踩过一次)。
UPDATE `account` a
JOIN (
    SELECT e.account_id AS aid,
           SUM(IF(e.direction = 2, e.amount, -e.amount)) AS net
    FROM `account_entry` e
    JOIN `account` a2 ON a2.id = e.account_id AND a2.account_no LIKE '99%'
    GROUP BY e.account_id
) x ON x.aid = a.id
SET a.balance = x.net
WHERE a.account_no LIKE '99%';

DROP TABLE IF EXISTS `tmp_seq`;

-- ---------------- 8. 结果自检 ----------------
SELECT '用户' AS 项目, COUNT(*) AS 数量 FROM `sys_user` WHERE `username` LIKE 'demo_%'
UNION ALL SELECT '账户', COUNT(*) FROM `account` WHERE `account_no` LIKE '99%'
UNION ALL SELECT '流水', COUNT(*) FROM `transaction_record` WHERE `txn_no` LIKE 'T%'
UNION ALL SELECT '分录', COUNT(*) FROM `account_entry`
UNION ALL SELECT '持仓', COUNT(*) FROM `product_holding` WHERE `holding_no` LIKE 'H%';

-- 试算平衡: 两列必须相等
SELECT IFNULL(SUM(IF(direction = 1, amount, 0)), 0) AS 借方合计,
       IFNULL(SUM(IF(direction = 2, amount, 0)), 0) AS 贷方合计,
       COUNT(*) AS 分录条数
FROM `account_entry`;

-- 余额对账: 期望 0 行
SELECT COUNT(*) AS 不平账户数 FROM (
    SELECT a.id
    FROM `account` a
    LEFT JOIN `account_entry` e ON e.account_id = a.id
    WHERE a.deleted = 0
    GROUP BY a.id, a.balance
    HAVING ABS(a.balance - IFNULL(SUM(IF(e.direction = 2, e.amount, -e.amount)), 0)) > 0.001
) t;

-- ---------------- 清理(需要时手工执行) ----------------
-- DELETE FROM `account_entry` WHERE `entry_no` LIKE 'E%' OR `account_no` = 'SYSTEM';
-- DELETE FROM `transaction_record` WHERE `txn_no` LIKE 'T%';
-- DELETE FROM `product_holding` WHERE `holding_no` LIKE 'H%';
-- DELETE FROM `account` WHERE `account_no` LIKE '99%';
-- DELETE FROM `sys_user` WHERE `username` LIKE 'demo_%';