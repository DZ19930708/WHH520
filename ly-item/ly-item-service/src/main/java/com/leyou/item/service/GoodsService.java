package com.leyou.item.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.mapper.*;
import com.leyou.item.pojo.*;
import com.leyou.item.bo.SpuBO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class GoodsService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BrandMapper brandMapper;


    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private SpecParamMapper specParamMapper;

    Logger logger= LoggerFactory.getLogger(GoodsService.class);




    public PageResult<SpuBO> queryGoods(String key, Boolean saleable, Integer page, Integer rows) {
        //开始分页
        PageHelper.startPage(page,rows);

        //创建查询条件
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        //saleable不为空的时候
        if(saleable!=null){
            criteria.orEqualTo("saleable",saleable);
        }
        //key不为空的时候
        if(StringUtils.isNotBlank(key)){
            criteria.andLike("title","%"+key+"%");
        }
        Page<Spu> pageInfo = (Page<Spu>) spuMapper.selectByExample(example);
        //将Spu转为SpuBO
        List<SpuBO> list=new ArrayList<>();

        pageInfo.getResult().forEach(spu -> {


            SpuBO spuBO=new SpuBO();

            BeanUtils.copyProperties(spu,spuBO);

            spuBO.setBname(brandMapper.selectByPrimaryKey(spu.getBrandId()).getName());

            List<String> list1 = categoryService.queryNameByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));


            spuBO.setCname(StringUtils.join(list1,"/"));

            list.add(spuBO);

        });

        return new PageResult<>(pageInfo.getTotal(),list);
    }

    public  void saveGoods(SpuBO spu) {
        //保存spu
        spu.setSaleable(true);
        spu.setValid(true);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());

        spuMapper.insert(spu);
        //保存spuDetial
        spu.getSpuDetail().setSpuId(spu.getId());
        spuDetailMapper.insert(spu.getSpuDetail());

        saveSkuAndStock(spu.getSkus(),spu.getId());

        sendMessage(spu.getId(),"insert");


    }
    //保存sku和库存
    @Transactional
    public void saveSkuAndStock(List<Sku> skus, Long spuId) {
        for (Sku sku : skus) {
            if(!sku.getEnable()){
                continue;
            }

            //保存sku
            sku.setSpuId(spuId);
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            skuMapper.insert(sku);

            //保存库存
            Stock stock =new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockMapper.insert(stock);

        }

    }

    public  SpuDetail findSpuById(Long id) {
       return spuDetailMapper.selectByPrimaryKey(id);
    }

    /**
     * 查sku的同时，将库存也查出来
     *
     * @return
     */
    public List<Sku> findSkuById(Long spuId) {
        Sku skus =new Sku();
        skus.setSpuId(spuId);
        List<Sku> skuList = skuMapper.select(skus);
        skuList.forEach(sku -> {
            //根据sku_id查出库存
            sku.setStock(stockMapper.selectByPrimaryKey(sku.getId()).getStock());

        });
        return skuList;


    }

    /**
     * 修改，就是将数据查出后，删除，然后插入新数据
     * @param spuBO
     */
    @Transactional
    public void updateGoods(SpuBO spuBO) {

        //查询之前的sku
        List<Sku> skus = findSkuById(spuBO.getId());
        //如果之前存在这个sku，就删除
        List<Long> ids =new ArrayList<>();
        if(!CollectionUtils.isEmpty(skus)){
            skus.forEach(sku -> {
                Long id = sku.getId();
                ids.add(id);

            });
            //删除以前的库存
            Example example=new Example(Stock.class);
           example.createCriteria().andIn("skuId",ids);
           stockMapper.deleteByExample(example);

           //删除以前的sku
           Sku sku=new Sku();
           sku.setSpuId(spuBO.getId());
           skuMapper.delete(sku);



        }
        //调用保存sku和库存的方法
        saveSkuAndStock(spuBO.getSkus(),spuBO.getId());

        //更新spu
        spuBO.setLastUpdateTime(new Date());
        spuBO.setCreateTime(null);
        spuBO.setSaleable(null);
        spuBO.setValid(null);
        spuMapper.updateByPrimaryKeySelective(spuBO);

        //更新spu详情
        spuDetailMapper.updateByPrimaryKeySelective(spuBO.getSpuDetail());

        sendMessage(spuBO.getId(),"update");

    }

    @Transactional
    public void deleteGood(Long spuId) {
        //删除Spu表中的数据
        spuMapper.deleteByPrimaryKey(spuId);

        //删除详情表的数据
        SpuDetail spuDetail =new SpuDetail();
        spuDetail.setSpuId(spuId);
        spuDetailMapper.delete(spuDetail);
        //获取到skus，删除库存表中数据
        Sku sku =new Sku();
        sku.setSpuId(spuId);

        List<Sku> skus = skuMapper.select(sku);
        skus.forEach(sku1 -> {

            Stock stock =new Stock();
            stock.setSkuId(sku1.getId());
            stockMapper.delete(stock);
        });
        skuMapper.delete(sku);

        sendMessage(spuId,"delete");
    }

    public void updateSaleableDown(Long spuId) {
        Spu spu =spuMapper.selectByPrimaryKey(spuId);
        spu.setSaleable(false);
        spuMapper.updateByPrimaryKey(spu);
    }

    public void updateSaleableUp(Long spuId) {
        Spu spu =spuMapper.selectByPrimaryKey(spuId);
        spu.setSaleable(true);
        spuMapper.updateByPrimaryKey(spu);
    }

    public Spu querySpuById(Long id) {
        return spuMapper.selectByPrimaryKey(id);
    }


    /**
     *
     * @param id 传进来的spuid
     * @param type 类型，例如 item.insert
     */
    private void sendMessage(Long id, String type){
        // 发送消息
        try {
            this.amqpTemplate.convertAndSend("item." + type, id);
        } catch (Exception e) {
            logger.error("{}商品消息发送异常，商品id：{}", type, id, e);
        }
    }

    public Sku querySkuById(Long id) {
        Sku sku = skuMapper.selectByPrimaryKey(id);
        return sku;
    }

    public List<SpecParam> queryAllSpecParam() {
        List<SpecParam> specParams = specParamMapper.selectAll();
        return specParams;
    }


}
