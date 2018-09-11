package com.leyou.search.test;

import com.leyou.LySearchService;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBO;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.service.IndexService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import com.leyou.search.repository.GoodsRepository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = LySearchService.class)
public class TestCategoryClient {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsRepository goodsRepository;

    @Autowired
    private ElasticsearchTemplate elasticsearchTemplate;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private IndexService indexService;


    /**
     * 这里使用的是 categoryClient,调用了CategoryApi,然后根据方法名和类上的url路径，到controller中查找对应的方法
     */
    @Test
    public void test1() {
        List<String> list = categoryClient.queryNameByIds(Arrays.asList(1L, 2L, 3L));
        list.forEach(name-> System.out.println("分类名称: " + name));

    }

    @Test
    public void test2() {
        //创建索引库
        elasticsearchTemplate.createIndex(Goods.class);
        //创建映射关系
        elasticsearchTemplate.putMapping(Goods.class);
    }

    @Test
    public void test3() {
        int page =1;
        int rows=100;
        int size =0;

       do {
           //查询spu

        PageResult<SpuBO> pageResult = goodsClient.queryGoods(null, true, page, rows);
        List<SpuBO> items = pageResult.getItems();
        List<Goods> list =new ArrayList<>();
        items.forEach(spu->{
            Goods goods = indexService.buildGoods(spu);
            list.add(goods);
        });
        //将goods添加到索引库
        goodsRepository.saveAll(list);
        size=items.size();
        page++;
       }while ( size==100);
    }
}
