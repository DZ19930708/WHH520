package com.leyou.user.controller;

import com.leyou.pojo.User;
import com.leyou.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("check/{data}/{type}")
    public ResponseEntity<Boolean> checkUserData(@PathVariable("data") String data,@PathVariable("type")Integer type){
        Boolean bool = userService.checkUserData(data,type);
        //bool的返回值有三种类型,true,false,null
        if (null == bool){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(bool);
    }

    /**
     * 获取验证码
     * @param phone 用户输入的手机号
     * @return
     */
    @PostMapping("code")
    public ResponseEntity<Void> sendVerifyCode(@RequestParam("phone") String phone){
        Boolean bool =userService.sendVerifyCode(phone);
        if (null == bool || !bool){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }


    /**
     * 注册
     * @param user 包括用户名，密码，手机号等
     * @param code 验证码
     * @return
     */
    @PostMapping("register")
    public ResponseEntity<Void> register(@Valid User user, @RequestParam("code") String code){
        Boolean bool =userService.register(user,code);
        if (null == bool || !bool){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     *登录，根据用户名和密码，查询用户是否存在
     */
    @PostMapping("query")
    public ResponseEntity<User> queryUser(
            @RequestParam("username") String username,
            @RequestParam("password") String password
    ){
        User storeUser=userService.queryUser(username,password);
        if (null == storeUser ){
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok(storeUser);
    }

    /**
     * 新增收货地址
     */


}
