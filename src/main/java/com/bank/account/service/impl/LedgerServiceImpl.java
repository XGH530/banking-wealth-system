package com.bank.account.service.impl;

import com.bank.account.common.BankConstants;
import com.bank.account.common.BankUtils;
import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.entity.Account;
import com.bank.account.entity.AccountEntry;
import com.bank.account.entity.TransactionRecord;
import com.bank.account.mapper.AccountEntryMapper;
import com.bank.account.mapper.AccountMapper;
import com.bank.account.mapper.TransactionRecordMapper;
import com.bank.account.service.LedgerService;
import com.bank.account.vo.PageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 账务服务实现
 *
 * 记账规则(客户存款在银行是负债,所以客户余额增加记在贷方):
 *   存入/赎回 → 借 内部账户, 贷 客户账户
 *   支取/申购 → 借 客户账户, 贷 内部账户
 *   转账      → 借 转出账户, 贷 转入账户
 * 每条业务固定两条分录,金额相等、方向相反 —— 这是试算平衡能成立的根本原因。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerServiceImpl implements LedgerService {

    private final AccountEntryMapper entryMapper;
    private final AccountMapper accountMapper;
    private final TransactionRecordMapper txnMapper;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void recordDeposit(Account account, BigDecimal amount, String txnNo, String remark) {
        post(txnNo, BankConstants.TXN_DEPOSIT, null, account, amount, LocalDateTime.now(), remark);
    }

    @Override
    public void recordWithdraw(Account account, BigDecimal amount, String txnNo, String remark) {
        post(txnNo, BankConstants.TXN_WITHDRAW, account, null, amount, LocalDateTime.now(), remark);
    }

    @Override
    public void recordTransfer(Account from, Account to, BigDecimal amount, String txnNo, String remark) {
        post(txnNo, BankConstants.TXN_TRANSFER_OUT, from, to, amount, LocalDateTime.now(), remark);
    }

    @Override
    public void recordProductPurchase(Account account, BigDecimal amount, String txnNo, String remark) {
        post(txnNo, BankConstants.TXN_PRODUCT_PURCHASE, account, null, amount, LocalDateTime.now(), remark);
    }

    @Override
    public void recordProductRedeem(Account account, BigDecimal amount, String txnNo, String remark) {
        post(txnNo, BankConstants.TXN_PRODUCT_REDEEM, null, account, amount, LocalDateTime.now(), remark);
    }

    @Override
    public Map<String, Object> trialBalance(LocalDate startDate, LocalDate endDate) {
        LocalDate from = startDate == null ? LocalDate.now().minusYears(20) : startDate;
        LocalDate to = endDate == null ? LocalDate.now() : endDate;
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();

        Map<String, Object> row = entryMapper.selectTrialBalance(start, end);
        Map<String, Object> result = new LinkedHashMap<>();
        BigDecimal debit = toDecimal(row == null ? null : row.get("debitTotal"));
        BigDecimal credit = toDecimal(row == null ? null : row.get("creditTotal"));
        BigDecimal diff = debit.subtract(credit).setScale(2, java.math.RoundingMode.HALF_UP);

        result.put("startDate", from.format(DATE));
        result.put("endDate", to.format(DATE));
        result.put("debitTotal", debit);
        result.put("creditTotal", credit);
        result.put("difference", diff);
        result.put("balanced", diff.abs().compareTo(new BigDecimal("0.01")) < 0);
        result.put("entryCount", row == null ? 0 : row.get("entryCount"));
        return result;
    }

    @Override
    public Map<String, Object> reconcile() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("checkedAccounts", entryMapper.countAccounts());
        result.put("unbalancedCount", entryMapper.countUnbalancedAccounts());
        result.put("totalBalance", entryMapper.sumBalance());
        result.put("ledgerNet", entryMapper.sumLedgerNet());

        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> row : entryMapper.selectUnbalancedAccounts(100)) {
            BigDecimal balance = toDecimal(row.get("balance"));
            BigDecimal net = toDecimal(row.get("ledgerNet"));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("accountNo", row.get("accountNo"));
            m.put("balance", balance);
            m.put("ledgerNet", net);
            m.put("difference", balance.subtract(net));
            m.put("entryCount", row.get("entryCount"));
            items.add(m);
        }
        result.put("items", items);
        result.put("serverTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        return result;
    }

    @Override
    public PageVO<Map<String, Object>> pageEntries(long current, long size, String accountNo,
                                                   String txnNo, Integer direction) {
        LambdaQueryWrapper<AccountEntry> wrapper = new LambdaQueryWrapper<>();
        if (accountNo != null && !accountNo.isBlank()) {
            wrapper.eq(AccountEntry::getAccountNo, accountNo);
        }
        if (txnNo != null && !txnNo.isBlank()) {
            wrapper.eq(AccountEntry::getTxnNo, txnNo);
        }
        if (direction != null) {
            wrapper.eq(AccountEntry::getDirection, direction);
        }
        wrapper.orderByDesc(AccountEntry::getEntryTime).orderByDesc(AccountEntry::getId);

        Page<AccountEntry> page = entryMapper.selectPage(new Page<>(current, size), wrapper);
        List<Map<String, Object>> records = new ArrayList<>();
        for (AccountEntry e : page.getRecords()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", e.getId());
            m.put("entryNo", e.getEntryNo());
            m.put("txnNo", e.getTxnNo());
            m.put("accountNo", e.getAccountNo());
            m.put("direction", e.getDirection());
            m.put("directionText", e.getDirection() != null && e.getDirection() == BankConstants.ENTRY_DEBIT ? "借" : "贷");
            m.put("amount", e.getAmount());
            m.put("balanceAfter", e.getBalanceAfter());
            m.put("bizType", e.getBizType());
            m.put("bizTypeText", bizTypeText(e.getBizType()));
            m.put("entryTime", e.getEntryTime() == null ? "" : e.getEntryTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            m.put("remark", e.getRemark() == null ? "" : e.getRemark());
            m.put("reverseOf", e.getReverseOf() == null ? "" : e.getReverseOf());
            m.put("isReversed", e.getIsReversed());
            records.add(m);
        }
        return PageVO.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reverse(String txnNo, String reason) {
        List<AccountEntry> entries = entryMapper.selectList(
                new LambdaQueryWrapper<AccountEntry>().eq(AccountEntry::getTxnNo, txnNo));
        if (entries.isEmpty()) {
            throw new BusinessException(ResultCode.NOT_FOUND, "该流水没有对应的会计分录，无法冲正");
        }
        boolean alreadyReversed = entries.stream()
                .anyMatch(e -> e.getIsReversed() != null && e.getIsReversed() == 1);
        if (alreadyReversed) {
            log.info("流水 {} 已冲正过，忽略重复请求", txnNo);
            return false;
        }

        String reverseTxnNo = BankUtils.generateTxnNo();
        LocalDateTime now = LocalDateTime.now();
        String remark = "冲正(原流水 " + txnNo + ")" + (reason == null || reason.isBlank() ? "" : "：" + reason);

        for (AccountEntry origin : entries) {
            boolean internal = origin.getAccountId() == null
                    || origin.getAccountId() == BankConstants.SYSTEM_ACCOUNT_ID;
            Account account = null;
            BigDecimal balanceAfter = BigDecimal.ZERO;

            if (!internal) {
                Account current = accountMapper.selectById(origin.getAccountId());
                if (current == null) {
                    throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
                }
                // 原分录是贷(余额增加)→ 冲正要扣回;原分录是借(余额减少)→ 冲正要补回
                int rows = origin.getDirection() == BankConstants.ENTRY_CREDIT
                        ? accountMapper.decreaseBalance(current.getId(), origin.getAmount())
                        : accountMapper.increaseBalance(current.getId(), origin.getAmount());
                if (rows == 0) {
                    throw new BusinessException(ResultCode.BALANCE_NOT_ENOUGH,
                            "冲正失败：账户 " + current.getAccountNo() + " 余额不足，无法扣回");
                }
                account = accountMapper.selectById(current.getId());
                balanceAfter = account.getBalance();

                // 客户视角同样留一条红冲流水,便于在流水查询里看到
                TransactionRecord reversal = new TransactionRecord();
                reversal.setTxnNo(reverseTxnNo);
                reversal.setAccountId(account.getId());
                reversal.setAccountNo(account.getAccountNo());
                reversal.setTxnType(BankConstants.TXN_REVERSE);
                reversal.setAmount(origin.getAmount());
                boolean creditOrigin = origin.getDirection() == BankConstants.ENTRY_CREDIT;
                reversal.setBalanceBefore(creditOrigin
                        ? balanceAfter.add(origin.getAmount())
                        : balanceAfter.subtract(origin.getAmount()));
                reversal.setBalanceAfter(balanceAfter);
                reversal.setRemark(remark);
                reversal.setTxnTime(now);
                txnMapper.insert(reversal);
            }

            insertRawEntry(generateEntryNo(), reverseTxnNo,
                    origin.getAccountId(), origin.getAccountNo(),
                    origin.getDirection() == BankConstants.ENTRY_DEBIT
                            ? BankConstants.ENTRY_CREDIT : BankConstants.ENTRY_DEBIT,
                    origin.getAmount(), balanceAfter, BankConstants.TXN_REVERSE, now, remark, txnNo);
        }

        // 原分录只标记"已冲正",金额与方向一律不动(审计要求:账不可改)
        AccountEntry mark = new AccountEntry();
        mark.setIsReversed(1);
        entryMapper.update(mark, new LambdaUpdateWrapper<AccountEntry>()
                .eq(AccountEntry::getTxnNo, txnNo));

        log.warn("冲正完成: 原流水={} 冲正流水={} 原因={}", txnNo, reverseTxnNo, reason);
        return true;
    }

    // ------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------

    /** 一笔业务写两条分录:借一条、贷一条 */
    private void post(String txnNo, int bizType, Account debit, Account credit,
                      BigDecimal amount, LocalDateTime time, String remark) {
        insertEntry(txnNo, bizType, debit, BankConstants.ENTRY_DEBIT, amount, time, remark);
        insertEntry(txnNo, bizType, credit, BankConstants.ENTRY_CREDIT, amount, time, remark);
    }

    private void insertEntry(String txnNo, int bizType, Account account, int direction,
                             BigDecimal amount, LocalDateTime time, String remark) {
        insertRawEntry(generateEntryNo(), txnNo,
                account == null ? BankConstants.SYSTEM_ACCOUNT_ID : account.getId(),
                account == null ? BankConstants.SYSTEM_ACCOUNT_NO : account.getAccountNo(),
                direction, amount,
                account == null ? BigDecimal.ZERO : account.getBalance(),
                bizType, time, remark, null);
    }

    private void insertRawEntry(String entryNo, String txnNo, Long accountId, String accountNo,
                                int direction, BigDecimal amount, BigDecimal balanceAfter,
                                int bizType, LocalDateTime time, String remark, String reverseOf) {
        AccountEntry entry = new AccountEntry();
        entry.setEntryNo(entryNo);
        entry.setTxnNo(txnNo);
        entry.setAccountId(accountId);
        entry.setAccountNo(accountNo);
        entry.setDirection(direction);
        entry.setAmount(amount);
        entry.setBalanceAfter(balanceAfter == null ? BigDecimal.ZERO : balanceAfter);
        entry.setBizType(bizType);
        entry.setEntryTime(time);
        entry.setRemark(remark);
        entry.setReverseOf(reverseOf);
        entry.setIsReversed(0);
        entry.setCreateTime(LocalDateTime.now());
        entryMapper.insert(entry);
    }

    private String generateEntryNo() {
        return BankConstants.ENTRY_NO_PREFIX + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private static String bizTypeText(Integer bizType) {
        if (bizType == null) return "-";
        return switch (bizType) {
            case BankConstants.BIZ_OPENING -> "期初余额";
            case BankConstants.TXN_DEPOSIT -> "存入";
            case BankConstants.TXN_WITHDRAW -> "支取";
            case BankConstants.TXN_TRANSFER_IN -> "转入";
            case BankConstants.TXN_TRANSFER_OUT -> "转出";
            case BankConstants.TXN_INTEREST -> "利息";
            case BankConstants.TXN_PRODUCT_PURCHASE -> "理财申购";
            case BankConstants.TXN_PRODUCT_REDEEM -> "理财赎回";
            case BankConstants.TXN_REVERSE -> "冲正";
            default -> "其他";
        };
    }

    private static BigDecimal toDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(String.valueOf(value));
    }
}