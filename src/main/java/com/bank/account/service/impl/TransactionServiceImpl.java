package com.bank.account.service.impl;

import com.bank.account.common.BankConstants;
import com.bank.account.common.BankUtils;
import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.dto.DepositWithdrawDTO;
import com.bank.account.dto.TransactionQueryDTO;
import com.bank.account.dto.TransferDTO;
import com.bank.account.entity.Account;
import com.bank.account.entity.TransactionRecord;
import com.bank.account.mapper.AccountMapper;
import com.bank.account.mapper.TransactionRecordMapper;
import com.bank.account.service.TransactionService;
import com.bank.account.vo.AccountVO;
import com.bank.account.vo.PageVO;
import com.bank.account.vo.TransactionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 交易服务实现
 * 存取款/转账/流水查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final AccountMapper accountMapper;
    private final TransactionRecordMapper txnMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${bank.transfer-limit:100000}")
    private BigDecimal transferLimit;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionVO deposit(DepositWithdrawDTO dto) {
        Account account = requireNormalAccount(dto.getAccountNo());

        // 通过 SQL 增加余额并返回受影响行数,确保原子性
        int rows = accountMapper.increaseBalance(account.getId(), dto.getAmount());
        if (rows == 0) {
            throw new BusinessException(ResultCode.DB_OPERATION_FAILED);
        }

        // 重新查询最新余额
        Account refreshed = accountMapper.selectById(account.getId());
        TransactionRecord record = buildRecord(
                refreshed, BankConstants.TXN_DEPOSIT, dto.getAmount(),
                null, null, dto.getRemark());
        txnMapper.insert(record);

        evictAccountCache(account.getAccountNo());
        log.info("存入成功: accountNo={}, amount={}, txnNo={}",
                account.getAccountNo(), dto.getAmount(), record.getTxnNo());

        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionVO withdraw(DepositWithdrawDTO dto) {
        Account account = requireNormalAccount(dto.getAccountNo());

        // 余额校验 + 扣减(乐观锁在 SQL 中保证)
        int rows = accountMapper.decreaseBalance(account.getId(), dto.getAmount());
        if (rows == 0) {
            throw new BusinessException(ResultCode.BALANCE_NOT_ENOUGH);
        }

        Account refreshed = accountMapper.selectById(account.getId());
        TransactionRecord record = buildRecord(
                refreshed, BankConstants.TXN_WITHDRAW, dto.getAmount(),
                null, null, dto.getRemark());
        txnMapper.insert(record);

        evictAccountCache(account.getAccountNo());
        log.info("支取成功: accountNo={}, amount={}, txnNo={}",
                account.getAccountNo(), dto.getAmount(), record.getTxnNo());

        return toVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionVO transfer(TransferDTO dto) {
        // 同账号校验
        if (dto.getFromAccountNo().equals(dto.getToAccountNo())) {
            throw new BusinessException(ResultCode.TRANSFER_SAME_ACCOUNT);
        }
        // 单笔限额
        if (dto.getAmount().compareTo(transferLimit) > 0) {
            throw new BusinessException(ResultCode.TRANSFER_LIMIT_EXCEEDED);
        }

        Account from = requireNormalAccount(dto.getFromAccountNo());
        Account to   = requireNormalAccount(dto.getToAccountNo());

        // 对方户名校验(如果传入)
        if (dto.getCounterpartyName() != null && !dto.getCounterpartyName().isBlank()
                && !dto.getCounterpartyName().equals(to.getUserId() == null ? "" : to.getUserId().toString())) {
            // 简化校验:用户表里查询对方账号对应姓名
            AccountVO toVO = accountMapper.selectVOByAccountNo(dto.getToAccountNo());
            if (toVO != null && !dto.getCounterpartyName().equals(toVO.getRealName())) {
                throw new BusinessException(ResultCode.BAD_REQUEST, "对方户名不匹配");
            }
        }

        // 1. 扣减转出账户余额
        int rows = accountMapper.decreaseBalance(from.getId(), dto.getAmount());
        if (rows == 0) {
            throw new BusinessException(ResultCode.BALANCE_NOT_ENOUGH);
        }

        // 2. 增加转入账户余额
        rows = accountMapper.increaseBalance(to.getId(), dto.getAmount());
        if (rows == 0) {
            // 此处理论上不会失败,但如果失败则事务回滚
            throw new BusinessException(ResultCode.DB_OPERATION_FAILED, "转入失败");
        }

        // 3. 写入两条流水(转出 + 转入)
        Account fromRefreshed = accountMapper.selectById(from.getId());
        Account toRefreshed   = accountMapper.selectById(to.getId());

        TransactionRecord out = buildRecord(
                fromRefreshed, BankConstants.TXN_TRANSFER_OUT, dto.getAmount(),
                dto.getToAccountNo(), null, dto.getRemark());
        txnMapper.insert(out);

        TransactionRecord in = buildRecord(
                toRefreshed, BankConstants.TXN_TRANSFER_IN, dto.getAmount(),
                dto.getFromAccountNo(), null, dto.getRemark());
        txnMapper.insert(in);

        // 4. 清除两边缓存
        evictAccountCache(from.getAccountNo());
        evictAccountCache(to.getAccountNo());

        log.info("转账成功: from={}, to={}, amount={}, txnNo={}",
                from.getAccountNo(), to.getAccountNo(), dto.getAmount(), out.getTxnNo());

        return toVO(out);
    }

    @Override
    public PageVO<TransactionVO> page(TransactionQueryDTO query) {
        Page<TransactionVO> p = new Page<>(query.getPage(), query.getSize());
        IPage<TransactionVO> result = txnMapper.selectVOPage(
                p, null, query.getAccountNo(), query.getTxnType(),
                query.getStartDate(), query.getEndDate());

        List<TransactionVO> records = result.getRecords();
        records.forEach(BankUtils::fillTxnDesc);
        return PageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    // ------------------------------------------------------------
    // 内部工具方法
    // ------------------------------------------------------------

    private Account requireNormalAccount(String accountNo) {
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getAccountNo, accountNo));
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        }
        if (account.getStatus() == BankConstants.ACCOUNT_STATUS_FROZEN) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN);
        }
        if (account.getStatus() == BankConstants.ACCOUNT_STATUS_CLOSED) {
            throw new BusinessException(ResultCode.ACCOUNT_CLOSED);
        }
        return account;
    }

    private TransactionRecord buildRecord(Account account, int txnType, BigDecimal amount,
                                          String counterpartyAccount, String counterpartyName, String remark) {
        TransactionRecord record = new TransactionRecord();
        record.setTxnNo(BankUtils.generateTxnNo());
        record.setAccountId(account.getId());
        record.setAccountNo(account.getAccountNo());
        record.setTxnType(txnType);
        record.setAmount(amount);
        // 交易前余额 = 当前余额 - amount(若是入账则是 -amount 的相反操作)
        BigDecimal before = switch (txnType) {
            case BankConstants.TXN_DEPOSIT, BankConstants.TXN_TRANSFER_IN, BankConstants.TXN_PRODUCT_REDEEM, BankConstants.TXN_INTEREST
                    -> account.getBalance().subtract(amount);
            default -> account.getBalance().add(amount);
        };
        record.setBalanceBefore(before);
        record.setBalanceAfter(account.getBalance());
        record.setCounterpartyAccount(counterpartyAccount);
        record.setCounterpartyName(counterpartyName);
        record.setRemark(remark);
        record.setTxnTime(LocalDateTime.now());
        return record;
    }

    private TransactionVO toVO(TransactionRecord record) {
        TransactionVO vo = new TransactionVO();
        vo.setId(record.getId());
        vo.setTxnNo(record.getTxnNo());
        vo.setAccountNo(record.getAccountNo());
        vo.setTxnType(record.getTxnType());
        vo.setAmount(record.getAmount());
        vo.setBalanceBefore(record.getBalanceBefore());
        vo.setBalanceAfter(record.getBalanceAfter());
        vo.setCounterpartyAccount(record.getCounterpartyAccount());
        vo.setCounterpartyName(record.getCounterpartyName());
        vo.setRemark(record.getRemark());
        vo.setTxnTime(record.getTxnTime());
        BankUtils.fillTxnDesc(vo);
        return vo;
    }

    private void evictAccountCache(String accountNo) {
        try {
            redisTemplate.delete(BankConstants.CACHE_ACCOUNT_PREFIX + accountNo);
        } catch (Exception e) {
            log.warn("删除账户缓存失败: {}", e.getMessage());
        }
    }
}
