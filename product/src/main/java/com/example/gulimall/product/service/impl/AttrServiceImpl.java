package com.example.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.constant.ProductConstant;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;
import com.example.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.example.gulimall.product.dao.AttrDao;
import com.example.gulimall.product.dao.AttrGroupDao;
import com.example.gulimall.product.dao.CategoryDao;
import com.example.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.example.gulimall.product.entity.AttrEntity;
import com.example.gulimall.product.entity.AttrGroupEntity;
import com.example.gulimall.product.entity.CategoryEntity;
import com.example.gulimall.product.service.AttrService;
import com.example.gulimall.product.service.CategoryService;
import com.example.gulimall.product.vo.AttrGroupRelationVO;
import com.example.gulimall.product.vo.AttrRespVO;
import com.example.gulimall.product.vo.AttrVO;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {
    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;
    @Autowired
    private AttrDao attrDao;
    @Autowired
    private AttrGroupDao attrGroupDao;
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private CategoryService categoryService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveAttr(AttrVO attr) {
        AttrEntity attrEntity = new AttrEntity();
        //将AttrVO的属性封装到AttrEntity对象
        BeanUtils.copyProperties(attr, attrEntity);
        //保存属性
        this.save(attrEntity);
        //基本属性需要保存关联关系
        if (attr.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            AttrEntity attrInsert = attrDao.selectOne(new QueryWrapper<AttrEntity>().eq("attr_name", attr.getAttrName()).eq("catelog_id", attr.getCatelogId()));
            relationEntity.setAttrId(attrInsert.getAttrId());
            attrAttrgroupRelationDao.insert(relationEntity);
        }

    }

    @Override
    public PageUtils queryBaseAttrPage(Map<String, Object> params, Long catelogId, String type) {
        //必须匹配属性类型，eq条件在new对象时添加
        QueryWrapper<AttrEntity> queryWrapper = new QueryWrapper<AttrEntity>().eq("attr_type", "base".equalsIgnoreCase(type) ? ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode() : ProductConstant.AttrEnum.ATTR_TYPE_SALE.getCode());
        if (catelogId != 0) {
            queryWrapper.eq("catelog_id", catelogId);
        }
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            queryWrapper.and((wrapper) -> {
                wrapper.eq("attr_id", key).or().like("attr_name", key);
            });
        }
        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                queryWrapper
        );
        PageUtils pageUtils = new PageUtils(page);
        //取出包装在结果中的AttrEntity集合
        List<AttrEntity> records = page.getRecords();
        //查询分类和分组信息，用AttrRespVO对象收集数据
        List<AttrRespVO> respVOs = records.stream().map((attrEntity) -> {
            AttrRespVO attrRespVO = new AttrRespVO();
            //复制属性
            BeanUtils.copyProperties(attrEntity, attrRespVO);
            //查询关联信息
            AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrEntity.getAttrId()));
            if (relationEntity != null) {
                AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                attrRespVO.setGroupName(attrGroupEntity.getAttrGroupName());
            }
            CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
            if (categoryEntity != null) {
                attrRespVO.setCatelogName(categoryEntity.getName());
            }
            return attrRespVO;
        }).collect(Collectors.toList());
        //将集合重新设置到结果中
        pageUtils.setList(respVOs);
        return pageUtils;
    }

    @Override
    public AttrRespVO getAttrInfo(Long attrId) {
        AttrRespVO respVO = new AttrRespVO();
        AttrEntity attrEntity = attrDao.selectById(attrId);
        BeanUtils.copyProperties(attrEntity, respVO);
        //设置分类信息
        CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
        if (categoryEntity != null) {
            Long[] catelogPath = categoryService.getCatelogPath(attrEntity.getCatelogId());
            respVO.setCatelogPath(catelogPath);
            respVO.setCatelogName(categoryEntity.getName());
        }
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            //基本属性需要回显分组信息
            AttrAttrgroupRelationEntity relationEntity = attrAttrgroupRelationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attrId));
            if (relationEntity != null) {
                respVO.setAttrGroupId(relationEntity.getAttrGroupId());
                AttrGroupEntity groupEntity = attrGroupDao.selectById(relationEntity.getAttrGroupId());
                if (groupEntity != null) {
                    respVO.setGroupName(groupEntity.getAttrGroupName());
                }
            }
        }

        return respVO;
    }

    @Override
    @Transactional
    public void updateAttr(AttrVO attr) {
        //修改attr对象
        AttrEntity attrEntity = new AttrEntity();
        BeanUtils.copyProperties(attr, attrEntity);
        this.updateById(attrEntity);
        //基本属性还需要修改关联关系的分组信息
        if (attrEntity.getAttrType() == ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode()) {
            //封装一个AttrAttrgroupRelationEntity对象，设置属性
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            relationEntity.setAttrId(attr.getAttrId());
            relationEntity.setAttrGroupId(attr.getAttrGroupId());
            //计算符合条件的记录条目
            int count = attrAttrgroupRelationDao.selectCount(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            if (count > 0) {
                //count>0，表中有数据，需要修改，使用对象修改关联数据UpdateWrapper
                attrAttrgroupRelationDao.update(relationEntity, new UpdateWrapper<AttrAttrgroupRelationEntity>().eq("attr_id", attr.getAttrId()));
            } else {
                //count==0时没有记录，新增即可
                attrAttrgroupRelationDao.insert(relationEntity);
            }
        }

    }

    @Override
    public List<AttrEntity> getRelationAttr(Long attrGroupId) {
        List<AttrAttrgroupRelationEntity> relationEntities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrGroupId));
        List<AttrEntity> attrEntities = relationEntities.stream().map((relationEntity) -> {
            if (relationEntity.getAttrId() != null) {
                return attrDao.selectById(relationEntity.getAttrId());
            } else return null;
        }).collect(Collectors.toList());
        return attrEntities;
    }

    @Override
    public void deleteRelation(AttrGroupRelationVO[] vos) {
        List<AttrAttrgroupRelationEntity> entities = Arrays.asList(vos).stream().map((vo) -> {
            AttrAttrgroupRelationEntity relationEntity = new AttrAttrgroupRelationEntity();
            BeanUtils.copyProperties(vo, relationEntity);
            return relationEntity;
        }).collect(Collectors.toList());
        attrAttrgroupRelationDao.deleteBatchRelation(entities);
    }

    @Override
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrGroupId) {
        //新增关联的属性只能是自己所在的分类属性(查出分类Id)
        Long catelogId = attrGroupDao.selectById(attrGroupId).getCatelogId();
        //过滤其他分组已经关联过的属性
        return null;
    }
}