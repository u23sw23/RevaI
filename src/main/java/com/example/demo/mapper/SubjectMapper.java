package com.example.demo.mapper;

import com.example.demo.entity.Subject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubjectMapper {
    List<Subject> findAllByUserId(Long userId);
    List<Subject> findAllPublic();
    Subject findById(Long id);
    int insert(Subject subject);
    int update(Subject subject);
    int delete(Long id);
}

