package com.bank.account.controller;

import com.bank.account.common.BusinessException;
import com.bank.account.common.Result;
import com.bank.account.common.ResultCode;
import com.bank.account.service.LedgerService;
import com.bank.account.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * 账务对账接口(后台管理控制台用)
 *
 * 【权限】与其它 /api/admin/** 接口一致,由 AdminInterceptor 校验 role=ADMIN。
 */
@RestController
@RequestMapping("/api/admin/ledger")
@RequiredArgsConstructor
@Tag(name = "账务对账", description = "复式记账：试算平衡 / 余额对账 / 分录查询 / 冲正")
public class AdminLedgerController {

    private final LedgerService ledgerService;

    @GetMapping("/trial-balance")
    @Operation(summary = "试算平衡", description = "统计区间内所有分录的借贷合计，借贷必须相等（差额应为 0）")
    public Result<Map<String, Object>> trialBalance(
            @Parameter(description = "起始日期 yyyy-MM-dd") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 yyyy-MM-dd") @RequestParam(required = false) String endDate) {
        return Result.success(ledgerService.trialBalance(parse(startDate), parse(endDate)));
    }

    @GetMapping("/reconcile")
    @Operation(summary = "余额对账", description = "用分录反推余额，与账户余额字段对撞，列出不平的账户")
    public Result<Map<String, Object>> reconcile() {
        return Result.success(ledgerService.reconcile());
    }

    @GetMapping("/entries")
    @Operation(summary = "分录查询")
    public Result<PageVO<Map<String, Object>>> entries(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String accountNo,
            @RequestParam(required = false) String txnNo,
            @Parameter(description = "方向:1-借 2-贷") @RequestParam(required = false) Integer direction) {
        return Result.success(ledgerService.pageEntries(current, size, accountNo, txnNo, direction));
    }

    @PostMapping("/reverse/{txnNo}")
    @Operation(summary = "冲正(红冲)",
            description = "为指定流水生成方向相反的分录并回滚余额；原分录只标记“已冲正”，不改金额、不删除（审计要求）。重复调用幂等。")
    public Result<Boolean> reverse(@PathVariable String txnNo,
                                   @RequestParam(required = false) String reason) {
        boolean changed = ledgerService.reverse(txnNo, reason);
        return Result.success(changed ? "冲正成功，已生成反向分录" : "该流水此前已冲正，无需重复操作", changed);
    }

    private LocalDate parse(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date.trim());
        } catch (Exception e) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "日期格式应为 yyyy-MM-dd");
        }
    }
}