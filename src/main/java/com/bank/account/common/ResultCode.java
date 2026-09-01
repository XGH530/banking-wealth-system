package com.bank.account.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

/**
 * 响应码
 */
@Getter
@Schema(description = "响应码枚举")
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    CREATED(201, "创建成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    CONFLICT(409, "资源冲突"),

    // 业务错误码(1xxx)
    USER_EXISTS(1001, "用户名已存在"),
    USER_NOT_EXISTS(1002, "用户不存在"),
    PASSWORD_ERROR(1003, "密码错误"),
    USER_DISABLED(1004, "账号已禁用"),

    ACCOUNT_NOT_EXISTS(2001, "账户不存在"),
    ACCOUNT_FROZEN(2002, "账户已冻结"),
    ACCOUNT_CLOSED(2003, "账户已销户"),
    BALANCE_NOT_ENOUGH(2004, "余额不足"),
    TRANSFER_SAME_ACCOUNT(2005, "转出转入账号不能相同"),
    TRANSFER_LIMIT_EXCEEDED(2006, "超出单笔转账限额"),
    ACCOUNT_TYPE_NOT_SUPPORT(2007, "账户类型不支持此操作"),

    PRODUCT_NOT_EXISTS(3001, "产品不存在"),
    PRODUCT_NOT_AVAILABLE(3002, "产品当前不可申购"),
    PRODUCT_QUOTA_NOT_ENOUGH(3003, "产品剩余额度不足"),
    PURCHASE_AMOUNT_INVALID(3004, "申购金额不符合要求"),
    HOLDING_NOT_EXISTS(3005, "持仓不存在"),
    HOLDING_NOT_REDEEMABLE(3006, "持仓不可赎回"),

    SERVER_ERROR(5000, "服务器内部错误"),
    DB_OPERATION_FAILED(5001, "数据库操作失败");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
