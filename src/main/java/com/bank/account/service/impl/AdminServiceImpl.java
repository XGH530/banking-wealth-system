package com.bank.account.service.impl;

import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.common.UserContext;
import com.bank.account.entity.Account;
import com.bank.account.entity.FinancialProduct;
import com.bank.account.entity.ProductHolding;
import com.bank.account.entity.SysUser;
import com.bank.account.entity.TransactionRecord;
import com.bank.account.mapper.AccountMapper;
import com.bank.account.mapper.FinancialProductMapper;
import com.bank.account.mapper.ProductHoldingMapper;
import com.bank.account.mapper.SysUserMapper;
import com.bank.account.mapper.TransactionRecordMapper;
import com.bank.account.service.AdminService;
import com.bank.account.vo.PageVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 后台管理控制台服务实现
 *
 * 说明:管理端列表用 MyBatis-Plus 的 Page + QueryWrapper 实现,
 *      统计类指标直接下推成 SQL 的 GROUP BY(而不是把明细全捞到内存里再循环),
 *      这样数据量涨上来也不会把 JVM 撑爆。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final SysUserMapper userMapper;
    private final AccountMapper accountMapper;
    private final TransactionRecordMapper transactionMapper;
    private final FinancialProductMapper productMapper;
    private final ProductHoldingMapper holdingMapper;
    private final StringRedisTemplate redisTemplate;
    private final DataSource dataSource;

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter MONTH_DAY = DateTimeFormatter.ofPattern("MM-dd");

    /** 账户类型文案 */
    private static String accountTypeText(Integer type) {
        if (type == null) return "-";
        return switch (type) {
            case 1 -> "活期";
            case 2 -> "定期";
            case 3 -> "理财";
            default -> "未知";
        };
    }

    /** 账户状态文案 */
    private static String accountStatusText(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 0 -> "冻结";
            case 1 -> "正常";
            case 2 -> "销户";
            default -> "未知";
        };
    }

    /** 交易类型文案 */
    private static String txnTypeText(Integer type) {
        if (type == null) return "-";
        return switch (type) {
            case 1 -> "存入";
            case 2 -> "支取";
            case 3 -> "转入";
            case 4 -> "转出";
            case 5 -> "利息";
            case 6 -> "理财申购";
            case 7 -> "理财赎回";
            default -> "其他";
        };
    }

    /** 产品状态文案 */
    private static String productStatusText(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 0 -> "待售";
            case 1 -> "募集中";
            case 2 -> "运作中";
            case 3 -> "已结束";
            default -> "未知";
        };
    }

    /** 持仓状态文案 */
    private static String holdingStatusText(Integer status) {
        if (status == null) return "-";
        return switch (status) {
            case 1 -> "持有中";
            case 2 -> "已赎回";
            case 3 -> "已到期";
            default -> "未知";
        };
    }

    private static Object nv(Object v) {
        return v == null ? "" : v;
    }

    @Override
    public Map<String, Object> currentAdmin() {
        Long userId = UserContext.getUserId();
        SysUser user = userId == null ? null : userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", user.getId());
        map.put("username", user.getUsername());
        map.put("realName", user.getRealName());
        map.put("role", user.getRole());
        return map;
    }

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();

        // ---------- 总量指标 ----------
        result.put("userCount", userMapper.selectCount(null));
        result.put("accountCount", accountMapper.selectCount(null));
        result.put("productCount", productMapper.selectCount(null));
        result.put("holdingCount", holdingMapper.selectCount(null));
        result.put("txnCount", transactionMapper.selectCount(null));

        Map<String, Object> balanceRow = one(accountMapper.selectMaps(new QueryWrapper<Account>()
                .select("IFNULL(SUM(balance),0) AS total")));
        result.put("totalBalance", balanceRow.getOrDefault("total", BigDecimal.ZERO));

        Map<String, Object> holdingRow = one(holdingMapper.selectMaps(new QueryWrapper<ProductHolding>()
                .select("IFNULL(SUM(amount),0) AS total")));
        result.put("holdingAmount", holdingRow.getOrDefault("total", BigDecimal.ZERO));

        // ---------- 今日交易 ----------
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        result.put("todayTxnCount", transactionMapper.selectCount(
                new LambdaQueryWrapper<TransactionRecord>().ge(TransactionRecord::getTxnTime, todayStart)));
        Map<String, Object> todayRow = one(transactionMapper.selectMaps(new QueryWrapper<TransactionRecord>()
                .select("IFNULL(SUM(amount),0) AS total")
                .ge("txn_time", todayStart)));
        result.put("todayTxnAmount", todayRow.getOrDefault("total", BigDecimal.ZERO));

        // ---------- 账户状态分布 ----------
        result.put("accountStatus", accountMapper.selectMaps(
                new QueryWrapper<Account>().select("status", "COUNT(*) AS cnt").groupBy("status"))
                .stream()
                .map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", accountStatusText(toInt(row.get("status"))));
                    m.put("value", row.get("cnt"));
                    return m;
                }).collect(Collectors.toList()));

        // ---------- 交易类型分布 ----------
        result.put("txnType", transactionMapper.selectMaps(new QueryWrapper<TransactionRecord>()
                        .select("txn_type", "COUNT(*) AS cnt", "IFNULL(SUM(amount),0) AS amt")
                        .groupBy("txn_type"))
                .stream().map(row -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("name", txnTypeText(toInt(row.get("txn_type"))));
                    m.put("count", row.get("cnt"));
                    m.put("amount", row.get("amt"));
                    return m;
                }).collect(Collectors.toList()));

        // ---------- 近 7 日交易趋势 ----------
        LocalDateTime from = today.minusDays(6).atStartOfDay();
        List<Map<String, Object>> rows = transactionMapper.selectMaps(new QueryWrapper<TransactionRecord>()
                .select("DATE(txn_time) AS d", "COUNT(*) AS c", "IFNULL(SUM(amount),0) AS amt")
                .ge("txn_time", from)
                .groupBy("DATE(txn_time)"));
        Map<String, Map<String, Object>> byDate = new HashMap<>();
        for (Map<String, Object> row : rows) {
            byDate.put(String.valueOf(row.get("d")), row);
        }
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Map<String, Object> row = byDate.get(day.format(DATE));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", day.format(MONTH_DAY));
            m.put("count", row == null ? 0 : row.get("c"));
            m.put("amount", row == null ? 0 : row.get("amt"));
            trend.add(m);
        }
        result.put("trend", trend);

        // ---------- 余额 Top 账户 ----------
        List<Account> topAccounts = accountMapper.selectList(new LambdaQueryWrapper<Account>()
                .orderByDesc(Account::getBalance).last("LIMIT 5"));
        result.put("topAccounts", enrichAccounts(topAccounts).stream()
                .map(vo -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("accountNo", vo.get("accountNo"));
                    m.put("userName", vo.get("userRealName"));
                    m.put("balance", vo.get("balance"));
                    m.put("accountTypeText", vo.get("accountTypeText"));
                    return m;
                }).collect(Collectors.toList()));

        result.put("serverTime", LocalDateTime.now().format(DATE_TIME));
        return result;
    }

    @Override
    public PageVO<Map<String, Object>> listUsers(long current, long size, String keyword, Integer status) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getRealName, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getId);

        Page<SysUser> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("realName", u.getRealName());
            m.put("idCard", u.getIdCard());
            m.put("phone", u.getPhone());
            m.put("email", nv(u.getEmail()));
            m.put("status", u.getStatus());
            m.put("role", nv(u.getRole()));
            m.put("createTime", u.getCreateTime() == null ? "" : u.getCreateTime().format(DATE_TIME));
            return m;
        }).collect(Collectors.toList());
        return PageVO.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0(禁用) 或 1(正常)");
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }
        if (Objects.equals(userId, UserContext.getUserId())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "不能停用自己的账号");
        }
        SysUser update = new SysUser();
        update.setId(userId);
        update.setStatus(status);
        userMapper.updateById(update);
        log.info("管理员 {} 将用户 {} 状态改为 {}", UserContext.getUsername(), user.getUsername(), status);
    }

    @Override
    public PageVO<Map<String, Object>> listAccounts(long current, long size, String keyword, Integer status) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.like(Account::getAccountNo, keyword);
        }
        if (status != null) {
            wrapper.eq(Account::getStatus, status);
        }
        wrapper.orderByDesc(Account::getBalance);

        Page<Account> page = accountMapper.selectPage(new Page<>(current, size), wrapper);
        return PageVO.of(page.getCurrent(), page.getSize(), page.getTotal(), enrichAccounts(page.getRecords()));
    }

    @Override
    public void updateAccountStatus(Long accountId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0(冻结) 或 1(正常)");
        }
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException(ResultCode.ACCOUNT_NOT_EXISTS);
        }
        if (account.getStatus() != null && account.getStatus() == 2) {
            throw new BusinessException(ResultCode.ACCOUNT_CLOSED, "已销户的账户不能改状态");
        }
        Account update = new Account();
        update.setId(accountId);
        update.setStatus(status);
        accountMapper.updateById(update);
        log.info("管理员 {} 将账户 {} 状态改为 {}", UserContext.getUsername(), account.getAccountNo(), status);
    }

    @Override
    public PageVO<Map<String, Object>> listTransactions(long current, long size, String accountNo,
                                                        Integer txnType, String startDate, String endDate) {
        LambdaQueryWrapper<TransactionRecord> wrapper = new LambdaQueryWrapper<>();
        if (accountNo != null && !accountNo.isBlank()) {
            wrapper.like(TransactionRecord::getAccountNo, accountNo);
        }
        if (txnType != null) {
            wrapper.eq(TransactionRecord::getTxnType, txnType);
        }
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(TransactionRecord::getTxnTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(TransactionRecord::getTxnTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        wrapper.orderByDesc(TransactionRecord::getTxnTime).orderByDesc(TransactionRecord::getId);

        Page<TransactionRecord> page = transactionMapper.selectPage(new Page<>(current, size), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream().map(t -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("txnNo", t.getTxnNo());
            m.put("accountNo", t.getAccountNo());
            m.put("txnType", t.getTxnType());
            m.put("txnTypeText", txnTypeText(t.getTxnType()));
            m.put("amount", t.getAmount());
            m.put("balanceBefore", t.getBalanceBefore());
            m.put("balanceAfter", t.getBalanceAfter());
            m.put("counterpartyAccount", nv(t.getCounterpartyAccount()));
            m.put("counterpartyName", nv(t.getCounterpartyName()));
            m.put("remark", nv(t.getRemark()));
            m.put("txnTime", t.getTxnTime() == null ? "" : t.getTxnTime().format(DATE_TIME));
            return m;
        }).collect(Collectors.toList());
        return PageVO.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public PageVO<Map<String, Object>> listProducts(long current, long size, String keyword, Integer status) {
        LambdaQueryWrapper<FinancialProduct> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(FinancialProduct::getProductName, keyword)
                    .or().like(FinancialProduct::getProductCode, keyword));
        }
        if (status != null) {
            wrapper.eq(FinancialProduct::getStatus, status);
        }
        wrapper.orderByAsc(FinancialProduct::getId);

        Page<FinancialProduct> page = productMapper.selectPage(new Page<>(current, size), wrapper);
        List<Map<String, Object>> records = page.getRecords().stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("productCode", p.getProductCode());
            m.put("productName", p.getProductName());
            m.put("productType", p.getProductType());
            m.put("annualRate", p.getAnnualRate());
            m.put("minAmount", p.getMinAmount());
            m.put("termDays", p.getTermDays());
            m.put("riskLevel", p.getRiskLevel());
            m.put("totalQuota", p.getTotalQuota());
            m.put("remainingQuota", p.getRemainingQuota());
            m.put("status", p.getStatus());
            m.put("statusText", productStatusText(p.getStatus()));
            m.put("raiseStartDate", p.getRaiseStartDate() == null ? "" : p.getRaiseStartDate().format(DATE));
            m.put("raiseEndDate", p.getRaiseEndDate() == null ? "" : p.getRaiseEndDate().format(DATE));
            return m;
        }).collect(Collectors.toList());
        return PageVO.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public void updateProductStatus(Long productId, Integer status) {
        if (status == null || status < 0 || status > 3) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "状态只能是 0~3");
        }
        FinancialProduct product = productMapper.selectById(productId);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXISTS);
        }
        FinancialProduct update = new FinancialProduct();
        update.setId(productId);
        update.setStatus(status);
        productMapper.updateById(update);
        log.info("管理员 {} 将产品 {} 状态改为 {}", UserContext.getUsername(), product.getProductCode(), status);
    }

    @Override
    public PageVO<Map<String, Object>> listHoldings(long current, long size, Integer status) {
        LambdaQueryWrapper<ProductHolding> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(ProductHolding::getStatus, status);
        }
        wrapper.orderByDesc(ProductHolding::getId);

        Page<ProductHolding> page = holdingMapper.selectPage(new Page<>(current, size), wrapper);
        List<ProductHolding> holdings = page.getRecords();

        Map<Long, String> userNameMap = loadUserNames(holdings.stream().map(ProductHolding::getUserId).collect(Collectors.toList()));
        Map<Long, FinancialProduct> productMap = loadProducts(holdings.stream().map(ProductHolding::getProductId).collect(Collectors.toList()));

        List<Map<String, Object>> records = holdings.stream().map(h -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("holdingNo", h.getHoldingNo());
            m.put("userId", h.getUserId());
            m.put("userName", userNameMap.getOrDefault(h.getUserId(), "-"));
            FinancialProduct product = productMap.get(h.getProductId());
            m.put("productName", product == null ? "-" : product.getProductName());
            m.put("productCode", product == null ? "-" : product.getProductCode());
            m.put("amount", h.getAmount());
            m.put("expectedIncome", h.getExpectedIncome());
            m.put("actualIncome", h.getActualIncome());
            m.put("purchaseDate", h.getPurchaseDate() == null ? "" : h.getPurchaseDate().format(DATE));
            m.put("maturityDate", h.getMaturityDate() == null ? "" : h.getMaturityDate().format(DATE));
            m.put("status", h.getStatus());
            m.put("statusText", holdingStatusText(h.getStatus()));
            return m;
        }).collect(Collectors.toList());
        return PageVO.of(page.getCurrent(), page.getSize(), page.getTotal(), records);
    }

    @Override
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();

        // ---------- 数据库 ----------
        Map<String, Object> db = new LinkedHashMap<>();
        long dbStart = System.nanoTime();
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            db.put("ok", true);
            db.put("product", meta.getDatabaseProductName());
            db.put("version", meta.getDatabaseProductVersion());
        } catch (Exception e) {
            db.put("ok", false);
            db.put("error", e.getMessage());
        }
        db.put("latencyMs", (System.nanoTime() - dbStart) / 1_000_000);
        result.put("db", db);

        // ---------- Redis ----------
        Map<String, Object> redis = new LinkedHashMap<>();
        long redisStart = System.nanoTime();
        try {
            Boolean exists = redisTemplate.hasKey("bank:health:probe");
            redis.put("ok", exists != null);
        } catch (Exception e) {
            redis.put("ok", false);
            redis.put("error", e.getMessage());
        }
        redis.put("latencyMs", (System.nanoTime() - redisStart) / 1_000_000);
        result.put("redis", redis);

        // ---------- JVM ----------
        Runtime runtime = Runtime.getRuntime();
        long mb = 1024 * 1024;
        Map<String, Object> jvm = new LinkedHashMap<>();
        jvm.put("usedMb", (runtime.totalMemory() - runtime.freeMemory()) / mb);
        jvm.put("committedMb", runtime.totalMemory() / mb);
        jvm.put("maxMb", runtime.maxMemory() / mb);
        jvm.put("processors", runtime.availableProcessors());
        jvm.put("javaVersion", System.getProperty("java.version"));
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        jvm.put("uptimeSeconds", uptimeMs / 1000);
        jvm.put("uptimeText", formatUptime(uptimeMs));
        result.put("jvm", jvm);

        result.put("os", System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        result.put("serverTime", LocalDateTime.now().format(DATE_TIME));
        return result;
    }

    // ==================== 私有工具 ====================

    private static String formatUptime(long uptimeMs) {
        long totalSeconds = uptimeMs / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        if (days > 0) return days + " 天 " + hours + " 小时";
        if (hours > 0) return hours + " 小时 " + minutes + " 分";
        return minutes + " 分";
    }

    private static Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        return Integer.valueOf(value.toString());
    }

    private static Map<String, Object> one(List<Map<String, Object>> rows) {
        return rows == null || rows.isEmpty() ? Collections.emptyMap() : rows.get(0);
    }

    /** 账户列表补上所属用户信息 */
    private List<Map<String, Object>> enrichAccounts(List<Account> accounts) {
        Map<Long, SysUser> userMap = accounts.isEmpty() ? Collections.emptyMap()
                : userMapper.selectBatchIds(accounts.stream().map(Account::getUserId).filter(Objects::nonNull)
                        .distinct().collect(Collectors.toList()))
                .stream().collect(Collectors.toMap(SysUser::getId, Function.identity()));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Account account : accounts) {
            SysUser user = userMap.get(account.getUserId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", account.getId());
            m.put("accountNo", account.getAccountNo());
            m.put("userId", account.getUserId());
            m.put("userName", user == null ? "-" : user.getUsername());
            m.put("userRealName", user == null ? "-" : user.getRealName());
            m.put("accountType", account.getAccountType());
            m.put("accountTypeText", accountTypeText(account.getAccountType()));
            m.put("balance", account.getBalance());
            m.put("currency", nv(account.getCurrency()));
            m.put("status", account.getStatus());
            m.put("statusText", accountStatusText(account.getStatus()));
            m.put("openDate", account.getOpenDate() == null ? "" : account.getOpenDate().format(DATE));
            list.add(m);
        }
        return list;
    }

    private Map<Long, String> loadUserNames(List<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId,
                        u -> u.getRealName() == null ? u.getUsername() : u.getRealName()));
    }

    private Map<Long, FinancialProduct> loadProducts(List<Long> productIds) {
        List<Long> ids = productIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return Collections.emptyMap();
        return productMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(FinancialProduct::getId, Function.identity()));
    }
}