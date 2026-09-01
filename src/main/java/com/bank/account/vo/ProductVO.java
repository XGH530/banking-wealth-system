package com.bank.account.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 理财产品视图对象
 */
@Data
@Schema(description = "理财产品")
public class ProductVO {

    @Schema(description = "产品ID")
    private Long id;

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

    @Schema(description = "最低申购金额")
    private BigDecimal minAmount;

    @Schema(description = "递增金额")
    private BigDecimal incrementAmount;

    @Schema(description = "产品期限(天)")
    private Integer termDays;

    @Schema(description = "风险等级:1-低 ~ 5-高")
    private Integer riskLevel;

    @Schema(description = "风险等级描述")
    private String riskLevelDesc;

    @Schema(description = "产品总额度")
    private BigDecimal totalQuota;

    @Schema(description = "剩余额度")
    private BigDecimal remainingQuota;

    @Schema(description = "募集开始日")
    private LocalDate raiseStartDate;

    @Schema(description = "募集结束日")
    private LocalDate raiseEndDate;

    @Schema(description = "状态:0-待售,1-募集中,2-运作中,3-已结束")
    private Integer status;

    @Schema(description = "状态描述")
    private String statusDesc;
}
