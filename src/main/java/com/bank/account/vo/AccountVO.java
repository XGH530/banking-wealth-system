package com.bank.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 账户视图对象(包含用户信息冗余)
 */
@Data
@Schema(description = "账户视图")
public class AccountVO {

    @Schema(description = "账户ID")
    private Long id;

    @Schema(description = "账号")
    private String accountNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "户名")
    private String realName;

    @Schema(description = "账户类型:1-活期,2-定期,3-理财")
    private Integer accountType;

    @Schema(description = "账户类型描述")
    private String accountTypeDesc;

    @Schema(description = "账户余额")
    private BigDecimal balance;

    @Schema(description = "状态:0-冻结,1-正常,2-销户")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "币种")
    private String currency;

    @Schema(description = "开户日期")
    private LocalDate openDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
