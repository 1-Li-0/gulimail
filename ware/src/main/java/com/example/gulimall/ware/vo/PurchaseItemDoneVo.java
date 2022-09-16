package com.example.gulimall.ware.vo;

import lombok.Data;

@Data
public class PurchaseItemDoneVo {
    private Long itemId; //采购项的id
    private Integer status; //采购的状态
    private String reason; //采购失败的原因
}
