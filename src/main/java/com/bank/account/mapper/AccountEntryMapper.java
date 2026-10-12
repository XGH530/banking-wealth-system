package com.bank.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.account.entity.AccountEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 会计分录 Mapper
 */
@Mapper
public interface AccountEntryMapper extends BaseMapper<AccountEntry> {

    /**
     * 试算平衡:统计区间内所有分录的借贷合计
     * 【期望】debitTotal == creditTotal,且 entryCount 为偶数(每笔业务两条腿)
     */
    @Select("SELECT IFNULL(SUM(CASE WHEN direction = 1 THEN amount ELSE 0 END), 0) AS debitTotal, "
            + "       IFNULL(SUM(CASE WHEN direction = 2 THEN amount ELSE 0 END), 0) AS creditTotal, "
            + "       COUNT(*) AS entryCount "
            + "FROM account_entry "
            + "WHERE entry_time >= #{start} AND entry_time < #{end}")
    Map<String, Object> selectTrialBalance(@Param("start") LocalDateTime start,
                                           @Param("end") LocalDateTime end);

    /**
     * 余额对账:找出「账户余额 != Σ贷 - Σ借」的账户
     *
     * 【为什么这个查询有价值】它不信任应用层的记账代码,
     * 而是用分录(账)反推余额,和余额字段(实)直接对撞。
     * 只要有一天有人漏写分录、重复写分录、或绕过服务层改余额,这里立刻暴露。
     */
    @Select("SELECT a.account_no AS accountNo, a.balance AS balance, "
            + "       IFNULL(SUM(CASE WHEN e.direction = 2 THEN e.amount ELSE -e.amount END), 0) AS ledgerNet, "
            + "       COUNT(e.id) AS entryCount "
            + "FROM account a "
            + "LEFT JOIN account_entry e ON e.account_id = a.id "
            + "WHERE a.deleted = 0 "
            + "GROUP BY a.id, a.account_no, a.balance "
            + "HAVING ABS(a.balance - ledgerNet) > 0.001 "
            + "ORDER BY ABS(a.balance - ledgerNet) DESC "
            + "LIMIT #{limit}")
    List<Map<String, Object>> selectUnbalancedAccounts(@Param("limit") int limit);

    /** 不平账户数量(不受 LIMIT 影响,用于总览统计) */
    @Select("SELECT COUNT(*) FROM ("
            + "  SELECT a.id FROM account a "
            + "  LEFT JOIN account_entry e ON e.account_id = a.id "
            + "  WHERE a.deleted = 0 "
            + "  GROUP BY a.id, a.balance "
            + "  HAVING ABS(a.balance - IFNULL(SUM(CASE WHEN e.direction = 2 THEN e.amount ELSE -e.amount END), 0)) > 0.001"
            + ") t")
    Long countUnbalancedAccounts();

    /** 全部分录的净额合计(Σ贷 - Σ借),理论上应等于账户余额合计 */
    @Select("SELECT IFNULL(SUM(CASE WHEN direction = 2 THEN amount ELSE -amount END), 0) FROM account_entry")
    java.math.BigDecimal sumLedgerNet();

    /** 参与对账的账户总数 */
    @Select("SELECT COUNT(*) FROM account WHERE deleted = 0")
    Long countAccounts();

    /** 全部账户的余额合计(用于对账总览) */
    @Select("SELECT IFNULL(SUM(balance), 0) FROM account WHERE deleted = 0")
    java.math.BigDecimal sumBalance();
}