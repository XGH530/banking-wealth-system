package com.bank.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 理财产品实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("financial_product")
public class FinancialProduct extends BaseEntity {

    /** 产品代码 */
    private String productCode;

    /** 产品名称 */
    private String productName;

    /** 产品类型:1-活期,2-定期,3-基金,4-债券 */
    private Integer productType;

    /** 年化收益率 */
    private BigDecimal annualRate;

    /** 最低申购金额 */
    private BigDecimal minAmount;

    /** 递增金额 */
    private BigDecimal incrementAmount;

    /** 产品期限(天) */
    private Integer termDays;

    /** 风险等级:1-低 ~ 5-高 */
    private Integer riskLevel;

    /** 产品总额度 */
    private BigDecimal totalQuota;

    /** 剩余额度 */
    private BigDecimal remainingQuota;

    /** 募集开始日 */
    private LocalDate raiseStartDate;

    /** 募集结束日 */
    private LocalDate raiseEndDate;

    /** 状态:0-待售,1-募集中,2-运作中,3-已结束 */
    private Integer status;
}
