package com.bank.account.service.impl;

import com.bank.account.common.BankConstants;
import com.bank.account.common.BankUtils;
import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.common.UserContext;
import com.bank.account.dto.ProductPurchaseDTO;
import com.bank.account.dto.ProductRedeemDTO;
import com.bank.account.entity.Account;
import com.bank.account.entity.FinancialProduct;
import com.bank.account.entity.ProductHolding;
import com.bank.account.entity.TransactionRecord;
import com.bank.account.mapper.AccountMapper;
import com.bank.account.mapper.FinancialProductMapper;
import com.bank.account.mapper.ProductHoldingMapper;
import com.bank.account.mapper.TransactionRecordMapper;
import com.bank.account.service.HoldingService;
import com.bank.account.vo.HoldingVO;
import com.bank.account.vo.PageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 理财持仓服务实现
 * 申购 / 赎回 / 查询
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HoldingServiceImpl implements HoldingService {

    private final ProductHoldingMapper holdingMapper;
    private final FinancialProductMapper productMapper;
    private final AccountMapper accountMapper;
    private final TransactionRecordMapper txnMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 一年的天数(简化处理,银行一般按 360 或 365) */
    private static final BigDecimal YEAR_DAYS = new BigDecimal("365");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HoldingVO purchase(ProductPurchaseDTO dto) {
        Long userId = UserContext.getUserId();

        // 1. 校验产品
        FinancialProduct product = productMapper.selectOne(new LambdaQueryWrapper<FinancialProduct>()
                .eq(FinancialProduct::getProductCode, dto.getProductCode()));
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXISTS);
        }
        if (product.getStatus() != BankConstants.PRODUCT_STATUS_RAISING
                && product.getStatus() != BankConstants.PRODUCT_STATUS_RUNNING) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_AVAILABLE);
        }

        // 2. 申购金额校验
        BigDecimal amount = dto.getAmount();
        if (amount.compareTo(product.getMinAmount()) < 0) {
            throw new BusinessException(ResultCode.PURCHASE_AMOUNT_INVALID,
                    "申购金额不能少于 " + product.getMinAmount());
        }
        BigDecimal increment = product.getIncrementAmount();
        if (increment.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal diff = amount.subtract(product.getMinAmount());
            if (diff.remainder(increment).compareTo(BigDecimal.ZERO) != 0) {
                throw new BusinessException(ResultCode.PURCHASE_AMOUNT_INVALID,
                        "超出最低申购金额的部分须为 " + increment + " 的整数倍");
            }
        }
        if (amount.compareTo(product.getRemainingQuota()) > 0) {
            throw new BusinessException(ResultCode.PRODUCT_QUOTA_NOT_ENOUGH);
        }

        // 3. 校验扣款账户(必须是当前登录用户自己的活期账户)
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getAccountNo, dto.getAccountNo())
                .eq(Account::getUserId, userId));
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS, "扣款账号不存在或不属于当前用户");
        }
        if (account.getStatus() != BankConstants.ACCOUNT_STATUS_NORMAL) {
            throw new BusinessException(ResultCode.ACCOUNT_FROZEN);
        }
        if (account.getAccountType() != BankConstants.ACCOUNT_TYPE_CURRENT) {
            throw new BusinessException(ResultCode.ACCOUNT_TYPE_NOT_SUPPORT, "仅支持活期账户扣款");
        }

        // 4. 扣减账户余额(乐观锁)
        int rows = accountMapper.decreaseBalance(account.getId(), amount);
        if (rows == 0) {
            throw new BusinessException(ResultCode.BALANCE_NOT_ENOUGH);
        }

        // 5. 扣减产品额度(乐观锁)
        rows = productMapper.decreaseQuota(product.getId(), amount);
        if (rows == 0) {
            throw new BusinessException(ResultCode.PRODUCT_QUOTA_NOT_ENOUGH);
        }

        // 6. 生成持仓
        Account refreshed = accountMapper.selectById(account.getId());
        LocalDate today = LocalDate.now();
        LocalDate maturity = product.getTermDays() > 0
                ? today.plusDays(product.getTermDays())
                : null;

        BigDecimal expectedIncome = product.getTermDays() > 0
                ? amount.multiply(product.getAnnualRate())
                       .multiply(BigDecimal.valueOf(product.getTermDays()))
                       .divide(YEAR_DAYS, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        ProductHolding holding = new ProductHolding();
        holding.setHoldingNo(BankUtils.generateHoldingNo());
        holding.setUserId(userId);
        holding.setProductId(product.getId());
        holding.setAccountId(account.getId());
        holding.setAmount(amount);
        holding.setExpectedIncome(expectedIncome);
        holding.setActualIncome(BigDecimal.ZERO);
        holding.setPurchaseDate(today);
        holding.setMaturityDate(maturity);
        holding.setStatus(BankConstants.HOLDING_HOLDING);
        holdingMapper.insert(holding);

        // 7. 写入交易流水
        TransactionRecord record = new TransactionRecord();
        record.setTxnNo(BankUtils.generateTxnNo());
        record.setAccountId(refreshed.getId());
        record.setAccountNo(refreshed.getAccountNo());
        record.setTxnType(BankConstants.TXN_PRODUCT_PURCHASE);
        record.setAmount(amount);
        record.setBalanceBefore(refreshed.getBalance().add(amount));
        record.setBalanceAfter(refreshed.getBalance());
        record.setCounterpartyAccount(product.getProductCode());
        record.setCounterpartyName(product.getProductName());
        record.setRemark("申购理财产品 " + product.getProductCode());
        record.setTxnTime(today.atStartOfDay().plusSeconds(java.time.LocalTime.now().toSecondOfDay()));
        txnMapper.insert(record);

        // 8. 失效缓存
        evictAccountCache(account.getAccountNo());
        evictProductCache(product.getId());

        log.info("理财申购成功: user={}, product={}, amount={}, holdingNo={}",
                userId, product.getProductCode(), amount, holding.getHoldingNo());

        return detail(holding.getHoldingNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HoldingVO redeem(ProductRedeemDTO dto) {
        Long userId = UserContext.getUserId();

        ProductHolding holding = holdingMapper.selectOne(new LambdaQueryWrapper<ProductHolding>()
                .eq(ProductHolding::getHoldingNo, dto.getHoldingNo()));
        if (holding == null) {
            throw new BusinessException(ResultCode.HOLDING_NOT_EXISTS);
        }
        if (!holding.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作他人持仓");
        }
        if (holding.getStatus() != BankConstants.HOLDING_HOLDING) {
            throw new BusinessException(ResultCode.HOLDING_NOT_REDEEMABLE);
        }

        FinancialProduct product = productMapper.selectById(holding.getProductId());
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXISTS);
        }

        // 计算持有天数与实际收益
        LocalDate today = LocalDate.now();
        long holdDays = ChronoUnit.DAYS.between(holding.getPurchaseDate(), today);
        if (holdDays < 0) holdDays = 0;
        BigDecimal actualIncome = holding.getAmount()
                .multiply(product.getAnnualRate())
                .multiply(BigDecimal.valueOf(holdDays))
                .divide(YEAR_DAYS, 2, RoundingMode.HALF_UP);

        BigDecimal principal = holding.getAmount();
        BigDecimal totalBack = principal.add(actualIncome);

        // 1. 增加账户余额(本金 + 收益)
        int rows = accountMapper.increaseBalance(holding.getAccountId(), totalBack);
        if (rows == 0) {
            throw new BusinessException(ResultCode.DB_OPERATION_FAILED);
        }

        // 2. 恢复产品额度
        productMapper.increaseQuota(product.getId(), principal);

        // 3. 更新持仓状态
        holding.setStatus(BankConstants.HOLDING_REDEEMED);
        holding.setActualIncome(actualIncome);
        holdingMapper.updateById(holding);

        // 4. 写入交易流水(赎回)
        Account refreshed = accountMapper.selectById(holding.getAccountId());
        TransactionRecord record = new TransactionRecord();
        record.setTxnNo(BankUtils.generateTxnNo());
        record.setAccountId(refreshed.getId());
        record.setAccountNo(refreshed.getAccountNo());
        record.setTxnType(BankConstants.TXN_PRODUCT_REDEEM);
        record.setAmount(totalBack);
        record.setBalanceBefore(refreshed.getBalance().subtract(totalBack));
        record.setBalanceAfter(refreshed.getBalance());
        record.setCounterpartyAccount(product.getProductCode());
        record.setCounterpartyName(product.getProductName());
        record.setRemark("赎回理财产品 本金=" + principal + " 收益=" + actualIncome);
        record.setTxnTime(today.atStartOfDay().plusSeconds(java.time.LocalTime.now().toSecondOfDay()));
        txnMapper.insert(record);

        // 5. 失效缓存
        evictAccountCache(refreshed.getAccountNo());
        evictProductCache(product.getId());

        log.info("理财赎回成功: user={}, holdingNo={}, principal={}, income={}",
                userId, holding.getHoldingNo(), principal, actualIncome);

        return detail(holding.getHoldingNo());
    }

    @Override
    public HoldingVO detail(String holdingNo) {
        HoldingVO vo = holdingMapper.selectVOByHoldingNo(holdingNo);
        if (vo == null) {
            throw new BusinessException(ResultCode.HOLDING_NOT_EXISTS);
        }
        BankUtils.fillHoldingDesc(vo);
        return vo;
    }

    @Override
    public PageVO<HoldingVO> page(Long userId, Integer status, long current, long size) {
        Page<HoldingVO> p = new Page<>(current, size);
        IPage<HoldingVO> result = holdingMapper.selectVOPage(p, userId, status);
        List<HoldingVO> records = result.getRecords();
        records.forEach(BankUtils::fillHoldingDesc);
        return PageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    private void evictAccountCache(String accountNo) {
        try {
            redisTemplate.delete(BankConstants.CACHE_ACCOUNT_PREFIX + accountNo);
        } catch (Exception e) {
            log.warn("删除账户缓存失败: {}", e.getMessage());
        }
    }

    private void evictProductCache(Long productId) {
        try {
            redisTemplate.delete(BankConstants.CACHE_PRODUCT_PREFIX + productId);
        } catch (Exception e) {
            log.warn("删除产品缓存失败: {}", e.getMessage());
        }
    }
}
