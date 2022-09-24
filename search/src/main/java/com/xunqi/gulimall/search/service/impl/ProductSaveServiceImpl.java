package com.xunqi.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.common.to.es.SkuEsModel;
import com.xunqi.gulimall.search.config.GulimallElasticSearchConfig;
import com.xunqi.gulimall.search.constant.EsConstant;
import com.xunqi.gulimall.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ProductSaveServiceImpl implements ProductSaveService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModels) throws IOException {
        //创建索引
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuEsModel model : skuEsModels) {
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            indexRequest.id(model.getSkuId().toString());
            String s = JSON.toJSONString(model);
            indexRequest.source(s, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
        //TODO 如果批量保存有失败，需要处理
        boolean b = bulk.hasFailures();
        BulkItemResponse[] items = bulk.getItems();
        List<String> collect = Arrays.stream(items).map(item -> item.getId()).collect(Collectors.toList());
        log.info("商品上架完成：{}",collect);

        return b;
    }
}
