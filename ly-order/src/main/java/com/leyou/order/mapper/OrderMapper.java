package com.leyou.order.mapper;

import com.leyou.order.pojo.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface OrderMapper extends tk.mybatis.mapper.common.Mapper<Order> {

    //数据库三表联查,订单表，订单详情表，订单状态表
    List<Order> queryOrderList(
            @Param("userId") Long userId,
            @Param("status") Integer status);
}
