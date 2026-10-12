package com.bank.account.service.impl;

import com.bank.account.common.BankConstants;
import com.bank.account.common.BankUtils;
import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.dto.DepositWithdrawDTO;
import com.bank.account.dto.TransactionQueryDTO;
import com.bank.account.dto.TransferDTO;
import com.bank.account.entity.Account;
import com.bank.account.entity.IdempotentRecord;
import com.bank.account.entity.TransactionRecord;
import com.bank.account.mapper.AccountMapper;
import com.bank.account.mapper.IdempotentRecordMapper;
import com.bank.account.mapper.TransactionRecordMapper;
import com.bank.account.service.LedgerService;
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
import org.springframework.dao.DuplicateKeyException;
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
    private final IdempotentRecordMapper idemMapper;
    private final LedgerService ledgerService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${bank.transfer-limit:100000}")
    private BigDecimal transferLimit;

    @Override
    public TransactionVO deposit(DepositWithdrawDTO dto) {
        return deposit(dto, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionVO deposit(DepositWithdrawDTO dto, String requestId) {
        // 幂等:同一个 requestId 重复提交(网络重试/用户连点)只入账一次
        IdempotentRecord idem = beginIdempotent(requestId, "DEPOSIT");
        if (idem != null) {
            return replay(idem);
        }
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

        // 复式记账:借 内部账户 / 贷 客户账户(与余额变更在同一事务里,要么都成功要么都回滚)
        ledgerService.recordDeposit(refreshed, dto.getAmount(), record.getTxnNo(), dto.getRemark());
        finishIdempotent(requestId, record.getTxnNo());

        evictAccountCache(account.getAccountNo());
        log.info("存入成功: accountNo={}, amount={}, txnNo={}",
                account.getAccountNo(), dto.getAmount(), record.getTxnNo());

        return toVO(record);
    }

    @Override
    public TransactionVO withdraw(DepositWithdrawDTO dto) {
        return withdraw(dto, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionVO withdraw(DepositWithdrawDTO dto, String requestId) {
        IdempotentRecord idem = beginIdempotent(requestId, "WITHDRAW");
        if (idem != null) {
            return replay(idem);
        }
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

        // 复式记账:借 客户账户 / 贷 内部账户
        ledgerService.recordWithdraw(refreshed, dto.getAmount(), record.getTxnNo(), dto.getRemark());
        finishIdempotent(requestId, record.getTxnNo());

        evictAccountCache(account.getAccountNo());
        log.info("支取成功: accountNo={}, amount={}, txnNo={}",
                account.getAccountNo(), dto.getAmount(), record.getTxnNo());

        return toVO(record);
    }

    @Override
    public TransactionVO transfer(TransferDTO dto) {
        return transfer(dto, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TransactionVO transfer(TransferDTO dto, String requestId) {
        // 幂等:转账是"最怕重复"的资金接口,同一个 requestId 只会成功一次
        IdempotentRecord idem = beginIdempotent(requestId, "TRANSFER");
        if (idem != null) {
            return replay(idem);
        }
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

        // 3.5 复式记账:借 转出账户 / 贷 转入账户(同一笔业务两条腿,金额相等方向相反)
        ledgerService.recordTransfer(fromRefreshed, toRefreshed, dto.getAmount(), out.getTxnNo(), dto.getRemark());
        finishIdempotent(requestId, out.getTxnNo());

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

    // ------------------------------------------------------------
    // 幂等控制
    // ------------------------------------------------------------

    /**
     * 幂等开始
     *
     * @return null 表示首次执行(可以继续做业务);非 null 表示重复请求,需要重放首次结果
     *
     * 【实现要点】
     *   1. 先查(快路径):绝大多数重复请求在这里被拦下,不产生异常
     *   2. 再插(占位):idem_key 是主键,并发下数据库只允许一个成功
     *   3. 插入冲突说明别人先成功了 → 重新读一次记录做重放
     *   4. 业务失败时整个事务回滚,幂等记录一起消失,客户端可以用同一个 key 重试
     */
    private IdempotentRecord beginIdempotent(String requestId, String bizType) {
        if (requestId == null || requestId.isBlank()) {
            return null;
        }
        IdempotentRecord exist = idemMapper.selectById(requestId);
        if (exist != null) {
            return exist;
        }
        try {
            IdempotentRecord record = new IdempotentRecord();
            record.setIdemKey(requestId);
            record.setBizType(bizType);
            record.setCreateTime(LocalDateTime.now());
            idemMapper.insert(record);
            return null;
        } catch (DuplicateKeyException e) {
            log.info("幂等键 {} 并发冲突,转为重放首次结果", requestId);
            return idemMapper.selectById(requestId);
        }
    }

    /** 业务成功后回填流水号,供后续重放使用 */
    private void finishIdempotent(String requestId, String txnNo) {
        if (requestId == null || requestId.isBlank()) {
            return;
        }
        IdempotentRecord update = new IdempotentRecord();
        update.setIdemKey(requestId);
        update.setTxnNo(txnNo);
        idemMapper.updateById(update);
    }

    /** 重放:把首次执行的流水原样返回,不重复动账 */
    private TransactionVO replay(IdempotentRecord record) {
        if (record == null || record.getTxnNo() == null) {
            throw new BusinessException(ResultCode.CONFLICT, "重复请求：该请求正在处理中，请稍后查询结果");
        }
        TransactionRecord exist = txnMapper.selectOne(new LambdaQueryWrapper<TransactionRecord>()
                .eq(TransactionRecord::getTxnNo, record.getTxnNo()));
        if (exist == null) {
            throw new BusinessException(ResultCode.CONFLICT, "重复请求：首次交易结果不存在，请查询流水确认");
        }
        log.info("命中幂等键 {},重放流水 {}", record.getIdemKey(), record.getTxnNo());
        return toVO(exist);
    }

    private void evictAccountCache(String accountNo) {
        try {
            redisTemplate.delete(BankConstants.CACHE_ACCOUNT_PREFIX + accountNo);
        } catch (Exception e) {
            log.warn("删除账户缓存失败: {}", e.getMessage());
        }
    }
}
