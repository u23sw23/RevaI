package com.example.demo.service;

import com.example.demo.entity.User;

import java.util.List;

public interface UserService {
    User register(String username, String password);
    User login(String username, String password);
    User getProfile(Long userId);
    User updateProfile(Long userId, User user);
    List<User> searchUsers(String keyword);
    int countSubjects(Long userId);
    int countGroups(Long userId);
}

