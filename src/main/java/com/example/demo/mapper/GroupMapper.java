package com.example.demo.mapper;

import com.example.demo.entity.Group;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMapper {
    List<Group> findByUserId(Long userId);
    Group findById(Long id);
    Group findByName(String name);
    int insert(Group group);
    int update(Group group);
    int delete(Long id);
}

