package com.f.mapper;

import com.f.pojo.SeckillVoucher;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.springframework.stereotype.Repository;

/**
 * <p>
 * 秒杀优惠券表，与优惠券是一对一关系 Mapper 接口
 * </p>
 *
 */
@Repository
public interface SeckillVoucherMapper extends BaseMapper<SeckillVoucher> {

}
