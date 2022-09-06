package com.example.gulimall.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;
import com.example.gulimall.product.dao.CategoryDao;
import com.example.gulimall.product.entity.CategoryEntity;
import com.example.gulimall.product.service.CategoryBrandRelationService;
import com.example.gulimall.product.service.CategoryService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //1.查出所有分类
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        //2.组装成父子树形结构
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter(categoryEntity -> categoryEntity.getParentCid() ==0 ).map(menu -> {
            menu.setChildren(getChildrens(menu, categoryEntities));
            return menu;
        }).sorted((menu1,menu2) -> (menu1.getSort()==null?0:menu1.getSort())-(menu2.getSort()==null?0:menu2.getSort())).collect(Collectors.toList());

        return level1Menus;
    }

    /**
     *  baseMapper 容器中对应的的mapper对象
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 检查当前需要删除的菜单是否被其他地方引用
        /**
         *  此处是物理删除，不推荐
         *  使用标识: 0表示已删除/不显示，1表示显示
         */
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] getCatelogPath(Long catelogId) {
        List<Long> path = new ArrayList<>();
        List<Long> parentPath = this.findParentPath(catelogId, path);
        //toArray()返回的是一个Object对象，需要换成Long[]
        return parentPath.toArray(new Long[0]);
    }

    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());
    }

    private List<Long> findParentPath(Long catelogId,List<Long> path){
        CategoryEntity category = this.getById(catelogId);
        if (category.getParentCid() != 0){
            findParentPath(category.getParentCid(),path);
        }
        path.add(catelogId);
        return path;
    }

    private List<CategoryEntity> getChildrens(CategoryEntity menu, List<CategoryEntity> categoryEntities) {
        //Long类型数据的比较使用.equals()，否则前端页面可能有DOM不显示
        List<CategoryEntity> children = categoryEntities.stream().filter(categoryEntity -> categoryEntity.getParentCid() .equals(menu.getCatId()) ).map( entity -> {
            entity.setChildren(getChildrens(entity, categoryEntities));
            return entity;
        }).sorted((menu1,menu2) -> (menu1.getSort()==null?0:menu1.getSort())-(menu2.getSort()==null?0:menu2.getSort())).collect(Collectors.toList());
        return children;
    }

}