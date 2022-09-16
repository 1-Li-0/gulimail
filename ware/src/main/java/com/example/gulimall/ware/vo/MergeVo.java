package com.example.gulimall.ware.vo;

import lombok.Data;

import java.util.List;

@Data
public class MergeVo {
    private Long purchaseId; //采购单id
    private List<Long> items; //整合的采购需求集合
}
