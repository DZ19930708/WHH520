package com.leyou.item.service;

import com.leyou.item.mapper.SpecGourpMapper;
import com.leyou.item.mapper.SpecParamMapper;
import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpecificationService {

    @Autowired
    private SpecGourpMapper specGourpMapper;

    @Autowired
    private SpecParamMapper specParamMapper;


    public List<SpecGroup> querySpecGroups(Long cid) {
        SpecGroup specGroup =new SpecGroup();
        specGroup.setCid(cid);
        return specGourpMapper.select(specGroup);
    }

    public List<SpecParam> querySpecParam(Long gid, Long cid, Boolean searching, Boolean generic) {

        SpecParam specParam=new SpecParam();
        specParam.setCid(cid);
        specParam.setGroupId(gid);
        specParam.setSearching(searching);
        specParam.setGeneric(generic);
        return specParamMapper.select(specParam);

    }

    public void saveSpecGroup(SpecGroup specGroup) {

        specGourpMapper.insert(specGroup);
    }

    public void editSpecGroup(SpecGroup specGroup) {


        specGourpMapper.updateByPrimaryKey(specGroup);
    }

    public void deleteSpecGroup(Long id) {

        specGourpMapper.deleteByPrimaryKey(id);
    }

    public void saveSpecParam(SpecParam specParam) {
        specParamMapper.insert(specParam);
    }

    public void updateSpecParam(SpecParam specParam) {
        specParamMapper.updateByPrimaryKey(specParam);

    }

    public void deleteSpecParam(Long group_id) {
        specParamMapper.deleteByPrimaryKey(group_id);
    }

    public List<SpecGroup> querySpecsByCid(Long cid) {
        List<SpecGroup> groups = querySpecGroups(cid);
        groups.forEach(group->{
            group.setParams(querySpecParam(group.getId(),null,null,null));
        });
        return groups;
    }
}
