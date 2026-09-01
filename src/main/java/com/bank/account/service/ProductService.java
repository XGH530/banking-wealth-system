package com.bank.account.service;

import com.bank.account.vo.PageVO;
import com.bank.account.vo.ProductVO;

/**
 * 理财产品服务
 */
public interface ProductService {

    /** 产品详情 */
    ProductVO detail(Long id);

    /** 分页查询 */
    PageVO<ProductVO> page(Integer productType, Integer status, long current, long size);
}
