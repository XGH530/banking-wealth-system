package com.bank.account.mapper;

import com.bank.account.entity.TransactionRecord;
import com.bank.account.vo.TransactionVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
 * 交易流水 Mapper
 */
@Mapper
public interface TransactionRecordMapper extends BaseMapper<TransactionRecord> {

    /**
     * 按账号分页查询流水
     */
    IPage<TransactionVO> selectVOPage(Page<TransactionVO> page,
                                     @Param("userId") Long userId,
                                     @Param("accountNo") String accountNo,
                                     @Param("txnType") Integer txnType,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);
}
