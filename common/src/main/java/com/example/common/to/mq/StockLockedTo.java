package com.example.common.to.mq;

import lombok.Data;

@Data
public class StockLockedTo {
    private Long id; //工作单id
    private StockDetailTo detail; //详情信息
}
