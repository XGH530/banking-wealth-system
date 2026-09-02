package com.bank.account.service;

import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.common.UserContext;
import com.bank.account.dto.ProductPurchaseDTO;
import com.bank.account.entity.Account;
import com.bank.account.entity.FinancialProduct;
import com.bank.account.mapper.AccountMapper;
import com.bank.account.mapper.FinancialProductMapper;
import com.bank.account.mapper.ProductHoldingMapper;
import com.bank.account.mapper.TransactionRecordMapper;
import com.bank.account.service.impl.HoldingServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 理财持仓服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class HoldingServiceTest {

    @Mock
    private ProductHoldingMapper holdingMapper;

    @Mock
    private FinancialProductMapper productMapper;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private TransactionRecordMapper txnMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private HoldingServiceImpl holdingService;

    private FinancialProduct product;
    private Account currentAccount;

    @BeforeEach
    void setUp() {
        // 设置当前登录用户
        UserContext.set(new UserContext.LoginUser(1L, "zhangsan", "张三"));

        product = new FinancialProduct();
        product.setId(1L);
        product.setProductCode("P2024001");
        product.setProductName("稳健天天赢");
        product.setAnnualRate(new BigDecimal("0.0350"));
        product.setMinAmount(new BigDecimal("1000"));
        product.setIncrementAmount(new BigDecimal("1000"));
        product.setRemainingQuota(new BigDecimal("8500000"));
        product.setStatus(2); // 运作中

        currentAccount = new Account();
        currentAccount.setId(1L);
        currentAccount.setAccountNo("6222000011110001");
        currentAccount.setUserId(1L);
        currentAccount.setAccountType(1); // 活期
        currentAccount.setBalance(new BigDecimal("50000"));
        currentAccount.setStatus(1);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    @Test
    @DisplayName("申购失败 - 产品不存在")
    void purchase_productNotFound() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ProductPurchaseDTO dto = new ProductPurchaseDTO();
        dto.setProductCode("P999999");
        dto.setAccountNo("6222000011110001");
        dto.setAmount(new BigDecimal("5000"));

        BusinessException ex = assertThrows(BusinessException.class, () -> holdingService.purchase(dto));
        assertEquals(ResultCode.PRODUCT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("申购失败 - 申购金额低于最低限额")
    void purchase_amountBelowMin() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(product);

        ProductPurchaseDTO dto = new ProductPurchaseDTO();
        dto.setProductCode("P2024001");
        dto.setAccountNo("6222000011110001");
        dto.setAmount(new BigDecimal("500")); // 低于 1000 最低限额

        BusinessException ex = assertThrows(BusinessException.class, () -> holdingService.purchase(dto));
        assertEquals(ResultCode.PURCHASE_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("申购失败 - 扣款账户不属于当前用户")
    void purchase_accountNotOwned() {
        when(productMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(product);
        // selectOne 条件带了 userId, 非本人账户返回 null
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        ProductPurchaseDTO dto = new ProductPurchaseDTO();
        dto.setProductCode("P2024001");
        dto.setAccountNo("6222000022220001");
        dto.setAmount(new BigDecimal("5000"));

        BusinessException ex = assertThrows(BusinessException.class, () -> holdingService.purchase(dto));
        assertEquals(ResultCode.ACCOUNT_NOT_EXISTS.getCode(), ex.getCode());
    }
}
