package com.bank.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 理财申购请求
 */
@Data
@Schema(description = "理财申购请求")
public class ProductPurchaseDTO {

    @Schema(description = "产品代码", example = "P2024001")
    @NotBlank(message = "产品代码不能为空")
    private String productCode;

    @Schema(description = "扣款账号", example = "6222000011110001")
    @NotBlank(message = "扣款账号不能为空")
    private String accountNo;

    @Schema(description = "申购金额", example = "10000.00")
    @NotNull(message = "金额不能为空")
    @Positive(message = "金额必须大于 0")
    private BigDecimal amount;
}
