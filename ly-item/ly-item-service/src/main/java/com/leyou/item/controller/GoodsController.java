package com.leyou.item.controller;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBO;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.SpecParam;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.service.GoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
public class GoodsController {

    @Autowired
    private GoodsService goodsService;

    @GetMapping("spu/page")
    public ResponseEntity<PageResult<SpuBO>> queryGoods(
            @RequestParam(value = "key", required = false) String key,
            @RequestParam(value = "saleable",required = false) Boolean saleable,
            @RequestParam(value = "page",defaultValue = "1") Integer page,
            @RequestParam(value = "rows",defaultValue = "5") Integer rows
    ) {
           PageResult<SpuBO> pageResult= goodsService.queryGoods(key,saleable,page,rows);
           if(null==pageResult){
               return new ResponseEntity<>(HttpStatus.NO_CONTENT);
           }
           return ResponseEntity.ok(pageResult);
    }

    @PostMapping("goods")
    public ResponseEntity<Void> saveGoods(@RequestBody SpuBO spu){
        try {
            goodsService.saveGoods(spu);
            return new ResponseEntity<>(HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }


    }

    //http://api.leyou.com/api/item/spu/detail/2
    @GetMapping("spu/detail/{id}")
    public ResponseEntity<SpuDetail> findSpuById(@PathVariable("id") Long id){
        SpuDetail spuDetail=goodsService.findSpuById(id);
        if(spuDetail==null){
            return  new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(spuDetail);
    }

    //http://api.leyou.com/api/item/sku/list?id=2
    @GetMapping("sku/list")
    public ResponseEntity<List<Sku>> findSkuById(@RequestParam("id") Long id){
       List<Sku> list =goodsService.findSkuById(id);
       if(null==list || list.size()<1){
           return new ResponseEntity<>(HttpStatus.NOT_FOUND);
       }
       return ResponseEntity.ok(list);
    }

    //http://api.leyou.com/api/item/goods
    @PutMapping
    public ResponseEntity<Void> updateGoods(@RequestBody SpuBO spuBO){
        try {
            goodsService.updateGoods(spuBO);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @DeleteMapping("spu/delete/{id}")
    public ResponseEntity<Void> deleteGood(@PathVariable("id") Long spuId){
        goodsService.deleteGood(spuId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    //spu/down/3
    @PutMapping("spu/down/{id}")
    public ResponseEntity<Void> updateSaleableDown(@PathVariable("id") Long spuId){
        goodsService.updateSaleableDown(spuId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PutMapping("spu/up/{id}")
    public ResponseEntity<Void> updateSaleableUp(@PathVariable("id") Long spuId){
        goodsService.updateSaleableUp(spuId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("spu/{id}")
    public ResponseEntity<Spu> querySpuById(@PathVariable("id") Long id){
        Spu spu = this.goodsService.querySpuById(id);
        if(spu == null){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return ResponseEntity.ok(spu);
    }

    @GetMapping("sku/{id}")
    public ResponseEntity<Sku> querySkuById(@PathVariable("id") Long id){
        Sku sku =  this.goodsService.querySkuById(id);
        if(sku == null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return ResponseEntity.ok(sku);
    }

    @GetMapping("paramMap")
    public ResponseEntity<List<SpecParam>> queryAllSpecParam(){
        List<SpecParam> specParams = goodsService.queryAllSpecParam();

        if (specParams == null || specParams.size()==0){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return ResponseEntity.ok(specParams);

    }




}
