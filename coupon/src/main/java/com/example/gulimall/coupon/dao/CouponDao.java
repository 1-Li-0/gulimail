package com.example.gulimall.coupon.dao;

import com.example.gulimall.coupon.entity.CouponEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 优惠券信息
 * 
 * @author Li
 * @email Li@163.com
 * @date 2022-08-18 15:26:31
 */
@Mapper
public interface CouponDao extends BaseMapper<CouponEntity> {
	
}
