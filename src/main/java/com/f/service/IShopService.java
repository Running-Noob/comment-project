package com.f.service;

import com.f.dto.Result;
import com.f.pojo.Shop;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 */
public interface IShopService extends IService<Shop> {
    Result queryShopById(Long id);

    Result updateShopById(Shop shop);
}
