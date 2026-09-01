package com.bank.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易流水实体
 * 流水表不进行逻辑删除,因此不继承 BaseEntity
 */
@Data
@TableName("transaction_record")
public class TransactionRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 交易流水号 */
    private String txnNo;

    /** 账户ID */
    private Long accountId;

    /** 账号(冗余) */
    private String accountNo;

    /** 交易类型:1-存入,2-支取,3-转入,4-转出,5-利息,6-理财申购,7-理财赎回 */
    private Integer txnType;

    /** 交易金额 */
    private BigDecimal amount;

    /** 交易前余额 */
    private BigDecimal balanceBefore;

    /** 交易后余额 */
    private BigDecimal balanceAfter;

    /** 对方账号 */
    private String counterpartyAccount;

    /** 对方户名 */
    private String counterpartyName;

    /** 备注 */
    private String remark;

    /** 交易时间 */
    private LocalDateTime txnTime;

    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;
}
