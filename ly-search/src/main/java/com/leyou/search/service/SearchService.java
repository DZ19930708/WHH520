package com.leyou.search.service;

import com.leyou.item.pojo.Brand;
import com.leyou.item.pojo.Category;
import com.leyou.item.pojo.SpecParam;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SearchService {

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private BrandClient brandClient;

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private SpecificationClient specificationClient;

    private Logger logger= LoggerFactory.getLogger(SearchService.class);

    public SearchResult search(SearchRequest searchRequest) {

        //初始化分类和品牌
        List<Category> categories=null;
        List<Brand> brands=null;
        //获取到key
        String key = searchRequest.getKey();
        //获取到page,用于分页,要-1，因为，是从0开始的
        Integer page = searchRequest.getPage()-1;
        //获取到size，用于分页
        Integer size = searchRequest.getSize();

        //如果用户什么都没搜，就 返回null
        if (key==null){
            return null;
        }
        //构建查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        //对结果进行筛选
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","skus","subTitle"},null));
        //基本查询

        queryBuilder.withQuery(QueryBuilders.matchQuery("all",key));
        //分页
        queryBuilder.withPageable(PageRequest.of(page,size));
        //基本查询
        QueryBuilder query=buildBasicQueryWithFilter(searchRequest);
        queryBuilder.withQuery(query);


        //排序
        //是否降序
        Boolean descending = searchRequest.getDescending();
        //获取排序字段
        String sortBy = searchRequest.getSortBy();

        if (StringUtils.isNotBlank(sortBy)){
            //如果排序字段不为空，就进行排序
            queryBuilder.withSort(SortBuilders.fieldSort(sortBy).order(descending ? SortOrder.DESC : SortOrder.ASC));

        }

        //聚合
        String categoryAggName = "category"; // 商品分类聚合名称
        String brandAggName = "brand"; // 品牌聚合名称
        //商品分类进行聚合
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        //品牌进行聚合
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        //执行查询
        AggregatedPage<Goods> goodsPage = (AggregatedPage<Goods>) goodsRepository.search(queryBuilder.build());
        //总记录数
        Long total = goodsPage.getTotalElements();
        //总页数
        Long totalPages = (total + size -1 ) /size;


        // 解析数据
        categories= getCategoryAggResult(goodsPage.getAggregation(categoryAggName));
        brands=getBrandAggResult(goodsPage.getAggregation(brandAggName));


        //判断商品分类数量，看是否需要对规格参数进行聚合
        List<Map<String,Object>> specs=null;
        //判断商品是否只属于一个分类
        if (categories.size() == 1) {
            // 如果分类只剩下一个，才进行规格参数过滤
            specs = getSpecs(categories.get(0).getId(),query);
        }

        return new SearchResult(total,totalPages, goodsPage.getContent(),categories,brands,specs);
    }

    private QueryBuilder buildBasicQueryWithFilter(SearchRequest searchRequest) {

        BoolQueryBuilder qurryBuilder = QueryBuilders.boolQuery();
        //基本查询条件
        qurryBuilder.must(QueryBuilders.matchQuery("all",searchRequest.getKey()));
        //过滤条件过滤器
        BoolQueryBuilder filterQueryBuilder = QueryBuilders.boolQuery();
        //获取过滤条件
        Map<String, String> filter = searchRequest.getFilter();
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            //获取key和value 例如:key:操作系统 value:Andriod
            String key = entry.getKey();
            String value = entry.getValue();
            if (!key.equals("cid") && !key.equals("brandId")){
                //由于kibana,默认在这些规格参数前加了spec. 所以需要拼接，又由于，该字段不可分词，所以，需要在后面加".keyword"
                key = "specs." + key + ".keyword";
            }
            filterQueryBuilder.must(QueryBuilders.termQuery(key,value));

        }
        //将过滤和基本查询关联起来
        qurryBuilder.filter(filterQueryBuilder);

        return qurryBuilder;


    }

    // 聚合规格参数
    private List<Map<String, Object>> getSpecs(Long cid, QueryBuilder query) {

            // 根据分类查询规格
            List<SpecParam> params =
                    this.specificationClient.querySpecParam(null, cid, true, null);

            // 创建集合，保存规格过滤条件
            List<Map<String, Object>> specs = new ArrayList<>();

            NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
            queryBuilder.withQuery(query);

            // 聚合规格参数
            params.forEach(p -> {
                String key = p.getName();
                queryBuilder.addAggregation(AggregationBuilders.terms(key).field("specs." + key + ".keyword"));

            });

            // 查询
            AggregatedPage<Goods> aggregatedPage= (AggregatedPage<Goods>) goodsRepository.search(queryBuilder.build());
            Map<String, Aggregation> aggs = aggregatedPage.getAggregations().asMap();

            // 解析聚合结果
            params.forEach(param -> {
                Map<String, Object> spec = new HashMap<>();
                String key = param.getName();

                StringTerms terms = (StringTerms) aggs.get(key);
                List<String> val =new ArrayList<>();
                terms.getBuckets().forEach(bucket -> {
                    val.add(bucket.getKeyAsString());
                });
                spec.put("options",val);
                spec.put("k", key);
                specs.add(spec);
            });

            return specs;

    }

    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        try {
            LongTerms brandAgg = (LongTerms) aggregation;
            List<Long> bids=new ArrayList<>();
            for (LongTerms.Bucket bucket : brandAgg.getBuckets()) {
                bids.add(bucket.getKeyAsNumber().longValue());
            }
            return brandClient.queryBrandByIds(bids);
        } catch (Exception e) {
           logger.error("品牌聚合出现异常,"+e);
           return null;
        }

    }

    private List<Category> getCategoryAggResult(Aggregation aggregation) {
        try {
            LongTerms aggs = (LongTerms) aggregation;
            List<Long> cids=new ArrayList<>();
            for (LongTerms.Bucket bucket : aggs.getBuckets()) {
                cids.add(bucket.getKeyAsNumber().longValue());
            }
            List<Category> categories=new ArrayList<>();
            List<String> names = categoryClient.queryNameByIds(cids);
            for (int i = 0; i < names.size(); i++) {
                Category category=new Category();
                category.setId(cids.get(i));
                category.setName(names.get(i));
                categories.add(category);
            }
            return categories;
        } catch (Exception e) {
            logger.error("品牌聚合出现异常"+e);
            return null;
        }

    }
}
