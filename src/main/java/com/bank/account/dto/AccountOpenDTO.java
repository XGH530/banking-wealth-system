package com.bank.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 开户请求
 */
@Data
@Schema(description = "开户请求")
public class AccountOpenDTO {

    @Schema(description = "用户ID(管理员为他人开户时传)", example = "1")
    private Long userId;

    @Schema(description = "账户类型:1-活期,2-定期,3-理财", example = "1")
    @NotNull(message = "账户类型不能为空")
    private Integer accountType;

    @Schema(description = "初始存入金额", example = "1000.00")
    private java.math.BigDecimal initialAmount;
}
