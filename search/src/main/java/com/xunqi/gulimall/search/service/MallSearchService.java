package com.xunqi.gulimall.search.service;

import com.xunqi.gulimall.search.vo.SearchParam;
import com.xunqi.gulimall.search.vo.SearchResult;

public interface MallSearchService {
    SearchResult search(SearchParam param);
}
