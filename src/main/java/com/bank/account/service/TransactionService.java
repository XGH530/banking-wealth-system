package com.bank.account.service;

import com.bank.account.dto.DepositWithdrawDTO;
import com.bank.account.dto.TransactionQueryDTO;
import com.bank.account.dto.TransferDTO;
import com.bank.account.vo.PageVO;
import com.bank.account.vo.TransactionVO;

/**
 * 交易服务
 */
public interface TransactionService {

    /** 存入 */
    TransactionVO deposit(DepositWithdrawDTO dto);

    /** 支取 */
    TransactionVO withdraw(DepositWithdrawDTO dto);

    /** 转账 */
    TransactionVO transfer(TransferDTO dto);

    /** 分页查询流水 */
    PageVO<TransactionVO> page(TransactionQueryDTO query);
}
