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
 * 账户 Mapper
 */
@Mapper
public interface AccountMapper extends BaseMapper<Account> {

    /**
     * 根据账号查询(带用户信息)
     */
    AccountVO selectVOByAccountNo(@Param("accountNo") String accountNo);

    /**
     * 分页查询账户(带用户信息)
     */
    IPage<AccountVO> selectVOPage(Page<AccountVO> page, @Param("userId") Long userId);

    /**
     * 扣减余额(乐观锁)
     *
     * @return 影响行数,0 表示余额不足或失败
     */
    int decreaseBalance(@Param("id") Long id,
                        @Param("amount") BigDecimal amount);

    /**
     * 增加余额
     */
    int increaseBalance(@Param("id") Long id,
                        @Param("amount") BigDecimal amount);
}
