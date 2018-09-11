package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.property.JwtProperties;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.pojo.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserClient userClient;

    Logger logger = LoggerFactory.getLogger(AuthService.class);

    public String authentication(String username, String password) {

        try {
            //查询用户
            User user = userClient.queryUser(username, password);
            if (null == user){
                logger.info("用户信息不存在,{}",username);
                return null;
            }
            //创建token
            String token = JwtUtils.generateToken(new UserInfo(user.getId(),
                    user.getUsername()),jwtProperties.getPrivateKey(),jwtProperties.getExpire());
            return token;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
