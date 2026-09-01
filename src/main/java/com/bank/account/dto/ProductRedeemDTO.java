package com.bank.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 理财赎回请求
 */
@Data
@Schema(description = "理财赎回请求")
public class ProductRedeemDTO {

    @Schema(description = "持仓编号", example = "H20240601000001")
    @NotBlank(message = "持仓编号不能为空")
    private String holdingNo;
}
