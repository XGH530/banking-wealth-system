package com.bank.account.service.impl;

import com.bank.account.common.BankConstants;
import com.bank.account.common.BankUtils;
import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.common.UserContext;
import com.bank.account.dto.AccountOpenDTO;
import com.bank.account.entity.Account;
import com.bank.account.entity.SysUser;
import com.bank.account.mapper.AccountMapper;
import com.bank.account.mapper.SysUserMapper;
import com.bank.account.service.AccountService;
import com.bank.account.vo.AccountVO;
import com.bank.account.vo.PageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

/**
 * 账户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountMapper accountMapper;
    private final SysUserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountVO open(AccountOpenDTO dto) {
        // 决定用户ID:若传了 userId 则以传入为准(管理员场景),否则用当前登录用户
        Long userId = dto.getUserId() != null ? dto.getUserId() : UserContext.getUserId();

        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 校验账户类型
        Integer type = dto.getAccountType();
        if (type != BankConstants.ACCOUNT_TYPE_CURRENT
                && type != BankConstants.ACCOUNT_TYPE_FIXED
                && type != BankConstants.ACCOUNT_TYPE_WEALTH) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账户类型无效");
        }

        // 生成不重复的账号
        String accountNo;
        for (int i = 0; i < 5; i++) {
            accountNo = BankUtils.generateAccountNo();
            Long exists = accountMapper.selectCount(new LambdaQueryWrapper<Account>()
                    .eq(Account::getAccountNo, accountNo));
            if (exists == null || exists == 0) {
                Account account = new Account();
                account.setAccountNo(accountNo);
                account.setUserId(userId);
                account.setAccountType(type);
                account.setBalance(BigDecimal.ZERO);
                account.setStatus(BankConstants.ACCOUNT_STATUS_NORMAL);
                account.setCurrency("CNY");
                account.setOpenDate(LocalDate.now());
                accountMapper.insert(account);

                // 如果有初始存款,直接做一笔存入流水(此处简化为直接加余额)
                BigDecimal initAmount = dto.getInitialAmount();
                if (initAmount != null && initAmount.compareTo(BigDecimal.ZERO) > 0) {
                    accountMapper.increaseBalance(account.getId(), initAmount);
                }

                AccountVO vo = accountMapper.selectVOByAccountNo(accountNo);
                BankUtils.fillAccountDesc(vo);
                // 缓存
                cacheAccount(vo);
                return vo;
            }
        }
        throw new BusinessException(ResultCode.SERVER_ERROR, "账号生成失败,请重试");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        }
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账户尚有余额,无法销户");
        }
        if (account.getStatus() == BankConstants.ACCOUNT_STATUS_CLOSED) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "账户已销户");
        }

        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .eq(Account::getId, accountId)
                .set(Account::getStatus, BankConstants.ACCOUNT_STATUS_CLOSED)
                .set(Account::getCloseDate, LocalDate.now()));
        // 失效缓存
        evictAccount(account.getAccountNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatus(Long accountId, Integer status) {
        if (status != BankConstants.ACCOUNT_STATUS_FROZEN
                && status != BankConstants.ACCOUNT_STATUS_NORMAL) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态值无效");
        }
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        }
        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .eq(Account::getId, accountId)
                .set(Account::getStatus, status));
        evictAccount(account.getAccountNo());
    }

    @Override
    public AccountVO detailByAccountNo(String accountNo) {
        if (!StringUtils.hasText(accountNo)) {
            throw new BusinessException(ResultCode.BAD_REQUEST);
        }
        // 先查缓存
        String key = BankConstants.CACHE_ACCOUNT_PREFIX + accountNo;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof AccountVO vo) {
            return vo;
        }

        AccountVO vo = accountMapper.selectVOByAccountNo(accountNo);
        if (vo == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        }
        BankUtils.fillAccountDesc(vo);
        cacheAccount(vo);
        return vo;
    }

    @Override
    public AccountVO detailById(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        }
        return detailByAccountNo(account.getAccountNo());
    }

    @Override
    public PageVO<AccountVO> page(Long userId, long current, long size) {
        Page<AccountVO> p = new Page<>(current, size);
        IPage<AccountVO> result = accountMapper.selectVOPage(p, userId);
        result.getRecords().forEach(BankUtils::fillAccountDesc);
        return PageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    // ------------------------------------------------------------
    // 内部缓存工具
    // ------------------------------------------------------------
    private void cacheAccount(AccountVO vo) {
        try {
            redisTemplate.opsForValue().set(
                    BankConstants.CACHE_ACCOUNT_PREFIX + vo.getAccountNo(),
                    vo, Duration.ofMinutes(30));
        } catch (Exception e) {
            log.warn("缓存账户失败: {}", e.getMessage());
        }
    }

    private void evictAccount(String accountNo) {
        try {
            redisTemplate.delete(BankConstants.CACHE_ACCOUNT_PREFIX + accountNo);
        } catch (Exception e) {
            log.warn("删除账户缓存失败: {}", e.getMessage());
        }
    }
}
