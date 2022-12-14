package com.xunqi.gulimall.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.example.common.to.es.SkuEsModel;
import com.xunqi.gulimall.search.config.GulimallElasticSearchConfig;
import com.xunqi.gulimall.search.constant.EsConstant;
import com.xunqi.gulimall.search.service.MallSearchService;
import com.xunqi.gulimall.search.vo.SearchParam;
import com.xunqi.gulimall.search.vo.SearchResult;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MallSearchServiceImpl implements MallSearchService {
    @Autowired
    private RestHighLevelClient client; //es???????????????

    @Override
    public SearchResult search(SearchParam param) {
        SearchResult result = null;
        SearchRequest searchRequest = buildSearchRequest(param);
        try {
            SearchResponse response = client.search(searchRequest, GulimallElasticSearchConfig.COMMON_OPTIONS);
            result = buildSearchResult(response, param);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * ??????????????????
     * ???????????????????????????????????????????????????????????????
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {
        //????????????dls???????????????dls??????
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //?????????????????????
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        if (!StringUtils.isEmpty(param.getKeyword())) {
            queryBuilder.must(QueryBuilders.matchQuery("skuTitle", param.getKeyword()));
        }
        if (param.getCatalog3Id() != null) {
            queryBuilder.filter(QueryBuilders.termQuery("catalogId", param.getCatalog3Id()));
        }
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            queryBuilder.filter(QueryBuilders.termsQuery("brandId", param.getBrandId()));
        }
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            //?????????????????????????????????????????????????????????
            for (String attrStr : param.getAttrs()) {
                //????????????????????? &attrs=1_16GB:32GB&attrs=2_4.2??????:5.2??????
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                String[] s = attrStr.split("_");
                String attrId = s[0];
                String[] attrValues = s[1].split(":");
                boolQuery.must(QueryBuilders.termQuery("attrs.attrId", attrId));
                boolQuery.must(QueryBuilders.termsQuery("attrs.attrValue", attrValues));
                NestedQueryBuilder nestedQuery = QueryBuilders.nestedQuery("attrs", boolQuery, ScoreMode.None);
                queryBuilder.filter(nestedQuery);
            }
        }
        if (param.getHasStock() != null) {
            //1????????????????????????true
            queryBuilder.filter(QueryBuilders.termQuery("hasStock", param.getHasStock() == 1));
        }
        if (!StringUtils.isEmpty(param.getSkuPrice())) {
            //???????????? skuPrice=100_500
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("skuPrice");
            String[] strings = param.getSkuPrice().split("_");
            if (param.getSkuPrice().startsWith("_")) {
                rangeQuery.lte(strings[1]);
            } else if (param.getSkuPrice().endsWith("_")) {
                rangeQuery.gte(strings[0]);
            } else if (strings.length == 2) {
                rangeQuery.gte(strings[0]).lte(strings[1]);
            }
            queryBuilder.filter(rangeQuery);
        }
        if (queryBuilder != null) {
            sourceBuilder.query(queryBuilder);
        }
        //??????
        if (!StringUtils.isEmpty(param.getSort())) {
            //???????????? sort=hotScore_desc
            String[] s = param.getSort().split("_");
            sourceBuilder.sort(s[0], s[1].equalsIgnoreCase("desc") ? SortOrder.DESC : SortOrder.ASC);
        }
        //??????
        if (param.getPageNum() != null) {
            sourceBuilder.from((param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE);
            sourceBuilder.size(EsConstant.PRODUCT_PAGESIZE);
        }
        //??????
        if (!StringUtils.isEmpty(param.getKeyword())) {
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuTitle");
            highlightBuilder.preTags("<b style='color:red'>");
            highlightBuilder.postTags("</b>");
            sourceBuilder.highlighter(highlightBuilder);
        }
        //??????
        TermsAggregationBuilder brand_agg = AggregationBuilders.terms("brand_agg");
        brand_agg.field("brandId").size(50);
        brand_agg.subAggregation(AggregationBuilders.terms("brand_name_agg").field("brandName").size(1));
        brand_agg.subAggregation(AggregationBuilders.terms("brand_img_agg").field("brandImg").size(1));
        //TODO ??????????????????
        sourceBuilder.aggregation(brand_agg);

        TermsAggregationBuilder catalog_agg = AggregationBuilders.terms("catalog_agg");
        catalog_agg.field("catalogId").size(50);
        catalog_agg.subAggregation(AggregationBuilders.terms("catalog_name_agg").field("catalogName").size(1));
        //TODO ??????????????????
        sourceBuilder.aggregation(catalog_agg);

        NestedAggregationBuilder attr_agg = AggregationBuilders.nested("attr_agg", "attrs");
        TermsAggregationBuilder attr_id_agg = AggregationBuilders.terms("attr_id_agg").field("attrs.attrId");
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_name_agg").field("attrs.attrName").size(1));
        attr_id_agg.subAggregation(AggregationBuilders.terms("attr_value_agg").field("attrs.attrValue").size(50));
        //??????????????????
        attr_agg.subAggregation(attr_id_agg);
        //TODO ??????????????????
        sourceBuilder.aggregation(attr_agg);

        return new SearchRequest(new String[]{EsConstant.PRODUCT_INDEX}, sourceBuilder);
    }

    /**
     * ??????????????????
     *
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse response, SearchParam param) {
        SearchResult result = new SearchResult();
        //??????????????????????????????????????????????????????
        SearchHits hits = response.getHits();
        //????????????
        long totalHits = hits.getTotalHits().value;
        result.setTotal(totalHits);
        int totalPage = (int) Math.ceil(totalHits / EsConstant.PRODUCT_PAGESIZE);
        result.setTotalPages(totalPage);
        List<Integer> pageNavs = new ArrayList<>();
        for (int i = 1; i <= totalPage; i++) {
            pageNavs.add(i);
        }
        result.setPageNavs(pageNavs);
        result.setPageNum(param.getPageNum());
        //????????????
        List<SkuEsModel> products = new ArrayList<>();
        if (hits.getHits() != null && hits.getHits().length > 0) {
            for (SearchHit hit : hits.getHits()) {
                String source = hit.getSourceAsString();
                SkuEsModel product = JSON.parseObject(source, SkuEsModel.class);
                //?????????????????????
                if (!StringUtils.isEmpty(param.getKeyword())) {
                    //???????????????????????????skuTitle
                    HighlightField skuTitle = hit.getHighlightFields().get("skuTitle");
                    String string = skuTitle.getFragments()[0].string();
                    //????????????skuTitle??????????????????????????????
                    product.setSkuTitle(string);
                }
                products.add(product);
            }
        }
        result.setProducts(products);
        //????????????
        ParsedLongTerms catalog_agg = response.getAggregations().get("catalog_agg");
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        if (catalog_agg.getBuckets() != null && catalog_agg.getBuckets().size() > 0) {
            for (Terms.Bucket catalog : catalog_agg.getBuckets()) {
                SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
                String idStr = catalog.getKeyAsString();
                catalogVo.setCatalogId(Long.parseLong(idStr));
                ParsedStringTerms catalog_name_agg = catalog.getAggregations().get("catalog_name_agg");
                String catalog_name = catalog_name_agg.getBuckets().get(0).getKeyAsString();
                catalogVo.setCatalogName(catalog_name);
                catalogVos.add(catalogVo);
            }
        }
        result.setCatalogs(catalogVos);
        //????????????
        ParsedLongTerms brand_agg = response.getAggregations().get("brand_agg");
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        if (brand_agg.getBuckets() != null && brand_agg.getBuckets().size() > 0) {
            for (Terms.Bucket brand : brand_agg.getBuckets()) {
                SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
                brandVo.setBrandId(brand.getKeyAsNumber().longValue());
                ParsedStringTerms brand_name_agg = brand.getAggregations().get("brand_name_agg");
                brandVo.setBrandName(brand_name_agg.getBuckets().get(0).getKeyAsString());
                ParsedStringTerms brand_img_agg = brand.getAggregations().get("brand_img_agg");
                brandVo.setBrandImg(brand_img_agg.getBuckets().get(0).getKeyAsString());
                brandVos.add(brandVo);
            }
        }
        result.setBrands(brandVos);
        //????????????
        ParsedNested attr_agg = response.getAggregations().get("attr_agg");
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        ParsedLongTerms attr_id_agg = attr_agg.getAggregations().get("attr_id_agg");
        if (attr_id_agg.getBuckets() != null && attr_id_agg.getBuckets().size() > 0) {
            for (Terms.Bucket bucket : attr_id_agg.getBuckets()) {
                SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
                long attrId = bucket.getKeyAsNumber().longValue();
                attrVo.setAttrId(attrId);

                ParsedStringTerms attr_name_agg = bucket.getAggregations().get("attr_name_agg");
                attrVo.setAttrName(attr_name_agg.getBuckets().get(0).getKeyAsString());

                ParsedStringTerms attr_value_agg = bucket.getAggregations().get("attr_value_agg");
                List<String> attrValues = attr_value_agg.getBuckets().stream().map(v -> v.getKeyAsString()).collect(Collectors.toList());
                attrVo.setAttrValue(attrValues);

                attrVos.add(attrVo);
            }
        }
        result.setAttrs(attrVos);

        //??????????????????????????????
        List<SearchResult.NavVo> collect = result.getNavVos();
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            param.getAttrs().forEach(attr -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                //????????????attrs=1_5.2???:6???
                String[] s = attr.split("_");
                long attrId = Long.parseLong(s[0]);
                result.getAttrIds().add(attrId);
                navVo.setNavId(attrId);
                navVo.setNavValue(s[1]);
                //????????????id??????????????????????????????
                if (result.getAttrs() != null && result.getAttrs().size() > 0) {
                    SearchResult.AttrVo attrVo = result.getAttrs().stream().filter(vo -> Long.parseLong(s[0]) == vo.getAttrId()).findFirst().get();
                    navVo.setNavName(attrVo.getAttrName());
                } else {
                    navVo.setNavName(s[0]);
                }
                String replace = replaceQueryString(param, attr, "attrs");
                navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                collect.add(navVo);
            });

        }
        //???????????????????????????????????????
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            param.getBrandId().forEach(brandId -> {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                navVo.setNavId(brandId);
                navVo.setNavName("??????");
                if (result.getBrands() != null && result.getBrands().size() > 0) {
                    List<SearchResult.BrandVo> brandVoList = result.getBrands().stream().filter(brandVo1 -> brandVo1.getBrandId() == brandId).collect(Collectors.toList());
                    StringBuffer buffer = new StringBuffer();
                    String replace = "";
                    for (SearchResult.BrandVo brandVo : brandVoList) {
                        buffer.append(brandVo.getBrandName() + ";");
                        replace = replaceQueryString(param, brandVo.getBrandId().toString(), "brandId");
                    }
                    navVo.setNavValue(buffer.toString());
                    navVo.setLink("http://search.gulimall.com/list.html?" + replace);
                }
                collect.add(navVo);
            });
        }
        if (param.getCatalog3Id() != null) {
            SearchResult.NavVo navVo = new SearchResult.NavVo();
            navVo.setNavId(param.getCatalog3Id());
            navVo.setNavName("??????");
            if (result.getCatalogs() != null && result.getCatalogs().size() > 0) {
                List<SearchResult.CatalogVo> catalogVoList = result.getCatalogs().stream().filter(catalogVo -> catalogVo.getCatalogId() == param.getCatalog3Id()).collect(Collectors.toList());
                StringBuffer buffer = new StringBuffer();
                for (SearchResult.CatalogVo catalogVo : catalogVoList) {
                    buffer.append(catalogVo.getCatalogName() + ";");
                }
                navVo.setNavValue(buffer.toString());
            }
            collect.add(navVo);
        }
        result.setNavVos(collect);
        return result;
    }

    /**
     * @param param ????????????
     * @param value ??????????????????????????????
     * @param key   ?????????????????????????????????
     * @return
     */
    private String replaceQueryString(SearchParam param, String value, String key) {
        //????????????????????????????????????????????????????????????????????????
        String encode = null;
        try {
            //???????????????????????????????????????
            encode = URLEncoder.encode(value, "UTF-8");
            //?????????????????????????????????+???????????????
            encode = encode.replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String replace = param.get_queryString().replace("&" + key + "=" + encode, "");
        return replace;
    }
}
