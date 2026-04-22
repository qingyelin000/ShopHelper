package com.shophelper.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.shophelper.order.entity.ProductSkuEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * SKU Mapper
 */
@Mapper
public interface ProductSkuMapper extends BaseMapper<ProductSkuEntity> {

    @Update("""
            UPDATE product_sku
            SET stock = stock - #{quantity},
                version = version + 1
            WHERE id = #{skuId}
              AND is_deleted = 0
              AND status = 1
              AND version = #{version}
              AND stock >= #{quantity}
            """)
    int deductStock(@Param("skuId") Long skuId,
                    @Param("quantity") Integer quantity,
                    @Param("version") Integer version);

    @Update("""
            UPDATE product_sku
            SET stock = stock + #{quantity},
                version = version + 1
            WHERE id = #{skuId}
              AND is_deleted = 0
            """)
    int restoreStock(@Param("skuId") Long skuId,
                     @Param("quantity") Integer quantity);
}
