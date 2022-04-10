package com.msb.mall.mallsearch.controller;

import com.msb.mall.mallsearch.service.MallSearchService;
import com.msb.mall.mallsearch.vo.SearchParam;
import com.msb.mall.mallsearch.vo.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SearchController {

    @Autowired
    private MallSearchService mallSearchService;

    /**
     * 检索处理
     * @param param
     * @return
     */
    @GetMapping("/list.html")
    public String listPage(SearchParam param){

        // 通过对应的Service根据传递过来的相关的信息去ES中检索对应的数据
        SearchResult search = mallSearchService.search(param);
        return "index";
    }

}
