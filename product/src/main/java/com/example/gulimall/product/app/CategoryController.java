package com.example.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.gulimall.product.entity.CategoryEntity;
import com.example.gulimall.product.service.CategoryService;
import com.example.common.utils.PageUtils;
import com.example.common.utils.R;


/**
 * 商品三级分类
 *
 * @author Li
 * @email Li@163.com
 * @date 2022-08-18 13:57:04
 */
@RestController
@RequestMapping("product/category")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    /**
     * 查出所有的分类和子分类，以树形结构组装起来
     */
    @RequestMapping("/list/tree")
    public R listTree() {
        List<CategoryEntity> entities = categoryService.listWithTree();
        return R.ok().put("data", entities);
    }

    /**
     * 分页查询
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:category:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = categoryService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{catId}")
    //@RequiresPermissions("product:category:info")
    public R info(@PathVariable("catId") Long catId) {
        CategoryEntity category = categoryService.getById(catId);

        return R.ok().put("data", category);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:category:save")
    public R save(@RequestBody CategoryEntity category) {
        categoryService.save(category);

        return R.ok();
    }

    /**
     * 级联更新数据
     */
    @RequestMapping("/update")
    public R update(@RequestBody CategoryEntity category) {
        categoryService.updateCascade(category);

        return R.ok();
    }

    /**
     * 批量修改
     */
    @RequestMapping("/update/sort")
    public R updateSort(@RequestBody CategoryEntity[] category) {
        categoryService.updateBatchById(Arrays.asList(category));
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:category:delete")
    public R delete(@RequestBody Long[] catIds) {
        categoryService.removeByIds(Arrays.asList(catIds));

        return R.ok();
    }

    @RequestMapping("/deleteMenu")
    //@RequiresPermissions("product:category:delete")
    public R deleteMenu(@RequestBody Long[] catIds) {
        categoryService.removeMenuByIds(Arrays.asList(catIds));

        return R.ok();
    }

}
