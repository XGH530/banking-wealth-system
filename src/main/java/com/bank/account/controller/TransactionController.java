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
import io.swagger.v3.oas.annotations.Parameter;
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
    @Operation(summary = "存入", description = "可带 X-Request-Id 请求头做幂等控制，重复提交只会入账一次")
    public Result<TransactionVO> deposit(
            @Valid @RequestBody DepositWithdrawDTO dto,
            @Parameter(description = "幂等键：客户端生成的唯一请求ID")
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return Result.success(transactionService.deposit(dto, requestId));
    }

    @PostMapping("/withdraw")
    @Operation(summary = "支取", description = "可带 X-Request-Id 请求头做幂等控制")
    public Result<TransactionVO> withdraw(
            @Valid @RequestBody DepositWithdrawDTO dto,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return Result.success(transactionService.withdraw(dto, requestId));
    }

    @PostMapping("/transfer")
    @Operation(summary = "转账", description = "可带 X-Request-Id 请求头做幂等控制；转账是最怕重复的资金接口")
    public Result<TransactionVO> transfer(
            @Valid @RequestBody TransferDTO dto,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        return Result.success(transactionService.transfer(dto, requestId));
    }

    @GetMapping
    @Operation(summary = "分页查询交易流水")
    public Result<PageVO<TransactionVO>> page(@Valid TransactionQueryDTO query) {
        return Result.success(transactionService.page(query));
    }
}
