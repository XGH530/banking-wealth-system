package com.bank.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 理财持仓视图对象
 */
@Data
@Schema(description = "理财持仓")
public class HoldingVO {

    @Schema(description = "持仓ID")
    private Long id;

    @Schema(description = "持仓编号")
    private String holdingNo;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "产品ID")
    private Long productId;

    @Schema(description = "产品代码")
    private String productCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "产品类型:1-活期,2-定期,3-基金,4-债券")
    private Integer productType;

    @Schema(description = "产品类型描述")
    private String productTypeDesc;

    @Schema(description = "年化收益率")
    private BigDecimal annualRate;

    @Schema(description = "扣款账号")
    private String accountNo;

    @Schema(description = "持有金额")
    private BigDecimal amount;

    @Schema(description = "预期收益")
    private BigDecimal expectedIncome;

    @Schema(description = "实际收益")
    private BigDecimal actualIncome;

    @Schema(description = "申购日期")
    private LocalDate purchaseDate;

    @Schema(description = "到期日期")
    private LocalDate maturityDate;

    @Schema(description = "状态:1-持有中,2-已赎回,3-已到期")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;
}
