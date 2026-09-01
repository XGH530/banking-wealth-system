package com.bank.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易流水视图对象
 */
@Data
@Schema(description = "交易流水")
public class TransactionVO {

    @Schema(description = "流水ID")
    private Long id;

    @Schema(description = "流水号")
    private String txnNo;

    @Schema(description = "账号")
    private String accountNo;

    @Schema(description = "交易类型:1-存入,2-支取,3-转入,4-转出,5-利息,6-理财申购,7-理财赎回")
    private Integer txnType;

    @Schema(description = "交易类型描述")
    private String txnTypeDesc;

    @Schema(description = "交易金额")
    private BigDecimal amount;

    @Schema(description = "交易前余额")
    private BigDecimal balanceBefore;

    @Schema(description = "交易后余额")
    private BigDecimal balanceAfter;

    @Schema(description = "对方账号")
    private String counterpartyAccount;

    @Schema(description = "对方户名")
    private String counterpartyName;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "交易时间")
    private LocalDateTime txnTime;
}
