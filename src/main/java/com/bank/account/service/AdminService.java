package com.bank.account.service;

import com.bank.account.vo.PageVO;

import java.util.Map;

/**
 * 后台管理控制台服务
 *
 * 【设计说明】
 * 管理端接口与客户端接口分开:客户端只能看"自己的"数据(靠 UserContext 里的 userId),
 * 管理端是跨用户的全局视角(用户/账户/流水/理财全量),因此单独一组 /api/admin/** 接口,
 * 并由 AdminInterceptor 统一做管理员鉴权。
 */
public interface AdminService {

    /** 当前登录的管理员信息 */
    Map<String, Object> currentAdmin();

    /** 控制台概览:总量指标 + 分布 + 近 7 日趋势 */
    Map<String, Object> overview();

    /** 用户列表(支持关键字与状态过滤) */
    PageVO<Map<String, Object>> listUsers(long current, long size, String keyword, Integer status);

    /** 启用/禁用用户 */
    void updateUserStatus(Long userId, Integer status);

    /** 账户列表(带所属用户名) */
    PageVO<Map<String, Object>> listAccounts(long current, long size, String keyword, Integer status);

    /** 冻结/解冻账户 */
    void updateAccountStatus(Long accountId, Integer status);

    /** 全量交易流水查询 */
    PageVO<Map<String, Object>> listTransactions(long current, long size, String accountNo,
                                                 Integer txnType, String startDate, String endDate);

    /** 理财产品列表 */
    PageVO<Map<String, Object>> listProducts(long current, long size, String keyword, Integer status);

    /** 修改理财产品状态 */
    void updateProductStatus(Long productId, Integer status);

    /** 理财持仓列表 */
    PageVO<Map<String, Object>> listHoldings(long current, long size, Integer status);

    /** 运行时健康检查:数据库 / Redis / JVM */
    Map<String, Object> health();
}