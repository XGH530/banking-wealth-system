package com.bank.account.service;

import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.dto.TransferDTO;
import com.bank.account.entity.Account;
import com.bank.account.mapper.AccountMapper;
import com.bank.account.mapper.TransactionRecordMapper;
import com.bank.account.service.impl.TransactionServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
 * 交易服务单元测试
 * 纯 Mockito, 不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private TransactionRecordMapper txnMapper;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private Account accountA;
    private Account accountB;

    @BeforeEach
    void setUp() throws Exception {
        // TransactionServiceImpl 里有 @Value 注入的 transferLimit, 需要手动反射设置
        java.lang.reflect.Field field = TransactionServiceImpl.class.getDeclaredField("transferLimit");
        field.setAccessible(true);
        field.set(transactionService, new BigDecimal("100000"));

        accountA = new Account();
        accountA.setId(1L);
        accountA.setAccountNo("6222000011110001");
        accountA.setBalance(new BigDecimal("50000.00"));
        accountA.setStatus(1);

        accountB = new Account();
        accountB.setId(2L);
        accountB.setAccountNo("6222000022220001");
        accountB.setBalance(new BigDecimal("35000.00"));
        accountB.setStatus(1);
    }

    @Test
    @DisplayName("转账失败 - 同账号转账")
    void transfer_sameAccount() {
        TransferDTO dto = new TransferDTO();
        dto.setFromAccountNo("6222000011110001");
        dto.setToAccountNo("6222000011110001");
        dto.setAmount(new BigDecimal("1000"));

        BusinessException ex = assertThrows(BusinessException.class, () -> transactionService.transfer(dto));
        assertEquals(ResultCode.TRANSFER_SAME_ACCOUNT.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("转账失败 - 单笔限额超限")
    void transfer_limitExceeded() {
        TransferDTO dto = new TransferDTO();
        dto.setFromAccountNo("6222000011110001");
        dto.setToAccountNo("6222000022220001");
        dto.setAmount(new BigDecimal("200000")); // 超过默认 10万限额

        BusinessException ex = assertThrows(BusinessException.class, () -> transactionService.transfer(dto));
        assertEquals(ResultCode.TRANSFER_LIMIT_EXCEEDED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("转账失败 - 转出账户不存在")
    void transfer_accountNotFound() {
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        TransferDTO dto = new TransferDTO();
        dto.setFromAccountNo("6222000099999999");
        dto.setToAccountNo("6222000022220001");
        dto.setAmount(new BigDecimal("1000"));

        BusinessException ex = assertThrows(BusinessException.class, () -> transactionService.transfer(dto));
        assertEquals(ResultCode.ACCOUNT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("转账失败 - 余额不足")
    void transfer_insufficientBalance() {
        // 第一次 selectOne 返回转出账户, 第二次返回转入账户
        when(accountMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(accountA)
                .thenReturn(accountB);
        // 余额扣减失败 (balance < amount), affect rows = 0
        when(accountMapper.decreaseBalance(anyLong(), any(BigDecimal.class))).thenReturn(0);

        TransferDTO dto = new TransferDTO();
        dto.setFromAccountNo("6222000011110001");
        dto.setToAccountNo("6222000022220001");
        dto.setAmount(new BigDecimal("60000")); // 超过 A 的 5万余额, 但低于 10万限额

        BusinessException ex = assertThrows(BusinessException.class, () -> transactionService.transfer(dto));
        assertEquals(ResultCode.BALANCE_NOT_ENOUGH.getCode(), ex.getCode());
    }
}
