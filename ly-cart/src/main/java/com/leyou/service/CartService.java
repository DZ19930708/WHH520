package com.leyou.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.client.GoodsClient;
import com.leyou.common.utils.JsonUtils;
import com.leyou.interceptor.LoginInterceptor;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.SpecParam;
import com.leyou.pojo.Cart;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import sun.rmi.runtime.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    static final String KEY_PREFIX = "ly:cart:uid:";

    Logger logger = LoggerFactory.getLogger(CartService.class);


    public void addCart(Cart cart) {
        //获取登录用户
        UserInfo user = LoginInterceptor.getLoginUser();
        //保存到redis中的key
        String key =KEY_PREFIX+user.getId();
        //获取hash操作对象
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);

        //取出购物车中的skuI的,
        Long skuId=cart.getSkuId();
        Integer num =cart.getNum();

        //查询是否存在
        Boolean bool = hashOps.hasKey(skuId.toString());
        if (bool){
            //如果存在,获取购物车中的数据
            String json = hashOps.get(skuId.toString()).toString();
            cart = JsonUtils.parse(json, Cart.class);
            //修改redis中的num
            cart.setNum(cart.getNum()+num);
        }else {
            //如果不存在，添加购物车
            cart.setUserId(user.getId());
            //查询商品信息
            Sku sku = goodsClient.querySkuById(skuId);
            if (null==sku) {
                logger.error("添加购物车的商品不存在：skuId:{}", skuId);
                throw new RuntimeException();
            }

            String image= StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(),",")[0];
            cart.setImage(image);
            cart.setOwnSpec(sku.getOwnSpec());
            cart.setPrice(sku.getPrice());
            cart.setTitle(sku.getTitle());
            cart.setSpuId(sku.getSpuId());
            Map<Long,String> paramMap =new HashMap<>();
            List<SpecParam> specParams = goodsClient.queryAllSpecParam();
            specParams.forEach(specParam -> {
                paramMap.put(specParam.getId(),specParam.getName());
            });
            cart.setParamMap(paramMap);


        }
        //将购物车数据写入redis
        hashOps.put(cart.getSkuId().toString(),JsonUtils.serialize(cart));


    }

    public List<Cart> queryCartList() {
        //获取登录用户
        UserInfo user = LoginInterceptor.getLoginUser();
        //判断是否存在购物车
        String key =KEY_PREFIX+user.getId();
        if (!redisTemplate.hasKey(key)){
            return null;
        }
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        List<Object> values = hashOps.values();
        //判断购物车是否有数据
        if (CollectionUtils.isEmpty(values)){
            return null;
        }
        List<Cart> carts =new ArrayList<>();
        values.forEach(value->{
            Cart cart = JsonUtils.parse(value.toString(), Cart.class);
            carts.add(cart);
        });
        return carts;

    }

    public void updateNum(Long skuId, Integer num) {
        //获取登录用户
        UserInfo user = LoginInterceptor.getLoginUser();
        String key =KEY_PREFIX+user.getId();
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        String json = hashOps.get(skuId.toString()).toString();
        Cart cart = JsonUtils.parse(json, Cart.class);
        cart.setNum(num);
        //写入购物车
        hashOps.put(skuId.toString(),JsonUtils.serialize(cart));


    }

    public void deleteCart(String skuId) {
        //获取登录用户
        UserInfo user = LoginInterceptor.getLoginUser();
        String key=KEY_PREFIX+user.getId();
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        hashOps.delete(skuId);
    }

    public Integer queryNum() {
        //获取登陆用户
        UserInfo user = LoginInterceptor.getLoginUser();
        String key =KEY_PREFIX+user.getId();
        BoundHashOperations<String, Object, Object> hashOps = redisTemplate.boundHashOps(key);
        List<Object> values = hashOps.values();
        //判断购物车是否有数据
        if (CollectionUtils.isEmpty(values)){
            return 0;
        }
        int num=0;
        for (Object value : values) {
            Cart cart = JsonUtils.parse(value.toString(), Cart.class);
            num +=cart.getNum();
        }
        return num;
    }
}
