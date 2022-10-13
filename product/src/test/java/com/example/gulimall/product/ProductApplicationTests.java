package com.example.gulimall.product;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.gulimall.product.dao.AttrGroupDao;
import com.example.gulimall.product.dao.SkuSaleAttrValueDao;
import com.example.gulimall.product.entity.BrandEntity;
import com.example.gulimall.product.service.BrandService;
import com.example.gulimall.product.vo.SkuItemSaleAttrVo;
import com.example.gulimall.product.vo.SkuItemVo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class ProductApplicationTests {
    @Resource
    BrandService brandService;
    @Resource
    AttrGroupDao attrGroupDao;
    @Resource
    SkuSaleAttrValueDao saleAttrValueDao;

    @Test
    public void test(){
//        List<SkuItemVo.SpuItemAttrGroupVo> groupWithAttrs = attrGroupDao.getAttrGroupWithAttrsBySpuId(1L, 225L);
//        System.out.println(groupWithAttrs);
        List<SkuItemSaleAttrVo> attrs = saleAttrValueDao.getSaleAttrsBySpuId(1L);
        System.out.println(attrs);
    }

    @Test
    public void contextLoads() {
       /* BrandEntity brandEntity = new BrandEntity();
        brandEntity.setName("华为");
        brandService.save(brandEntity);
        System.out.println("保存成功");*/
        List<BrandEntity> list = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1));
        list.forEach((item)->{
            System.out.println(item);
        });
    }
}
