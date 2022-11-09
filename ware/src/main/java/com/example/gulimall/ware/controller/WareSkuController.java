package com.example.gulimall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.authz.annotation.RequiresPermissions;
import com.example.common.exception.BizCodeEnum;
import com.example.common.to.SkuHasStockVo;
import com.example.gulimall.ware.exception.NoStockException;
import com.example.gulimall.ware.vo.OrderLockStockVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.gulimall.ware.entity.WareSkuEntity;
import com.example.gulimall.ware.service.WareSkuService;
import com.example.common.utils.PageUtils;
import com.example.common.utils.R;



/**
 * 商品库存
 *
 * @author Li
 * @email Li@163.com
 * @date 2022-08-18 15:53:01
 */
@RestController
@RequestMapping("ware/waresku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;

    @PostMapping("/lockStock")
    public R orderLockStock(@RequestBody OrderLockStockVo vo){
        try {
            Boolean b = wareSkuService.orderLockStock(vo);
            return R.ok();
        } catch (NoStockException e) {
            return R.error(BizCodeEnum.NO_STOCK_EXCEPTION.getCode(),e.getMessage());
        }
    }

    @PostMapping("/hasStock")
    public R getSkuHasStock(@RequestBody List<Long> skuIds){
        List<SkuHasStockVo> vos = wareSkuService.getSkuHasStock(skuIds);
        return R.ok().setData(vos);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id){
		WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku){
		wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku){
		wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids){
		wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
