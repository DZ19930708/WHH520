package com.leyou.item.mapper;

import com.leyou.item.pojo.Brand;
import org.apache.ibatis.annotations.*;
import tk.mybatis.mapper.additional.idlist.SelectByIdListMapper;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BrandMapper extends Mapper<Brand>,SelectByIdListMapper<Brand,Long> {

    @Insert("INSERT INTO tb_category_brand (category_id, brand_id) VALUES (#{cid},#{bid})")
    void insertCategoryBrand(@Param("cid") Long cid, @Param("bid") Long bid);

    @Delete("delete from tb_category_brand where brand_id=#{bid}  ")
    void deleteCategoryBrandByCid(Long bid);

    @Insert("INSERT INTO tb_category_brand VALUES (#{cid}, #{bid})")
    void editCategoeyBrand(@Param("cid") Long cid, @Param("bid") Long bid);

   @Delete("delete from  tb_category_brand where brand_id=#{bid} ")
    void deleteCategoryBrand(@Param("bid") Long bid);

   @Select("select b.* from tb_category_brand cb left join tb_brand b on b.id = cb.brand_id where category_id =#{cid}")
    List<Brand> queryBrandByCategory(@Param("cid") Long cid);


}
