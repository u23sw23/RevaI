package com.example.demo.service;

import com.example.demo.entity.Group;

import java.util.List;

public interface GroupService {
    List<Group> getUserGroups(Long userId);
    Group getGroupById(Long id);
    Group getGroupByName(String name);
    Group createGroup(Group group, List<Long> memberIds);
    void joinGroup(Long groupId, Long userId);
}

