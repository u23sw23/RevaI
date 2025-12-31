package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    
    @Autowired
    private UserMapper userMapper;

    @Override
    public User register(String username, String password) {
        try {
            
            User existingUser = userMapper.findByUsername(username);
            if (existingUser != null) {
                throw new RuntimeException("用户名已存在");
            }

            User user = new User();
            user.setUsername(username);
            
            user.setPassword(DigestUtils.md5DigestAsHex(password.getBytes()));  
            user.setCreateTime(LocalDateTime.now());
            user.setUpdateTime(LocalDateTime.now());
            
            userMapper.insert(user);

            User savedUser = userMapper.findByUsername(username);
            if (savedUser == null) {
                throw new RuntimeException("用户创建失败，请重试");
            }
            return savedUser;
        } catch (RuntimeException e) {
            throw e; 
        } catch (Exception e) {
            throw new RuntimeException("注册失败：" + e.getMessage(), e);
        }
    }

    @Override
    public User login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!user.getPassword().equals(encryptedPassword)) {
            throw new RuntimeException("用户名或密码错误");
        }

        user.setPassword(null);
        return user;
    }

    @Override
    public User getProfile(Long userId) {
        User user = userMapper.findById(userId);
        if (user != null) {
            user.setPassword(null); 
        }
        return user;
    }

    @Override
    public User updateProfile(Long userId, User user) {
        User existingUser = userMapper.findById(userId);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            
            User userWithSameName = userMapper.findByUsername(user.getUsername());
            if (userWithSameName != null && !userWithSameName.getId().equals(userId)) {
                throw new RuntimeException("用户名已被使用");
            }
            existingUser.setUsername(user.getUsername().trim());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail().trim());
        }
        if (user.getBio() != null) {
            existingUser.setBio(user.getBio().trim());
        }
        if (user.getAvatar() != null) {
            existingUser.setAvatar(user.getAvatar().trim());
        }
        existingUser.setUpdateTime(LocalDateTime.now());
        
        userMapper.update(existingUser);
        existingUser.setPassword(null);
        return existingUser;
    }

    @Override
    public List<User> searchUsers(String keyword) {
        return userMapper.searchByKeyword(keyword);
    }

    @Override
    public int countSubjects(Long userId) {
        return userMapper.countSubjectsByUserId(userId);
    }

    @Override
    public int countGroups(Long userId) {
        return userMapper.countGroupsByUserId(userId);
    }
}

