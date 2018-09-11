package com.leyou.item.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.pojo.Brand;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@Service
public class BrandService {

    @Autowired
    private BrandMapper brandMapper;

    public PageResult<Brand> queryBrandByPageAndSort(
            Integer page, Integer rows, String sortBy, Boolean desc, String key) {
        // 开始分页
        PageHelper.startPage(page, rows);
        // 过滤
        Example example = new Example(Brand.class);
        if (StringUtils.isNotBlank(key)) {
            example.createCriteria().andLike("name", "%" + key + "%")
                    .orEqualTo("letter", key.toUpperCase());
        }
        if (StringUtils.isNotBlank(sortBy)) {
            // 排序
            String orderByClause = sortBy + (desc ? " DESC" : " ASC");
            example.setOrderByClause(orderByClause);
        }
        // 查询
        Page<Brand> pageInfo = (Page<Brand>) brandMapper.selectByExample(example);
        // 返回结果
        return new PageResult<>(pageInfo.getTotal(), pageInfo);
    }
    @Transactional
    public void saveBrand(Brand brand, List<Long> cids) {
        //新增品牌信息到brand表
        brandMapper.insertSelective(brand);
        //新增品牌和分类中间表
        cids.forEach(cid->{
            brandMapper.insertCategoryBrand(cid,brand.getId());

        });

    }

    public void editBrand(Brand brand, List<Long> cids) {
     brandMapper.updateByPrimaryKey(brand);
     brandMapper.deleteCategoryBrandByCid(brand.getId());
     cids.forEach(cid->{
         brandMapper.editCategoeyBrand(cid,brand.getId());
     });
    }

    public void deleteBrand(Long bid) {

        brandMapper.deleteByPrimaryKey(bid);
        brandMapper.deleteCategoryBrand(bid);
    }

    public List<Brand> queryBrandByCategory(Long cid) {

       return  brandMapper.queryBrandByCategory(cid);
    }

    public List<Brand> queryBrandByIds(List<Long> ids) {
        return brandMapper.selectByIdList(ids);
    }

    public String queryBrandNameById(Long id) {
        Brand brand = brandMapper.selectByPrimaryKey(id);
        return  brand.getName();
    }
}
