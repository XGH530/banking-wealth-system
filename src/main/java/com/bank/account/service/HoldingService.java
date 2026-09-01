package com.bank.account.service;

import com.bank.account.dto.ProductPurchaseDTO;
import com.bank.account.dto.ProductRedeemDTO;
import com.bank.account.vo.HoldingVO;
import com.bank.account.vo.PageVO;

/**
 * 理财持仓服务
 */
public interface HoldingService {

    /** 申购 */
    HoldingVO purchase(ProductPurchaseDTO dto);

    /** 赎回 */
    HoldingVO redeem(ProductRedeemDTO dto);

    /** 持仓详情 */
    HoldingVO detail(String holdingNo);

    /** 分页查询 */
    PageVO<HoldingVO> page(Long userId, Integer status, long current, long size);
}
