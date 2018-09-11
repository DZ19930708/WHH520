package com.leyou.item.filter;

import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import com.leyou.item.config.FilterProperties;
import com.leyou.item.config.JwtProperties;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginFilter extends ZuulFilter {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private FilterProperties filterProperties;


    @Override
    public String filterType() {
        //前缀拦截
        return "pre";
    }

    @Override
    public int filterOrder() {
        return 1;
    }

    @Override
    public boolean shouldFilter() {
        //获取上下文
        RequestContext currentContext = RequestContext.getCurrentContext();
        //获取request
        HttpServletRequest request = currentContext.getRequest();
        //获取uri
        String requestURI = request.getRequestURI();
        //获取到所有需要放行的uri
        List<String> allowPaths = filterProperties.getAllowPaths();
        boolean flag =true;
        for (String allowPath : allowPaths) {
            if (requestURI.startsWith(allowPath)){
                flag=false;
                break;
            }
        }
        return flag;
    }

    @Override
    public Object run() throws ZuulException {
        //获取上下文
        RequestContext currentContext = RequestContext.getCurrentContext();
        //获取request
        HttpServletRequest request = currentContext.getRequest();
        //获取token
        String token = CookieUtils.getCookieValue(request, jwtProperties.getCookieName());
        //校验
        try {
        //校验通过就放行
            JwtUtils.getInfoFromToken(token,jwtProperties.getPublicKey());

        } catch (Exception e) {
        //没通过就报401
            currentContext.setSendZuulResponse(false);
            currentContext.setResponseStatusCode(401);
        }

        return null;
    }
}
