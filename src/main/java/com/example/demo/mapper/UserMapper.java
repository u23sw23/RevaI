package com.example.demo.mapper;

import com.example.demo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {
    User findById(Long id);
    User findByUsername(String username);
    List<User> searchByKeyword(@Param("keyword") String keyword);
    int insert(User user);
    int update(User user);
    int countSubjectsByUserId(Long userId);
    int countGroupsByUserId(Long userId);
}

