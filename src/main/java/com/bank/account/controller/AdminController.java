package com.bank.account.controller;

import com.bank.account.common.Result;
import com.bank.account.service.AdminService;
import com.bank.account.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 后台管理控制台接口
 *
 * 【权限】本组接口全部由 AdminInterceptor 校验 role=ADMIN,
 *        普通用户持自己的 Token 调用会直接 403。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "后台管理", description = "管理控制台:概览 / 用户 / 账户 / 流水 / 理财 / 运维健康检查")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/me")
    @Operation(summary = "当前管理员信息")
    public Result<Map<String, Object>> me() {
        return Result.success(adminService.currentAdmin());
    }

    @GetMapping("/overview")
    @Operation(summary = "控制台概览", description = "总量指标、账户状态分布、交易类型分布、近 7 日趋势、余额 Top5 账户")
    public Result<Map<String, Object>> overview() {
        return Result.success(adminService.overview());
    }

    @GetMapping("/users")
    @Operation(summary = "用户列表")
    public Result<PageVO<Map<String, Object>>> users(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "用户名/姓名/手机号 模糊匹配") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态:0-禁用,1-正常") @RequestParam(required = false) Integer status) {
        return Result.success(adminService.listUsers(current, size, keyword, status));
    }

    @PutMapping("/users/{id}/status")
    @Operation(summary = "启用/禁用用户")
    public Result<Void> updateUserStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateUserStatus(id, status);
        return Result.success();
    }

    @GetMapping("/accounts")
    @Operation(summary = "账户列表")
    public Result<PageVO<Map<String, Object>>> accounts(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @Parameter(description = "账号模糊匹配") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态:0-冻结,1-正常,2-销户") @RequestParam(required = false) Integer status) {
        return Result.success(adminService.listAccounts(current, size, keyword, status));
    }

    @PutMapping("/accounts/{id}/status")
    @Operation(summary = "冻结/解冻账户")
    public Result<Void> updateAccountStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateAccountStatus(id, status);
        return Result.success();
    }

    @GetMapping("/transactions")
    @Operation(summary = "全量流水查询")
    public Result<PageVO<Map<String, Object>>> transactions(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String accountNo,
            @Parameter(description = "1-存入 2-支取 3-转入 4-转出 5-利息 6-理财申购 7-理财赎回")
            @RequestParam(required = false) Integer txnType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return Result.success(adminService.listTransactions(current, size, accountNo, txnType, startDate, endDate));
    }

    @GetMapping("/products")
    @Operation(summary = "理财产品列表")
    public Result<PageVO<Map<String, Object>>> products(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(adminService.listProducts(current, size, keyword, status));
    }

    @PutMapping("/products/{id}/status")
    @Operation(summary = "修改理财产品状态", description = "0-待售,1-募集中,2-运作中,3-已结束")
    public Result<Void> updateProductStatus(@PathVariable Long id, @RequestParam Integer status) {
        adminService.updateProductStatus(id, status);
        return Result.success();
    }

    @GetMapping("/holdings")
    @Operation(summary = "理财持仓列表")
    public Result<PageVO<Map<String, Object>>> holdings(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) Integer status) {
        return Result.success(adminService.listHoldings(current, size, status));
    }

    @GetMapping("/health")
    @Operation(summary = "运行时健康检查", description = "数据库 / Redis 连通性与耗时、JVM 内存与运行时长")
    public Result<Map<String, Object>> health() {
        return Result.success(adminService.health());
    }
}