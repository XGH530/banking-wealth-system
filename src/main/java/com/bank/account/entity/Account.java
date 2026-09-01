package com.bank.account.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 银行账户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("account")
public class Account extends BaseEntity {

    /** 银行账号 */
    private String accountNo;

    /** 用户ID */
    private Long userId;

    /** 账户类型:1-活期,2-定期,3-理财 */
    private Integer accountType;

    /** 账户余额 */
    private BigDecimal balance;

    /** 状态:0-冻结,1-正常,2-销户 */
    private Integer status;

    /** 币种 */
    private String currency;

    /** 开户日期 */
    private LocalDate openDate;

    /** 销户日期 */
    private LocalDate closeDate;

    /** 上次结息日期 */
    private LocalDate lastInterestDate;
}
