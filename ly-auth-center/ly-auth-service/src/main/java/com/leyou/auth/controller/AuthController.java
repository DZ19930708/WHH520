package com.leyou.auth.controller;

import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.property.JwtProperties;
import com.leyou.auth.service.AuthService;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.pojo.User;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@RestController
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("accredit")
    public ResponseEntity<Void> authentication(
            @RequestParam("username")String username,
            @RequestParam("password")String password,
            HttpServletRequest request,
            HttpServletResponse response

    ){
        //登录校验
        String token=authService.authentication(username,password);
        if (StringUtils.isBlank(token)){
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        //将token写入cookie,并指定httponly为true
        CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),token,
                jwtProperties.getCookieMaxAge(),null,true);

        return ResponseEntity.ok().build();
    }

    /**
     * 返回值为UserInfo
     * @param token
     * @return
     */
    @GetMapping("verify")
    public ResponseEntity<UserInfo> verifyUser(@CookieValue("LY_TOKEN")String token,
                                                HttpServletRequest request,
                                                HttpServletResponse response){
        try {
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
            if (null==userInfo){
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
            //如果成功，我们需要刷新token
            String newToken=JwtUtils.generateToken(userInfo,jwtProperties.getPrivateKey(),jwtProperties.getExpire());

            //写入cookie中
            CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),newToken,jwtProperties.getCookieMaxAge(),null,true);


            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            //抛出异常，证明token无效，返回401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

    @GetMapping("clear")
    public ResponseEntity<UserInfo> clearUser(@CookieValue("LY_TOKEN")String token,
                                               HttpServletRequest request,
                                               HttpServletResponse response){
        try {
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());



            //写入cookie中
            CookieUtils.setCookie(request,response,jwtProperties.getCookieName(),null,0,null,true);

            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            //抛出异常，证明token无效，返回401
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
    }

}
