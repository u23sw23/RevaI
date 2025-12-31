package com.example.demo.mapper;

import com.example.demo.entity.Subject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SubjectMapper {
    List<Subject> findAllByUserId(Long userId);
    List<Subject> findAllPublic();
    List<Subject> searchPublic(@Param("keyword") String keyword);
    Subject findById(Long id);
    Subject findByNameAndUserId(@Param("name") String name, @Param("userId") Long userId);
    List<Subject> findByGroupCreatorIds(@Param("creatorIds") List<Long> creatorIds, @Param("namePrefix") String namePrefix);
    int insert(Subject subject);
    int update(Subject subject);
    int delete(Long id);
}

