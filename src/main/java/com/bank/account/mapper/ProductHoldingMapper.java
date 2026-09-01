package com.bank.account.mapper;

import com.bank.account.entity.ProductHolding;
import com.bank.account.vo.HoldingVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 理财持仓 Mapper
 */
@Mapper
public interface ProductHoldingMapper extends BaseMapper<ProductHolding> {

    /**
     * 按持仓编号查询(带产品和用户信息)
     */
    HoldingVO selectVOByHoldingNo(@Param("holdingNo") String holdingNo);

    /**
     * 分页查询用户持仓
     */
    IPage<HoldingVO> selectVOPage(Page<HoldingVO> page,
                                  @Param("userId") Long userId,
                                  @Param("status") Integer status);
}
