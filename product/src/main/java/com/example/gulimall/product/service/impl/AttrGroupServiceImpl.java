package com.example.gulimall.product.service.impl;

import com.example.gulimall.product.entity.AttrEntity;
import com.example.gulimall.product.service.AttrService;
import com.example.gulimall.product.vo.AttrGroupWithAttrsVo;
import com.example.gulimall.product.vo.SkuItemVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.product.dao.AttrGroupDao;
import com.example.gulimall.product.entity.AttrGroupEntity;
import com.example.gulimall.product.service.AttrGroupService;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {
    @Autowired
    private AttrService attrService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Long catelogId) {
        //获取查询的条件
        String key = (String) params.get("key");
        //设置查询条件
        QueryWrapper<AttrGroupEntity> wrapper = new QueryWrapper<AttrGroupEntity>();
        if (!StringUtils.isEmpty(key)) {
            wrapper.and((obj) -> {
                obj.eq("attr_group_id", key).or().like("attr_group_name", key);
            });
        }
        if (catelogId == 0) {
            IPage<AttrGroupEntity> page = this.page(new Query<AttrGroupEntity>().getPage(params), wrapper);
            return new PageUtils(page);
        } else {
            wrapper.eq("catelog_id", catelogId);
            IPage<AttrGroupEntity> page = this.page(
                    new Query<AttrGroupEntity>().getPage(params),
                    wrapper
            );
            return new PageUtils(page);
        }
    }

    @Override
    public List<AttrGroupWithAttrsVo> getAttrGroupWithAttrsByCatelogId(Long catelogId) {
        //查询分组
        List<AttrGroupEntity> attrGroupEntities = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));

        //查询分组下的所有属性
        if (attrGroupEntities != null) {
            List<AttrGroupWithAttrsVo> collect = attrGroupEntities.stream().map((attrGroupEntity) -> {
                AttrGroupWithAttrsVo vo = new AttrGroupWithAttrsVo();
                BeanUtils.copyProperties(attrGroupEntity, vo);
                List<AttrEntity> attrEntities = attrService.getRelationAttr(attrGroupEntity.getAttrGroupId());
                vo.setAttrs(attrEntities);
                return vo;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    @Override
    public List<SkuItemVo.SpuItemAttrGroupVo> getAttrGroupWithAttrsBySpuId(Long spuId, Long catalogId) {
        List<SkuItemVo.SpuItemAttrGroupVo> vos = this.baseMapper.getAttrGroupWithAttrsBySpuId(spuId,catalogId);
        return vos;
    }

}