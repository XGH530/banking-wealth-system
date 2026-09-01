package com.bank.account.service;

import com.bank.account.dto.AccountOpenDTO;
import com.bank.account.vo.AccountVO;
import com.bank.account.vo.PageVO;

/**
 * 账户服务
 */
public interface AccountService {

    /** 开户 */
    AccountVO open(AccountOpenDTO dto);

    /** 销户 */
    void close(Long accountId);

    /** 冻结/解冻 */
    void changeStatus(Long accountId, Integer status);

    /** 查询账户详情(带用户信息) */
    AccountVO detailByAccountNo(String accountNo);

    /** 查询账户详情 */
    AccountVO detailById(Long accountId);

    /** 分页查询 */
    PageVO<AccountVO> page(Long userId, long current, long size);
}
