package com.leyou.item.api;

import com.leyou.item.pojo.Category;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("category")
public interface CategoryApi {


    @GetMapping("list")
    List<Category> queryByPid(@RequestParam(value = "pid",defaultValue = "0") Long pid);

    /**
     * 通过pid查询商品分类
     * @return
     */
    @GetMapping("bid/{bid}")
    List<Category>queryByBrandId(@PathVariable("bid") Long bid);

    @GetMapping("names")
    List<String> queryNameByIds(@RequestParam("ids") List<Long> ids);
}
