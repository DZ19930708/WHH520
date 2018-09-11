package com.leyou.item.service;

import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Brand;
import com.leyou.item.pojo.Category;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;




    public List<Category> queryByPid(Long pid) {
        Category category =new Category();
        category.setParentId(pid);
        return categoryMapper.select(category);
    }


    public List<Category> queryByBrandId(Long bid) {
        return categoryMapper.queryByBrandId(bid);
    }



    public List<String> queryNameByIds(List<Long> ids) {
        List<String> list=new ArrayList<>();
        List<Category> categoryList = categoryMapper.selectByIdList(ids);
        categoryList.forEach(category -> {
            String name = category.getName();
            list.add(name);

        });
        return list;
    }

    public List<Category> queryAllByCid3(Long id) {
        Category c3 = this.categoryMapper.selectByPrimaryKey(id);
        Category c2 = this.categoryMapper.selectByPrimaryKey(c3.getParentId());
        Category c1 = this.categoryMapper.selectByPrimaryKey(c2.getParentId());
        return Arrays.asList(c1,c2,c3);
    }
}
