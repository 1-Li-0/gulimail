package com.example.gulimall.ware.service.impl;

import com.example.common.constant.WareConstant;
import com.example.gulimall.ware.entity.PurchaseDetailEntity;
import com.example.gulimall.ware.feign.ProductFeignService;
import com.example.gulimall.ware.service.PurchaseDetailService;
import com.example.gulimall.ware.service.WareSkuService;
import com.example.gulimall.ware.vo.MergeVo;
import com.example.gulimall.ware.vo.PurchaseDoneVo;
import com.example.gulimall.ware.vo.PurchaseItemDoneVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.common.utils.PageUtils;
import com.example.common.utils.Query;

import com.example.gulimall.ware.dao.PurchaseDao;
import com.example.gulimall.ware.entity.PurchaseEntity;
import com.example.gulimall.ware.service.PurchaseService;
import org.springframework.transaction.annotation.Transactional;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {
    @Autowired
    private PurchaseDetailService purchaseDetailService;
    @Autowired
    private WareSkuService wareSkuService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceivePurchase(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status", 0).or().eq("status", 1)
        );
        return new PageUtils(page);
    }

    @Override
    @Transactional
    public void mergePurchase(MergeVo mergeVo) {
        Long purchaseId = mergeVo.getPurchaseId();
        //如果没有对应的采购单id，则新建采购单
        if (purchaseId == null) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            Date date = new Date();
            purchaseEntity.setCreateTime(date);
            purchaseEntity.setUpdateTime(date);
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            this.save(purchaseEntity);
            PurchaseEntity purchase = this.baseMapper.selectOne(new QueryWrapper<PurchaseEntity>().eq("create_time", date).eq("update_time", date));
            purchaseId = purchase.getId();
        }
        //合并前，判断采购单的状态是否是新建（0）或已分配（1）
        PurchaseEntity purchase = this.getById(purchaseId);
        if (purchase.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() || purchase.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
            List<Long> items = mergeVo.getItems();
            Long finalPurchaseId = purchaseId;
            List<PurchaseDetailEntity> detailEntityList = items.stream()
                    .map(i -> {
                        PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
                        detailEntity.setId(i);
                        detailEntity.setPurchaseId(finalPurchaseId);
                        detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.ASSIGNED.getCode());
                        return detailEntity;
                    })
                    .collect(Collectors.toList());
            //更新采购需求表
            purchaseDetailService.updateBatchById(detailEntityList);
            //更新采购单表
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            purchaseEntity.setId(finalPurchaseId);
            purchaseEntity.setUpdateTime(new Date());
            this.updateById(purchaseEntity);
        }

    }

    @Override
    @Transactional
    public void received(List<Long> ids) {
        //判断采购单的状态是否是新建（0）或已分配（1）
        Date date = new Date();
        List<PurchaseEntity> collect = ids.stream()
                .map(id -> {
                    PurchaseEntity purchase = this.getById(id);
                    return purchase;
                })
                .filter(item -> item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() || item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode())
                .map(entity -> {
                    entity.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
                    entity.setUpdateTime(date);
                    return entity;
                })
                .collect(Collectors.toList());
        //批量修改采购单表的状态
        this.updateBatchById(collect);

        //批量修改采购单对应的采购需求表的状态
        collect.forEach(purchaseEntity -> {
            List<PurchaseDetailEntity> list = purchaseDetailService.listDetailByPurchaseId(purchaseEntity.getId());
            if (list != null && list.size() > 0) {
                list.forEach(item -> {
                    item.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                });
                purchaseDetailService.updateBatchById(list);
            }
        });
    }

    @Override
    @Transactional
    public void done(PurchaseDoneVo doneVo) {
        //修改采购需求的状态
        boolean flag = true;
        List<PurchaseItemDoneVo> items = doneVo.getItems();
        List<PurchaseDetailEntity> updates = new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
                flag = false;
                detailEntity.setStatus(item.getStatus());
            } else {
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
                //采购成功的需要入库（增加库存wms_ware_sku）
                PurchaseDetailEntity detail = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addStock(detail.getSkuId(),detail.getWareId(),detail.getSkuNum());
            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }
        purchaseDetailService.updateBatchById(updates);

        //修改采购单的状态，如果有采购项失败则改为“有异常”
        PurchaseEntity entity = new PurchaseEntity();
        entity.setId(doneVo.getId());
        entity.setUpdateTime(new Date());
        entity.setStatus(flag?WareConstant.PurchaseStatusEnum.FINISH.getCode():WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        this.updateById(entity);

    }

}