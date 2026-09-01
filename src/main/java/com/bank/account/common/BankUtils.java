package com.bank.account.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 业务工具方法
 */
public final class BankUtils {

    private BankUtils() {}

    private static final DateTimeFormatter TXN_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * 生成银行卡号:622200 + 14 位随机数
     */
    public static String generateAccountNo() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        StringBuilder sb = new StringBuilder(BankConstants.CARD_BIN);
        for (int i = 0; i < 14; i++) {
            sb.append(r.nextInt(10));
        }
        return sb.toString();
    }

    /**
     * 生成流水号:T + yyyyMMddHHmmss + 6 位随机数
     */
    public static String generateTxnNo() {
        return BankConstants.TXN_NO_PREFIX
                + LocalDateTime.now().format(TXN_FMT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    /**
     * 生成持仓编号:H + yyyyMMddHHmmss + 6 位随机数
     */
    public static String generateHoldingNo() {
        return BankConstants.HOLDING_NO_PREFIX
                + LocalDateTime.now().format(TXN_FMT)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    /**
     * 账户类型描述
     */
    public static String accountTypeDesc(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "活期";
            case 2 -> "定期";
            case 3 -> "理财";
            default -> "未知";
        };
    }

    /**
     * 账户状态描述
     */
    public static String accountStatusDesc(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "冻结";
            case 1 -> "正常";
            case 2 -> "销户";
            default -> "未知";
        };
    }

    /**
     * 交易类型描述
     */
    public static String txnTypeDesc(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "存入";
            case 2 -> "支取";
            case 3 -> "转入";
            case 4 -> "转出";
            case 5 -> "利息";
            case 6 -> "理财申购";
            case 7 -> "理财赎回";
            default -> "未知";
        };
    }

    /**
     * 产品类型描述
     */
    public static String productTypeDesc(Integer type) {
        if (type == null) return "";
        return switch (type) {
            case 1 -> "活期理财";
            case 2 -> "定期理财";
            case 3 -> "基金";
            case 4 -> "债券";
            default -> "未知";
        };
    }

    /**
     * 产品状态描述
     */
    public static String productStatusDesc(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 0 -> "待售";
            case 1 -> "募集中";
            case 2 -> "运作中";
            case 3 -> "已结束";
            default -> "未知";
        };
    }

    /**
     * 风险等级描述
     */
    public static String riskLevelDesc(Integer level) {
        if (level == null) return "";
        return switch (level) {
            case 1 -> "低风险";
            case 2 -> "中低风险";
            case 3 -> "中风险";
            case 4 -> "中高风险";
            case 5 -> "高风险";
            default -> "未知";
        };
    }

    /**
     * 持仓状态描述
     */
    public static String holdingStatusDesc(Integer status) {
        if (status == null) return "";
        return switch (status) {
            case 1 -> "持有中";
            case 2 -> "已赎回";
            case 3 -> "已到期";
            default -> "未知";
        };
    }

    /**
     * 填充账户 VO 描述字段
     */
    public static void fillAccountDesc(com.bank.account.vo.AccountVO vo) {
        vo.setAccountTypeDesc(accountTypeDesc(vo.getAccountType()));
        vo.setStatusDesc(accountStatusDesc(vo.getStatus()));
    }

    /**
     * 填充产品 VO 描述字段
     */
    public static void fillProductDesc(com.bank.account.vo.ProductVO vo) {
        vo.setProductTypeDesc(productTypeDesc(vo.getProductType()));
        vo.setStatusDesc(productStatusDesc(vo.getStatus()));
        vo.setRiskLevelDesc(riskLevelDesc(vo.getRiskLevel()));
    }

    /**
     * 填充交易流水 VO 描述字段
     */
    public static void fillTxnDesc(com.bank.account.vo.TransactionVO vo) {
        vo.setTxnTypeDesc(txnTypeDesc(vo.getTxnType()));
    }

    /**
     * 填充持仓 VO 描述字段
     */
    public static void fillHoldingDesc(com.bank.account.vo.HoldingVO vo) {
        vo.setProductTypeDesc(productTypeDesc(vo.getProductType()));
        vo.setStatusDesc(holdingStatusDesc(vo.getStatus()));
    }
}
