package com.bank.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 存取款请求
 */
@Data
@Schema(description = "存取款请求")
public class DepositWithdrawDTO {

    @Schema(description = "账号", example = "6222000011110001")
    @NotBlank(message = "账号不能为空")
    private String accountNo;

    @Schema(description = "金额", example = "1000.00")
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于 0")
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;
}
