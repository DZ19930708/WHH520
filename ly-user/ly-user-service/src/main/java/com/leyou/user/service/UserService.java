package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.pojo.User;
import com.leyou.user.mapper.UserMapper;
import com.leyou.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;


@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    Logger logger=LoggerFactory.getLogger(UserService.class);

    /**
     *
     * @param data 要校验的数据
     * @param type 1表示用户名，2表示手机号
     * @return
     */
    public Boolean checkUserData(String data, Integer type) {
        User user =new User();
        switch (type){
            case 1:
                user.setUsername(data);
                break;
            case 2:
                user.setPhone(data);
                break;
             default:
                 return null;

        }
        return userMapper.selectCount(user) == 0;
    }

    public Boolean sendVerifyCode(String phone) {
            //获取验证码
            String code = NumberUtils.generateCode(6);
        try {
            Map<String,String> msg =new HashMap();
            msg.put("phone",phone);
            msg.put("code",code);
            //发送短信
            amqpTemplate.convertAndSend("ly.sms.exchange","sms.verify.code",msg);
            //将code存到redis
            stringRedisTemplate.opsForValue().set("ly."+phone,code,5, TimeUnit.MINUTES);

            return true;
        } catch (AmqpException e) {
            logger.error("发送短信失败,电话:{},code:{}",phone,code);
            return false;
        }
    }

    public Boolean register(User user, String code) {
        if(StringUtils.isBlank(code)){
            return false;
        }
        //手机号，需要拼接
        String phone ="ly."+user.getPhone();
        //从redis中取出验证码，与用户输入的验证码进行比较
        String checkCode = stringRedisTemplate.opsForValue().get(phone);
        //校验验证码是否正确
        if (!code.equals(checkCode)){
            return false;
        }
        //添加id和创建时间
        user.setId(null);
        user.setCreated(new Date());
        //添加盐值
        String salt= CodecUtils.generateSalt();
        user.setSalt(salt);
        //对密码进行加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));
        //将数据添加到数据库
        Boolean bool =userMapper.insertSelective(user)==1;
        //如果注册成功了，将redis中的验证码删掉
          if (bool){
              try {
                  stringRedisTemplate.delete(phone);
              } catch (Exception e) {
                  logger.error("删除缓存验证码失败，code：{}", code, e);
              }
          }
        return bool;


    }

    public User queryUser(String username,String password) {
        //通过用户名查询用户
        User queryUser =new User();
        queryUser.setUsername(username);
        User storeUser = userMapper.selectOne(queryUser);
        //如果没查到用户
        if (null == storeUser){
            return null;
        }
        //校验密码，如果密码不正确
        String password1=CodecUtils.md5Hex(password,storeUser.getSalt());
        if (!password1.equals(storeUser.getPassword())){
            return null;
        }
        //密码和用户名都正确
        return storeUser;


    }
}
