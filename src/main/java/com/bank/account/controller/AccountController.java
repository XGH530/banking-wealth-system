package com.bank.account.controller;

import com.bank.account.common.Result;
import com.bank.account.common.UserContext;
import com.bank.account.dto.AccountOpenDTO;
import com.bank.account.service.AccountService;
import com.bank.account.vo.AccountVO;
import com.bank.account.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 账户接口
 */
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Tag(name = "账户接口")
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    @Operation(summary = "开户")
    public Result<AccountVO> open(@Valid @RequestBody AccountOpenDTO dto) {
        return Result.created(accountService.open(dto));
    }

    @GetMapping("/{accountNo}")
    @Operation(summary = "按账号查询账户详情")
    public Result<AccountVO> detail(@PathVariable String accountNo) {
        return Result.success(accountService.detailByAccountNo(accountNo));
    }

    @GetMapping
    @Operation(summary = "分页查询账户(默认查当前用户)")
    public Result<PageVO<AccountVO>> page(
            @Parameter(description = "用户ID(为空则查当前登录用户)") @RequestParam(required = false) Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") long size) {
        if (userId == null) {
            userId = UserContext.getUserId();
        }
        return Result.success(accountService.page(userId, current, size));
    }

    @PutMapping("/{accountId}/status")
    @Operation(summary = "冻结/解冻账户")
    public Result<Void> changeStatus(@PathVariable Long accountId,
                                     @Parameter(description = "状态:0-冻结,1-正常") @RequestParam Integer status) {
        accountService.changeStatus(accountId, status);
        return Result.success();
    }

    @DeleteMapping("/{accountId}")
    @Operation(summary = "销户")
    public Result<Void> close(@PathVariable Long accountId) {
        accountService.close(accountId);
        return Result.success();
    }
}
