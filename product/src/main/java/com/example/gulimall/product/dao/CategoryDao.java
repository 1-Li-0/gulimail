package com.example.gulimall.product.dao;

import com.example.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author Li
 * @email Li@163.com
 * @date 2022-08-17 15:43:12
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
