package com.msb.mall.mallsearch.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msb.common.dto.es.SkuESModel;
import com.msb.mall.mallsearch.config.MallElasticSearchConfiguration;
import com.msb.mall.mallsearch.constant.ESConstant;
import com.msb.mall.mallsearch.service.ElasticSearchSaveService;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ElasticSearchSaveServiceImpl implements ElasticSearchSaveService {

    @Autowired
    private RestHighLevelClient client;

    /**
     * 实现上架数据存储到ES的操作
     * @param skuESModels
     * @return
     */
    @Override
    public Boolean productStatusUp(List<SkuESModel> skuESModels) throws IOException {
        // 1.创建对应索引库 --》 创建对应的Product的映射
        // BulkRequest bulkRequest, RequestOptions options
        BulkRequest bulkRequest = new BulkRequest();
        for (SkuESModel skuESModel : skuESModels) {
            // 2.绑定对应的索引库  product
            IndexRequest indexRequest = new IndexRequest(ESConstant.PRODUCT_INDEX);
            // 3.设置id
            indexRequest.id(skuESModel.getSkuId().toString());
            // 4.设置文档
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(skuESModel);
            indexRequest.source(json, XContentType.JSON);
            // 5.转换后的数据封装到Bulk中
            bulkRequest.add(indexRequest);
        }
        // 6.批量向ElasticSearch中保存数据
        BulkResponse bulk = client.bulk(bulkRequest, MallElasticSearchConfiguration.COMMON_OPTIONS);
        // 7.获取批量操作是否有错误
        boolean b = bulk.hasFailures();
        return !b;
    }
}
