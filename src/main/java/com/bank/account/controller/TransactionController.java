package com.bank.account.controller;

import com.bank.account.common.Result;
import com.bank.account.dto.DepositWithdrawDTO;
import com.bank.account.dto.TransactionQueryDTO;
import com.bank.account.dto.TransferDTO;
import com.bank.account.service.TransactionService;
import com.bank.account.vo.PageVO;
import com.bank.account.vo.TransactionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 交易接口
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Tag(name = "交易接口")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposit")
    @Operation(summary = "存入")
    public Result<TransactionVO> deposit(@Valid @RequestBody DepositWithdrawDTO dto) {
        return Result.success(transactionService.deposit(dto));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "支取")
    public Result<TransactionVO> withdraw(@Valid @RequestBody DepositWithdrawDTO dto) {
        return Result.success(transactionService.withdraw(dto));
    }

    @PostMapping("/transfer")
    @Operation(summary = "转账")
    public Result<TransactionVO> transfer(@Valid @RequestBody TransferDTO dto) {
        return Result.success(transactionService.transfer(dto));
    }

    @GetMapping
    @Operation(summary = "分页查询交易流水")
    public Result<PageVO<TransactionVO>> page(@Valid TransactionQueryDTO query) {
        return Result.success(transactionService.page(query));
    }
}
