package com.bank.account.service;

import com.bank.account.entity.Account;
import com.bank.account.vo.PageVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 账务服务(复式记账 / 试算平衡 / 余额对账 / 冲正)
 *
 * 【设计说明】
 * 资金类业务只做"改余额 + 记流水"是不够的,那是记账 App;
 * 真正的账务系统要有三件东西:
 *   1. 复式记账 —— 每笔业务借贷两条腿,借贷必相等(可自动校验)
 *   2. 对账     —— 用分录反推余额,和余额字段对撞,发现漏记/重复记账
 *   3. 冲正     —— 错账不删不改,用反向分录红冲,保留完整审计轨迹
 * 这个 Service 就是这三件事的落点,业务层(存取款/转账/理财)只负责调用。
 */
public interface LedgerService {

    /** 存入:借 内部账户,贷 客户账户(客户余额增加) */
    void recordDeposit(Account account, BigDecimal amount, String txnNo, String remark);

    /** 支取:借 客户账户(客户余额减少),贷 内部账户 */
    void recordWithdraw(Account account, BigDecimal amount, String txnNo, String remark);

    /** 转账:借 转出账户,贷 转入账户(银行负债内部转移) */
    void recordTransfer(Account from, Account to, BigDecimal amount, String txnNo, String remark);

    /** 理财申购:借 客户账户,贷 内部理财归集账户 */
    void recordProductPurchase(Account account, BigDecimal amount, String txnNo, String remark);

    /** 理财赎回:借 内部理财归集账户,贷 客户账户 */
    void recordProductRedeem(Account account, BigDecimal amount, String txnNo, String remark);

    /** 试算平衡:Σ借 与 Σ贷 是否相等 */
    Map<String, Object> trialBalance(LocalDate startDate, LocalDate endDate);

    /** 余额对账:账户余额 vs 分录净额 */
    Map<String, Object> reconcile();

    /** 分录分页查询 */
    PageVO<Map<String, Object>> pageEntries(long current, long size, String accountNo,
                                           String txnNo, Integer direction);

    /**
     * 冲正(红冲):为原流水生成方向相反的分录并回滚余额,原分录只标记不修改
     *
     * @return true-冲正成功;false-该流水此前已被冲正过(幂等)
     */
    boolean reverse(String txnNo, String reason);
}