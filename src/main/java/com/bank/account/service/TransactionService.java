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

    /**
     * 存入(带幂等键)
     *
     * @param requestId 客户端生成的唯一请求ID;同一个 requestId 重复提交只会入账一次,
     *                  重复请求直接返回首次的流水结果。传 null 表示不做幂等控制。
     */
    TransactionVO deposit(DepositWithdrawDTO dto, String requestId);

    /** 支取 */
    TransactionVO withdraw(DepositWithdrawDTO dto);

    /** 支取(带幂等键,见 {@link #deposit(DepositWithdrawDTO, String)}) */
    TransactionVO withdraw(DepositWithdrawDTO dto, String requestId);

    /** 转账 */
    TransactionVO transfer(TransferDTO dto);

    /** 转账(带幂等键,见 {@link #deposit(DepositWithdrawDTO, String)}) */
    TransactionVO transfer(TransferDTO dto, String requestId);

    /** 分页查询流水 */
    PageVO<TransactionVO> page(TransactionQueryDTO query);
}
