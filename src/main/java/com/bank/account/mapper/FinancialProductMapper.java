package com.bank.account.mapper;

import com.bank.account.entity.FinancialProduct;
import com.bank.account.vo.ProductVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 理财产品 Mapper
 */
@Mapper
public interface FinancialProductMapper extends BaseMapper<FinancialProduct> {

    /**
     * 分页查询产品(视图对象)
     */
    IPage<ProductVO> selectVOPage(Page<ProductVO> page,
                                 @Param("productType") Integer productType,
                                 @Param("status") Integer status);

    /**
     * 扣减产品剩余额度(乐观锁)
     */
    int decreaseQuota(@Param("id") Long id,
                      @Param("amount") BigDecimal amount);

    /**
     * 恢复产品额度(赎回)
     */
    int increaseQuota(@Param("id") Long id,
                      @Param("amount") BigDecimal amount);
}
