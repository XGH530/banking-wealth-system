package com.bank.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 转账请求
 */
@Data
@Schema(description = "转账请求")
public class TransferDTO {

    @Schema(description = "转出账号", example = "6222000011110001")
    @NotBlank(message = "转出账号不能为空")
    private String fromAccountNo;

    @Schema(description = "转入账号", example = "6222000022220001")
    @NotBlank(message = "转入账号不能为空")
    private String toAccountNo;

    @Schema(description = "转账金额", example = "500.00")
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于 0")
    private BigDecimal amount;

    @Schema(description = "对方户名(用于校验)")
    private String counterpartyName;

    @Schema(description = "备注")
    private String remark;
}
