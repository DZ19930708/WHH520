package com.leyou.controller;

import com.leyou.pojo.Cart;
import com.leyou.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping
    public ResponseEntity<Void> addCart(@RequestBody Cart cart){
        cartService.addCart(cart);
        return ResponseEntity.ok().build();

    }

    @GetMapping
    public ResponseEntity<List<Cart>> queryCartList(){
        List<Cart> carts =cartService.queryCartList();
        if (carts == null) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        }
        return ResponseEntity.ok(carts);
    }

    @PutMapping
    public ResponseEntity<Void> updateNum(
            @RequestParam("skuId") Long skuId,
            @RequestParam("num") Integer num
    ){
        cartService.updateNum(skuId,num);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{skuId}")
    public ResponseEntity<Void> deleteCart(@PathVariable("skuId") String skuId){
            cartService.deleteCart(skuId);
            return ResponseEntity.ok().build();
    }

    @GetMapping("num")
    public ResponseEntity<Integer> queryNum(){
        Integer num =cartService.queryNum();
        if (num ==null){
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        return ResponseEntity.ok(num);
    }

}
