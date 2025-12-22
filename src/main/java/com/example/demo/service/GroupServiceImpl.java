package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Group;
import com.example.demo.entity.GroupMember;
import com.example.demo.mapper.GroupMapper;
import com.example.demo.mapper.GroupMemberMapper;

@Service
public class GroupServiceImpl implements GroupService {
    
    @Autowired
    private GroupMapper groupMapper;
    
    @Autowired
    private GroupMemberMapper groupMemberMapper;

    @Override
    public List<Group> getUserGroups(Long userId) {
        List<Group> groups = groupMapper.findByUserId(userId);
        // 为每个群组加载成员列表
        for (Group group : groups) {
            List<Long> memberIds = groupMemberMapper.findUserIdsByGroupId(group.getId());
            group.setMemberIds(memberIds);
        }
        return groups;
    }

    @Override
    public Group getGroupById(Long id) {
        Group group = groupMapper.findById(id);
        if (group != null) {
            List<Long> memberIds = groupMemberMapper.findUserIdsByGroupId(id);
            group.setMemberIds(memberIds);
        }
        return group;
    }

    @Override
    public Group getGroupByName(String name) {
        return groupMapper.findByName(name);
    }

    @Override
    @Transactional
    public Group createGroup(Group group, List<Long> memberIds) {
        group.setCreateTime(LocalDateTime.now());
        group.setUpdateTime(LocalDateTime.now());
        groupMapper.insert(group);
        
        // 创建者自动加入群组
        GroupMember creator = new GroupMember();
        creator.setGroupId(group.getId());
        creator.setUserId(group.getCreatorId());
        creator.setJoinTime(LocalDateTime.now());
        groupMemberMapper.insert(creator);
        
        // 添加其他成员（排除创建者，避免重复）
        if (memberIds != null && !memberIds.isEmpty()) {
            for (Long memberId : memberIds) {
                // 跳过创建者，因为已经添加过了
                if (memberId.equals(group.getCreatorId())) {
                    continue;
                }
                GroupMember member = new GroupMember();
                member.setGroupId(group.getId());
                member.setUserId(memberId);
                member.setJoinTime(LocalDateTime.now());
                groupMemberMapper.insert(member);
            }
        }
        
        return getGroupById(group.getId());
    }

    @Override
    @Transactional
    public void joinGroup(Long groupId, Long userId) {
        // 检查是否已经是成员
        GroupMember existing = groupMemberMapper.findByGroupIdAndUserId(groupId, userId);
        if (existing != null) {
            throw new RuntimeException("已经是群组成员");
        }
        
        GroupMember member = new GroupMember();
        member.setGroupId(groupId);
        member.setUserId(userId);
        member.setJoinTime(LocalDateTime.now());
        groupMemberMapper.insert(member);
    }
}

