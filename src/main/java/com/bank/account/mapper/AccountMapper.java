package com.bank.account.mapper;

import com.bank.account.entity.Account;
import com.bank.account.vo.AccountVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
/**
 * 账户表数据访问
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    /** 按账号查询账户（带所属用户信息） */
    AccountVO selectVOByAccountNo(@Param("accountNo") String accountNo);

    /** 分页查询账户（带所属用户信息） */
    IPage<AccountVO> selectVOPage(Page<AccountVO> page, @Param("userId") Long userId);

    /** 扣减余额；余额不足时返回 0（条件更新，靠数据库行锁保证并发安全） */
    int decreaseBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

    /** 增加余额 */
    int increaseBalance(@Param("id") Long id, @Param("amount") BigDecimal amount);

}
