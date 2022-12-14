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
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    @Autowired
    private RedissonClient redisson;

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
        //1.??????????????????
        List<CategoryEntity> categoryEntities = baseMapper.selectList(null);

        //2.???????????????????????????
        List<CategoryEntity> level1Menus = categoryEntities.stream().filter(categoryEntity -> categoryEntity.getParentCid() == 0).map(menu -> {
            menu.setChildren(getChildrens(menu, categoryEntities));
            return menu;
        }).sorted((menu1, menu2) -> (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort())).collect(Collectors.toList());

        return level1Menus;
    }

    /**
     * baseMapper ?????????????????????mapper??????
     */
    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO ????????????????????????????????????????????????????????????
        /**
         *  ?????????????????????????????????
         *  ????????????: 0???????????????/????????????1????????????
         */
        baseMapper.deleteBatchIds(asList);
    }

    @Override
    public Long[] getCatelogPath(Long catelogId) {
        List<Long> path = new ArrayList<>();
        List<Long> parentPath = this.findParentPath(catelogId, path);
        //toArray()??????????????????Object?????????????????????Long[]
        return parentPath.toArray(new Long[0]);
    }

    /** ??????????????????
     * ?????????????????????????????????????????????: @CacheEvict(value = "category",allEntries = true)
     * @param category ???????????????????????????
     * @Caching ??????????????????????????????
     * @CacheEvict ????????????????????????????????????????????????key???
     */
    @Caching(evict = {
            @CacheEvict(cacheNames = "category", key = "'getLevel1Categorys'"),
            @CacheEvict(cacheNames = "category", key = "'getCatalogJson'")
    })
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????? (?????????????????????????????????????????????cacheNames)
     * ??????????????????????????????????????????-1???
     * redis?????????key???????????????::???????????????????????????????????????????????????SpEL????????? (????????????????????????????????????'')
     * ??????????????????jdk???????????????????????????????????????????????????
     */
    @Cacheable(cacheNames = {"category"}, key = "#root.methodName")
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }

    /**
     * ????????????????????????????????????????????????
     */
    @Override
    @Cacheable(value = {"category"}, key = "#root.methodName")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        //?????????????????????????????????????????????????????????????????????
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        //??????????????????????????????id???????????????????????????
        return this.getParentCid(selectList, 0L).stream()
                .collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    //??????????????????????????????v?????????????????????
                    List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        //??????????????????
                        catelog2Vos = categoryEntities.stream()
                                .map(level2 -> {
                                    //??????????????????????????????level2?????????????????????
                                    List<CategoryEntity> level3Catelog = getParentCid(selectList, level2.getCatId());
                                    List<Catelog2Vo.Catelog3Vo> catelog3Vos = null;
                                    if (level3Catelog != null) {
                                        //????????????
                                        catelog3Vos = level3Catelog.stream().map(level3 -> {
                                            //????????????????????????
                                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(level2.getCatId().toString(), level3.getCatId().toString(), level3.getName());
                                            return catelog3Vo;
                                        }).collect(Collectors.toList());
                                    }
                                    //???????????????????????????
                                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), catelog3Vos, level2.getCatId().toString(), level2.getName());
                                    return catelog2Vo;
                                }).collect(Collectors.toList());
                    }
                    return catelog2Vos;
                }));
    }

    /**
     * ??????redis????????????????????????
     * ?????????????????????????????????????????????????????????????????????
     * 1.?????????????????????
     * 2.??????????????????????????????????????????????????????????????????????????????????????????
     * 3.?????????????????????synchronized????????? / ???????????????
     **/
    public Map<String, List<Catelog2Vo>> getCatalogJson2() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            //??????????????????????????????????????????????????????
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromDBWithRedissonLock();
            return catalogJsonFromDB;
        } else {
            //???JSON?????????????????????????????????
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }
    }

    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedissonLock() {
        RLock catalogJsonLock = redisson.getLock("catalogJson-lock");
        catalogJsonLock.lock();
        Map<String, List<Catelog2Vo>> catalogJsonMap;
        try {
            catalogJsonMap = this.getCatalogJsonFromDB();
        } finally {
            catalogJsonLock.unlock();
        }
        return catalogJsonMap;
    }

    //??????redis????????????setnx(key,value)???????????????---????????????????????????Redis???java???????????????Redisson
    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedisLock() {
        String uuid = UUID.randomUUID().toString();
        //??????????????????????????????????????????????????????
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);
        if (lock) {
            Map<String, List<Catelog2Vo>> map;
            try {
                map = this.getCatalogJsonFromDB();
            } finally {
                //????????????????????????????????????????????????del?????????????????????????????????lua??????????????????
//            redisTemplate.delete("lock");
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                Long i = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            return map;
        } else {
            //??????????????????????????????????????????????????????????????????????????????
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //??????????????????????????????????????????????????????????????????
            return getCatalogJson();
        }
    }

    //????????????????????????????????????????????????????????????synchronized??????JUC(lock)?????????????????????????????????????????????????????????????????????
    public synchronized Map<String, List<Catelog2Vo>> getCatalogJsonFromDB() {
        //????????????????????????????????????????????????????????????
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }

        //?????????????????????????????????????????????????????????????????????
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //???????????????????????????
        List<CategoryEntity> level1Catagorys = getParentCid(selectList, 0L);

        //??????????????????id???????????????????????????
        Map<String, List<Catelog2Vo>> parent_cid = level1Catagorys.stream()
                .collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    //??????????????????????????????v?????????????????????
                    List<CategoryEntity> categoryEntities = getParentCid(selectList, v.getCatId());
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        //??????????????????
                        catelog2Vos = categoryEntities.stream()
                                .map(level2 -> {
                                    //??????????????????????????????level2?????????????????????
                                    List<CategoryEntity> level3Catelog = getParentCid(selectList, level2.getCatId());
                                    List<Catelog2Vo.Catelog3Vo> catelog3Vos = null;
                                    if (level3Catelog != null) {
                                        //????????????
                                        catelog3Vos = level3Catelog.stream().map(level3 -> {
                                            //????????????????????????
                                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(level2.getCatId().toString(), level3.getCatId().toString(), level3.getName());
                                            return catelog3Vo;
                                        }).collect(Collectors.toList());
                                    }
                                    //???????????????????????????
                                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), catelog3Vos, level2.getCatId().toString(), level2.getName());
                                    return catelog2Vo;
                                }).collect(Collectors.toList());
                    }
                    return catelog2Vos;
                }));
        //?????????json??????????????????
        String jsonString = JSON.toJSONString(parent_cid);
        redisTemplate.opsForValue().set("catalogJson", jsonString, new Random().nextInt(5), TimeUnit.MINUTES);
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
        //Long???????????????????????????.equals()??????????????????????????????DOM?????????
        List<CategoryEntity> children = categoryEntities.stream().filter(categoryEntity -> categoryEntity.getParentCid().equals(menu.getCatId())).map(entity -> {
            entity.setChildren(getChildrens(entity, categoryEntities));
            return entity;
        }).sorted((menu1, menu2) -> (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort())).collect(Collectors.toList());
        return children;
    }

}