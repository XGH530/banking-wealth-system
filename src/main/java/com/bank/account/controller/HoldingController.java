package com.bank.account.controller;

import com.bank.account.common.Result;
import com.bank.account.common.UserContext;
import com.bank.account.dto.ProductPurchaseDTO;
import com.bank.account.dto.ProductRedeemDTO;
import com.bank.account.service.HoldingService;
import com.bank.account.vo.HoldingVO;
import com.bank.account.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 理财持仓接口
 */
@RestController
@RequestMapping("/api/holdings")
@RequiredArgsConstructor
@Tag(name = "理财持仓接口")
public class HoldingController {

    private final HoldingService holdingService;

    @PostMapping("/purchase")
    @Operation(summary = "申购理财产品")
    public Result<HoldingVO> purchase(@Valid @RequestBody ProductPurchaseDTO dto) {
        return Result.created(holdingService.purchase(dto));
    }

    @PostMapping("/redeem")
    @Operation(summary = "赎回理财产品")
    public Result<HoldingVO> redeem(@Valid @RequestBody ProductRedeemDTO dto) {
        return Result.success(holdingService.redeem(dto));
    }

    @GetMapping("/{holdingNo}")
    @Operation(summary = "持仓详情")
    public Result<HoldingVO> detail(@PathVariable String holdingNo) {
        return Result.success(holdingService.detail(holdingNo));
    }

    @GetMapping
    @Operation(summary = "分页查询持仓")
    public Result<PageVO<HoldingVO>> page(
            @Parameter(description = "用户ID(为空则查当前登录用户)") @RequestParam(required = false) Long userId,
            @Parameter(description = "状态:1-持有中,2-已赎回,3-已到期") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") long size) {
        if (userId == null) {
            userId = UserContext.getUserId();
        }
        return Result.success(holdingService.page(userId, status, current, size));
    }
}
