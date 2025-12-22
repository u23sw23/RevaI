package com.example.demo.mapper;

import com.example.demo.entity.GroupMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupMemberMapper {
    List<Long> findUserIdsByGroupId(Long groupId);
    GroupMember findByGroupIdAndUserId(@Param("groupId") Long groupId, @Param("userId") Long userId);
    int insert(GroupMember groupMember);
    int delete(@Param("groupId") Long groupId, @Param("userId") Long userId);
    int deleteByGroupId(Long groupId);
}

