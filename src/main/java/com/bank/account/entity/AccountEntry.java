package com.bank.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 会计分录实体(复式记账的明细账)
 *
 * 【设计说明:为什么要有分录表】
 * 只有"流水表"时,一笔业务想还原成"钱从哪来、到哪去"要靠读代码猜;
 * 而分录表把每笔业务拆成"借/贷"两腿(有借必有贷、借贷必相等),
 * 于是三件事变成了可验证的:
 *   1. 试算平衡: 所有分录 Σ借 = Σ贷(对不上就说明记账逻辑有 bug)
 *   2. 余额对账: 账户余额 == Σ贷 - Σ借(对不上就说明有资金被漏记/重复记账)
 *   3. 冲正纠错: 原分录不可变,用反向分录把错账"红冲"回去,保留完整痕迹
 *
 * 流水表(transaction_record)面向客户与业务展示;
 * 分录表(account_entry)面向账务与审计,两者通过 txn_no 关联。
 */
@Data
@TableName("account_entry")
public class AccountEntry implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分录号(唯一) */
    private String entryNo;

    /** 关联业务流水号:同一笔业务的借贷两条分录共享同一个 txnNo */
    private String txnNo;

    /** 账户ID(0 表示银行内部账户) */
    private Long accountId;

    /** 账号(冗余,便于查询) */
    private String accountNo;

    /** 方向:1-借 2-贷 */
    private Integer direction;

    /** 金额(始终为正数,方向由 direction 表示) */
    private BigDecimal amount;

    /** 该腿记账后的账户余额 */
    private BigDecimal balanceAfter;

    /** 业务类型:0-期初,其余与交易类型一致(1-存入 2-支取 3-转入 4-转出 5-利息 6-申购 7-赎回 8-冲正) */
    private Integer bizType;

    /** 记账时间 */
    private LocalDateTime entryTime;

    /** 摘要 */
    private String remark;

    /** 冲正关系:本次分录是冲正哪一笔 txnNo 产生的 */
    private String reverseOf;

    /** 原分录是否已被冲正:0-否 1-是(原分录只标记,不删除、不修改金额) */
    private Integer isReversed;

    private LocalDateTime createTime;
}