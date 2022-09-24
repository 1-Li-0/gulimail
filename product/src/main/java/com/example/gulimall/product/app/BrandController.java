package com.example.gulimall.product.app;

import com.example.common.utils.PageUtils;
import com.example.common.utils.R;
import com.example.common.valid.AddGroup;
import com.example.common.valid.UpdateGroup;
import com.example.common.valid.UpdateStatusGroup;
import com.example.gulimall.product.entity.BrandEntity;
import com.example.gulimall.product.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;


/**
 * 品牌
 *
 * @author Li
 * @email Li@163.com
 * @date 2022-08-18 13:57:04
 */
@RestController
@RequestMapping("product/brand")
public class BrandController {
    @Autowired
    private BrandService brandService;

    @RequestMapping("/upload")
    public R upload(@RequestParam("picture") MultipartFile picture) throws IOException {
        byte[] data = picture.getBytes();
        String filename = picture.getOriginalFilename();
        long size = picture.getSize();
        String[] path = brandService.uploadLogo(data, filename, size);
        System.out.println(path[0] + "/" + path[1]);
        return R.ok().put("path", path[0] + "/" + path[1]);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:brand:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = brandService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{brandId}")
    //@RequiresPermissions("product:brand:info")
    public R info(@PathVariable("brandId") Long brandId) {
        BrandEntity brand = brandService.getById(brandId);

        return R.ok().put("brand", brand);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:brand:save")
    public R save(@Validated(value = {AddGroup.class}) @RequestBody BrandEntity brand) {
        brandService.save(brand);
        return R.ok();
    }

    /**
     * 同时更新品牌表和关联关系表
     */
    @RequestMapping("/update")
    public R update(@Validated(value = {UpdateGroup.class}) @RequestBody BrandEntity brand) {
        brandService.updateDetail(brand);
        return R.ok();
    }

    /**
     * 修改品牌的状态
     */
    @RequestMapping("/update/status")
    //@RequiresPermissions("product:brand:update")
    public R updateStatus(@Validated(value = {UpdateStatusGroup.class}) @RequestBody BrandEntity brand) {
        brandService.updateById(brand);
        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:brand:delete")
    public R delete(@RequestBody Long[] brandIds) {
        brandService.removeByIds(Arrays.asList(brandIds));

        return R.ok();
    }

}
