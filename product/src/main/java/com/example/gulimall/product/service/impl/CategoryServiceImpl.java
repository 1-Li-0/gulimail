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

    /** 级联更新菜单
     * 缓存失效，此处可以直接删除分区: @CacheEvict(value = "category",allEntries = true)
     * @param category 需要更新的分类对象
     * @Caching 同时进行多种缓存操作
     * @CacheEvict 可以指明需要删除的缓存的分区名和key值
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
     * 该方法的返回值放入缓存，如果缓存中有则不需要调用此方法 (可以按业务类型指定缓存的分区名cacheNames)
     * 默认设置的缓存没有过期时间（-1）
     * redis的默认key是【分区名::随机生成】，随机生成的值默认是一个SpEL表达式 (如果是自定义字符串需要加'')
     * 默认使用的是jdk序列化格式（其它编程语言难以使用）
     */
    @Cacheable(cacheNames = {"category"}, key = "#root.methodName")
    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> categoryEntities = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return categoryEntities;
    }

    /**
     * 改造菜单查询的方法，使用缓存注解
     */
    @Override
    @Cacheable(value = {"category"}, key = "#root.methodName")
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        //一次性查询所有分类，包括子分类（比多次查询快）
        List<CategoryEntity> selectList = this.baseMapper.selectList(null);

        //通过查出所有一级分类id，查出返回的结果集
        return this.getParentCid(selectList, 0L).stream()
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
    }

    /**
     * 使用redis缓存查询所有分类
     * 有缓存穿透，缓存雪崩，缓存击穿的问题，需要加锁
     * 1.缓存穿透：空值
     * 2.缓存雪崩：过期时间（随机数，缓存数据不能大量的同一时间到期）
     * 3.缓存击穿：锁（synchronized本地锁 / 分布式锁）
     **/
    public Map<String, List<Catelog2Vo>> getCatalogJson2() {
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            //如果缓存中没有查到，需要到数据库查询
            Map<String, List<Catelog2Vo>> catalogJsonFromDB = getCatalogJsonFromDBWithRedissonLock();
            return catalogJsonFromDB;
        } else {
            //将JSON字符串转换成需要的类型
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

    //使用redis分布式锁setnx(key,value)原子性指令---不推荐；推荐使用Redis对java语言的支持Redisson
    private Map<String, List<Catelog2Vo>> getCatalogJsonFromDBWithRedisLock() {
        String uuid = UUID.randomUUID().toString();
        //创建锁的同时设置过期时间，保证原子性
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid, 30, TimeUnit.SECONDS);
        if (lock) {
            Map<String, List<Catelog2Vo>> map;
            try {
                map = this.getCatalogJsonFromDB();
            } finally {
                //查询数据并缓存后，删除锁不能使用del命令（保证原子性，使用lua脚本命令删）
//            redisTemplate.delete("lock");
                String script = "if redis.call('get',KEYS[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                Long i = redisTemplate.execute(new DefaultRedisScript<>(script, Long.class), Arrays.asList("lock"), uuid);
            }
            return map;
        } else {
            //重试，直到获取被释放的锁（设置睡眠时间，否则栈溢出）
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //调用上一级的方法，重新确认缓存中是否存在数据
            return getCatalogJson();
        }
    }

    //从数据库查询所有分类，效率太低（需要使用synchronized或者JUC(lock)本地锁；若无缓存，只查一次数据库并且存到缓存）
    public synchronized Map<String, List<Catelog2Vo>> getCatalogJsonFromDB() {
        //再次查询验证缓存，如果有缓存，不查数据库
        String catalogJson = redisTemplate.opsForValue().get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            Map<String, List<Catelog2Vo>> result = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {
            });
            return result;
        }

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
        //转换成json，保存到缓存
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
        //Long类型数据的比较使用.equals()，否则前端页面可能有DOM不显示
        List<CategoryEntity> children = categoryEntities.stream().filter(categoryEntity -> categoryEntity.getParentCid().equals(menu.getCatId())).map(entity -> {
            entity.setChildren(getChildrens(entity, categoryEntities));
            return entity;
        }).sorted((menu1, menu2) -> (menu1.getSort() == null ? 0 : menu1.getSort()) - (menu2.getSort() == null ? 0 : menu2.getSort())).collect(Collectors.toList());
        return children;
    }

}