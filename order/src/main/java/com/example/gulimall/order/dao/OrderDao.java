package com.example.gulimall.order.dao;

import com.example.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author Li
 * @email Li@163.com
 * @date 2022-08-18 15:51:01
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
