package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

import com.example.demo.entity.Group;
import com.example.demo.entity.Subject;
import com.example.demo.mapper.NoteMapper;
import com.example.demo.mapper.SubjectMapper;
import com.example.demo.service.GroupService;

@RestController
@RequestMapping("/api/groups")
public class GroupController {
    @Autowired
    private GroupService groupService;
    
    @Autowired
    private SubjectMapper subjectMapper;
    
    @Autowired
    private NoteMapper noteMapper;
    @GetMapping
    public ResponseEntity<Map<String, Object>> getGroups(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String name) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (name != null) {
                Group group = groupService.getGroupByName(name);
                response.put("success", true);
                response.put("group", group);
                return ResponseEntity.ok(response);
            } else if (userId != null) {
                List<Group> groups = groupService.getUserGroups(userId);
                response.put("success", true);
                response.put("data", groups);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "需要提供userId或name参数");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getGroupById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Group group = groupService.getGroupById(id);
            response.put("success", true);
            response.put("data", group);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createGroup(
            @RequestBody Map<String, Object> request,
            @RequestParam(required = false) Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            Group group = new Group();
            group.setName((String) request.get("name"));

            Long creatorId = userId;
            if (creatorId == null && request.get("creatorId") != null) {
                creatorId = Long.valueOf(request.get("creatorId").toString());
            }
            if (creatorId == null) {
                throw new RuntimeException("需要提供创建者ID");
            }
            group.setCreatorId(creatorId);
            
            @SuppressWarnings("unchecked")
            List<Object> memberIdsObj = (List<Object>) request.get("memberIds");
            List<Long> memberIds = null;
            if (memberIdsObj != null && !memberIdsObj.isEmpty()) {
                memberIds = memberIdsObj.stream()
                    .map(obj -> Long.valueOf(obj.toString()))
                    .toList();
            }
            
            Group created = groupService.createGroup(group, memberIds);
            response.put("success", true);
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> joinGroup(
            @RequestBody Map<String, Object> request,  
            @RequestParam(required = false) Long userId) { 
        Map<String, Object> response = new HashMap<>();
        try {
            Long groupId = Long.valueOf(request.get("groupId").toString());

            Long currentUserId = userId;
            if (currentUserId == null && request.get("userId") != null) {
                currentUserId = Long.valueOf(request.get("userId").toString());
            }
            if (currentUserId == null) {
                throw new RuntimeException("需要提供用户ID");
            }
            
            groupService.joinGroup(groupId, currentUserId);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}/subject")
    public ResponseEntity<Map<String, Object>> getGroupSharedSubject(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Group group = groupService.getGroupById(id);
            if (group == null) {
                response.put("success", false);
                response.put("message", "Group not found");
                return ResponseEntity.badRequest().body(response);
            }

            String subjectName = "Group: " + group.getName();
            Subject subject = subjectMapper.findByNameAndUserId(subjectName, group.getCreatorId());
            
            if (subject != null) {
                
                subject.setNotes(noteMapper.findBySubjectId(subject.getId()));
                response.put("success", true);
                response.put("data", subject);
            } else {
                response.put("success", true);
                response.put("data", null);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{id}/subject")
    public ResponseEntity<Map<String, Object>> createGroupSharedSubject(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Group group = groupService.getGroupById(id);
            if (group == null) {
                response.put("success", false);
                response.put("message", "Group not found");
                return ResponseEntity.badRequest().body(response);
            }

            String subjectName = "Group: " + group.getName();
            Subject existingSubject = subjectMapper.findByNameAndUserId(subjectName, group.getCreatorId());
            if (existingSubject != null) {
                
                existingSubject.setNotes(noteMapper.findBySubjectId(existingSubject.getId()));
                response.put("success", true);
                response.put("data", existingSubject);
                return ResponseEntity.ok(response);
            }

            Subject subject = new Subject();
            subject.setName((String) request.getOrDefault("name", subjectName));
            subject.setDescription((String) request.getOrDefault("description", "Shared workspace for group \"" + group.getName() + "\""));
            subject.setUserId(group.getCreatorId());
            subject.setVisibility("private");
            subject.setCreateTime(LocalDateTime.now());
            subject.setUpdateTime(LocalDateTime.now());
            
            subjectMapper.insert(subject);

            Subject createdSubject = subjectMapper.findById(subject.getId());
            if (createdSubject != null) {
                createdSubject.setNotes(noteMapper.findBySubjectId(createdSubject.getId()));
            }
            
            response.put("success", true);
            response.put("data", createdSubject);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

