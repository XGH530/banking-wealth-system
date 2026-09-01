package com.bank.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;

/**
 * 交易流水查询请求
 */
@Data
@Schema(description = "交易流水查询")
public class TransactionQueryDTO {

    @Schema(description = "账号(可选)")
    private String accountNo;

    @Schema(description = "交易类型:1-存入,2-支取,3-转入,4-转出,5-利息,6-理财申购,7-理财赎回")
    private Integer txnType;

    @Schema(description = "起始日期(yyyy-MM-dd)")
    private LocalDate startDate;

    @Schema(description = "结束日期(yyyy-MM-dd)")
    private LocalDate endDate;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;
}
