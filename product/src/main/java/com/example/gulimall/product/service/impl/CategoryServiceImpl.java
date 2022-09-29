package com.example.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;
import com.example.gulimall.product.dao.CategoryDao;
import com.example.gulimall.product.entity.CategoryEntity;
import com.example.gulimall.product.service.CategoryBrandRelationService;
import com.example.gulimall.product.service.CategoryService;
import com.example.gulimall.product.vo.Catelog2Vo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    private StringRedisTemplate redisTemplate;

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
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter(categoryEntity -> categoryEntity.getParentCid() == 0).map(menu -> {
            menu.setChildren(getChildrens(menu, categoryEntities));
            return menu;
        }).sorted((menu1, menu2) -> (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort())).collect(Collectors.toList());

        return level1Menus;
    }

    /**
     * baseMapper 容器中对应的的mapper对象
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
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    @Override
    public List<CategoryEntity> getLevel1Catagorys() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }

    /** 使用redis缓存查询所有分类
     * 有缓存穿透，缓存雪崩，缓存击穿的问题，需要加锁
     * 1.缓存穿透：空值
     * 2.缓存雪崩：过期时间（随机数，缓存数据不能大量的同一时间到期）
     * 3.缓存击穿：锁（synchronized本地锁 / 分布式锁）
     **/
    @Override
    public synchronized Map<String, List<Catelog2Vo>> getCatalogJson() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            //如果缓存中没有查到，需要到数据库查询
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromDB();
            //转换成json，保存到缓存
            String jsonString = JSON.toJSONString(catalogJsonFromDB);
            redisTemplate.opsForValue().set("catalogJson",jsonString,new Random().nextInt(5), TimeUnit.MINUTES);
            return catalogJsonFromDB;
        }else {
            //将JSON字符串转换成需要的类型
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }
    }

    //从数据库查询所有分类，效率太低（需要使用缓存技术）
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDB() {
        //一次性查询所有分类，包括子分类（比多次查询快）
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //过滤出所有一级分类
        List<CategoryEntity> level1Catagorys = getParentCid(selectList, 0L);

        //通过一级分类id，查出返回的结果集
        Map<String, List<Catelog2Vo>> parent_cid = level1Catagorys.stream()
                .collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    //过滤出所有二级分类，v是一级分类对象
                    List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        //查询二级分类
                        catelog2Vos = categoryEntities.stream()
                                .map(level2 -> {
                                    //过滤出所有三级分类，level2是二级分类对象
                                    List<CategoryEntity> level3Catelog = getParentCid(selectList, level2.getCatId());
                                    List<Catelog2Vo.Catelog3Vo> catelog3Vos = null;
                                    if (level3Catelog != null) {
                                        //遍历集合
                                        catelog3Vos = level3Catelog.stream().map(level3 -> {
                                            //封装三级分类对象
                                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(level2.getCatId().toString(), level3.getCatId().toString(), level3.getName());
                                            return catelog3Vo;
                                        }).collect(Collectors.toList());
                                    }
                                    //包装二级分类得对象
                                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), catelog3Vos, level2.getCatId().toString(), level2.getName());
                                    return catelog2Vo;
                                }).collect(Collectors.toList());
                    }
                    return catelog2Vos;
                }));
        return parent_cid;
    }

    private List<CategoryEntity> getParentCid(List<CategoryEntity> selectList, long l) {
        List<CategoryEntity> collect = selectList.stream()
                .filter(categoryEntity -> categoryEntity.getParentCid() == l)
                .collect(Collectors.toList());
        return collect;
    }

    private List<Long> findParentPath(Long catelogId, List<Long> path) {
        CategoryEntity category = this.getById(catelogId);
        if (category.getParentCid() != 0) {
            findParentPath(category.getParentCid(), path);
        }
        path.add(catelogId);
        return path;
    }

    private List<CategoryEntity> getChildrens(CategoryEntity menu, List<CategoryEntity> categoryEntities) {
        //Long类型数据的比较使用.equals()，否则前端页面可能有DOM不显示
        List<CategoryEntity> children = categoryEntities.stream().filter(categoryEntity -> categoryEntity.getParentCid().equals(menu.getCatId())).map(entity -> {
            entity.setChildren(getChildrens(entity, categoryEntities));
            return entity;
        }).sorted((menu1, menu2) -> (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort())).collect(Collectors.toList());
        return children;
    }

}