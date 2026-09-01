package com.bank.account.controller;

import com.bank.account.common.Result;
import com.bank.account.service.ProductService;
import com.bank.account.vo.PageVO;
import com.bank.account.vo.ProductVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 理财产品接口
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "理财产品接口")
public class ProductController {

    private final ProductService productService;

    @GetMapping("/{id}")
    @Operation(summary = "产品详情")
    public Result<ProductVO> detail(@PathVariable Long id) {
        return Result.success(productService.detail(id));
    }

    @GetMapping
    @Operation(summary = "分页查询产品")
    public Result<PageVO<ProductVO>> page(
            @Parameter(description = "产品类型:1-活期,2-定期,3-基金,4-债券") @RequestParam(required = false) Integer productType,
            @Parameter(description = "状态:0-待售,1-募集中,2-运作中,3-已结束") @RequestParam(required = false) Integer status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") long current,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") long size) {
        return Result.success(productService.page(productType, status, current, size));
    }
}
