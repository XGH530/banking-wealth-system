package com.bank.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 理财持仓实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("product_holding")
public class ProductHolding extends BaseEntity {

    /** 持仓编号 */
    private String holdingNo;

    /** 用户ID */
    private Long userId;

    /** 产品ID */
    private Long productId;

    /** 扣款账户ID */
    private Long accountId;

    /** 持有金额 */
    private BigDecimal amount;

    /** 预期收益 */
    private BigDecimal expectedIncome;

    /** 实际收益 */
    private BigDecimal actualIncome;

    /** 申购日期 */
    private LocalDate purchaseDate;

    /** 到期日期 */
    private LocalDate maturityDate;

    /** 状态:1-持有中,2-已赎回,3-已到期 */
    private Integer status;
}
