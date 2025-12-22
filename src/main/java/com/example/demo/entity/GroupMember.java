package com.example.demo.entity;
import java.time.LocalDateTime;

public class GroupMember {
    private Long id;
    private Long groupId;
    private Long userId;
    private LocalDateTime joinTime;
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getGroupId() {
        return groupId;
    }
    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }
    public Long getUserId() {
        return userId;
    }
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    public LocalDateTime getJoinTime() {
        return joinTime;
    }
    public void setJoinTime(LocalDateTime joinTime) {
        this.joinTime = joinTime;
    }
}

