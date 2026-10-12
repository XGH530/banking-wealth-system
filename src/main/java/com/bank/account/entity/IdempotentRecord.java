package com.bank.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 幂等记录
 *
 * 【设计说明:为什么幂等不能只靠前端】
 * 转账/存取款这类资金接口,网络超时后客户端会重试,
 * 但服务端可能已经处理成功了 —— 用户看到"失败"却扣了钱,或者重试导致重复扣款。
 * 解决办法是客户端每次请求带一个唯一 requestId(幂等键):
 *   - 表中以 idem_key 建唯一索引(数据库层兜底,并发下也只会成功一次)
 *   - 命中已有记录 → 直接把首次的执行结果返回(重放),不再执行业务
 *   - 业务失败时整个事务回滚,幂等记录也一起回滚,允许客户端用同一个 key 重试
 */
@Data
@TableName("idempotent_record")
public class IdempotentRecord implements Serializable {

    /** 幂等键(客户端生成的请求ID,主键) */
    @TableId(type = IdType.INPUT)
    private String idemKey;

    /** 业务类型:TRANSFER / DEPOSIT / WITHDRAW ... */
    private String bizType;

    /** 首次执行生成的流水号(事后重放时用它找回结果) */
    private String txnNo;

    private LocalDateTime createTime;
}