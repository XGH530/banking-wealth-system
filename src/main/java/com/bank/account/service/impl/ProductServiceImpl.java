package com.bank.account.service.impl;

import com.bank.account.common.BankConstants;
import com.bank.account.common.BankUtils;
import com.bank.account.common.BusinessException;
import com.bank.account.common.ResultCode;
import com.bank.account.entity.FinancialProduct;
import com.bank.account.mapper.FinancialProductMapper;
import com.bank.account.service.ProductService;
import com.bank.account.vo.PageVO;
import com.bank.account.vo.ProductVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 理财产品服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final FinancialProductMapper productMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public ProductVO detail(Long id) {
        String key = BankConstants.CACHE_PRODUCT_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof ProductVO vo) {
            return vo;
        }

        FinancialProduct product = productMapper.selectById(id);
        if (product == null) {
            throw new BusinessException(ResultCode.PRODUCT_NOT_EXISTS);
        }
        ProductVO vo = toVO(product);
        try {
            redisTemplate.opsForValue().set(key, vo, Duration.ofMinutes(10));
        } catch (Exception e) {
            log.warn("缓存产品失败: {}", e.getMessage());
        }
        return vo;
    }

    @Override
    public PageVO<ProductVO> page(Integer productType, Integer status, long current, long size) {
        Page<ProductVO> p = new Page<>(current, size);
        IPage<ProductVO> result = productMapper.selectVOPage(p, productType, status);
        result.getRecords().forEach(BankUtils::fillProductDesc);
        return PageVO.of(result.getCurrent(), result.getSize(), result.getTotal(), result.getRecords());
    }

    private ProductVO toVO(FinancialProduct p) {
        ProductVO vo = new ProductVO();
        vo.setId(p.getId());
        vo.setProductCode(p.getProductCode());
        vo.setProductName(p.getProductName());
        vo.setProductType(p.getProductType());
        vo.setAnnualRate(p.getAnnualRate());
        vo.setMinAmount(p.getMinAmount());
        vo.setIncrementAmount(p.getIncrementAmount());
        vo.setTermDays(p.getTermDays());
        vo.setRiskLevel(p.getRiskLevel());
        vo.setTotalQuota(p.getTotalQuota());
        vo.setRemainingQuota(p.getRemainingQuota());
        vo.setRaiseStartDate(p.getRaiseStartDate());
        vo.setRaiseEndDate(p.getRaiseEndDate());
        vo.setStatus(p.getStatus());
        BankUtils.fillProductDesc(vo);
        return vo;
    }
}
