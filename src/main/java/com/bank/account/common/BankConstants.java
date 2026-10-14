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

    /** 交易类型:冲正(红冲) */
    public static final int TXN_REVERSE = 8;

    /** 分录方向:1-借(资产增加/负债减少) 2-贷(负债增加/资产减少) */
    public static final int ENTRY_DEBIT  = 1;
    public static final int ENTRY_CREDIT = 2;

    /** 分录业务类型:0-期初余额,其余复用交易类型 */
    public static final int BIZ_OPENING = 0;

    /**
     * 内部账户(银行现金/清算/理财归集)
     *
     * 【设计说明】存取款只有一方是客户账户,另一方是银行内部账户。
     * 复式记账要求"有借必有贷",所以用 id=0、账号 SYSTEM 表示内部账户,
     * 它只参与平衡,不参与客户余额对账。
     */
    public static final long SYSTEM_ACCOUNT_ID = 0L;
    public static final String SYSTEM_ACCOUNT_NO = "SYSTEM";

    /** 分录号前缀 */
    public static final String ENTRY_NO_PREFIX = "E";

    /**
     * Token 黑名单前缀
     * 用途:登出时把 access token 加入黑名单(有效期 = token 剩余时间),
     *      拦截器每次校验都先查黑名单 —— 这样"登出立即失效"才成立,
     *      而不是等 token 自然过期。
     */
    public static final String CACHE_TOKEN_BLACKLIST_PREFIX = "bank:jwt:blacklist:";

    /** 流水号前缀 */
    public static final String TXN_NO_PREFIX     = "T";
    public static final String HOLDING_NO_PREFIX = "H";

    /** 银行卡 BIN(发卡行标识) */
    public static final String CARD_BIN = "622200";
}
