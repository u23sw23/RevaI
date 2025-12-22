package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.entity.Subject;
import com.example.demo.service.SubjectService;

@RestController
@RequestMapping("/api/subjects")
public class SubjectController {
    
    @Autowired
    private SubjectService subjectService;

    @GetMapping  
    public ResponseEntity<Map<String, Object>> getAllSubjects(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String visibility) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Subject> subjects;  //这行代码声明了一个List<Subject>类型的变量subjects，用于存储科目列表。
            if ("public".equals(visibility)) { //这行代码检查visibility参数是否等于"public"。
                // 获取所有公开的科目
                subjects = subjectService.getAllPublicSubjects();
            } else if (userId != null) {
                // 获取指定用户的科目
                subjects = subjectService.getAllSubjects(userId);
            } else {
                response.put("success", false);
                response.put("message", "需要提供userId或visibility=public参数");
                return ResponseEntity.badRequest().body(response);
            }
            response.put("success", true);
            response.put("data", subjects);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace(); // 打印堆栈跟踪以便调试
            response.put("success", false);
            response.put("message", "获取科目列表失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSubjectById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Subject subject = subjectService.getSubjectById(id);
            response.put("success", true);
            response.put("data", subject);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createSubject(@RequestBody Subject subject) {
        Map<String, Object> response = new HashMap<>();
        try {
            Subject created = subjectService.createSubject(subject);
            response.put("success", true);
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateSubject(
            @PathVariable Long id,
            @RequestBody Subject subject) {
        Map<String, Object> response = new HashMap<>();
        try {
            Subject updated = subjectService.updateSubject(id, subject);
            response.put("success", true);
            response.put("data", updated);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteSubject(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            subjectService.deleteSubject(id);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

