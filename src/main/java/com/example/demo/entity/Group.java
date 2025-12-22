package com.example.demo.entity;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 群组实体类
 */
public class Group {
    private Long id;
    private String name;
    private Long creatorId; // 创建者ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 成员列表（不存储在数据库中，通过关联表查询）
    private List<Long> memberIds;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public List<Long> getMemberIds() {
        return memberIds;
    }

    public void setMemberIds(List<Long> memberIds) {
        this.memberIds = memberIds;
    }
}

