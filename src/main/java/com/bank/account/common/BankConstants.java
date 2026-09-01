package com.bank.account.common;

/**
 * 业务常量
 */
public final class BankConstants {

    private BankConstants() {}

    /** 账户类型 */
    public static final int ACCOUNT_TYPE_CURRENT = 1;
    public static final int ACCOUNT_TYPE_FIXED    = 2;
    public static final int ACCOUNT_TYPE_WEALTH   = 3;

    /** 账户状态 */
    public static final int ACCOUNT_STATUS_FROZEN = 0;
    public static final int ACCOUNT_STATUS_NORMAL = 1;
    public static final int ACCOUNT_STATUS_CLOSED  = 2;

    /** 交易类型 */
    public static final int TXN_DEPOSIT          = 1;
    public static final int TXN_WITHDRAW         = 2;
    public static final int TXN_TRANSFER_IN      = 3;
    public static final int TXN_TRANSFER_OUT     = 4;
    public static final int TXN_INTEREST         = 5;
    public static final int TXN_PRODUCT_PURCHASE = 6;
    public static final int TXN_PRODUCT_REDEEM   = 7;

    /** 产品状态 */
    public static final int PRODUCT_STATUS_PENDING = 0;
    public static final int PRODUCT_STATUS_RAISING = 1;
    public static final int PRODUCT_STATUS_RUNNING  = 2;
    public static final int PRODUCT_STATUS_ENDED    = 3;

    /** 持仓状态 */
    public static final int HOLDING_HOLDING  = 1;
    public static final int HOLDING_REDEEMED = 2;
    public static final int HOLDING_MATURED  = 3;

    /** Redis 缓存键 */
    public static final String CACHE_ACCOUNT_PREFIX   = "bank:account:";
    public static final String CACHE_PRODUCT_PREFIX   = "bank:product:";
    public static final String CACHE_PRODUCT_LIST_KEY = "bank:product:list";

    /** 流水号前缀 */
    public static final String TXN_NO_PREFIX     = "T";
    public static final String HOLDING_NO_PREFIX = "H";

    /** 银行卡 BIN(发卡行标识) */
    public static final String CARD_BIN = "622200";
}
