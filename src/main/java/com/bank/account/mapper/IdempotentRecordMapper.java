package com.bank.account.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bank.account.entity.IdempotentRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * 幂等记录 Mapper
 */
@Mapper
public interface IdempotentRecordMapper extends BaseMapper<IdempotentRecord> {
}