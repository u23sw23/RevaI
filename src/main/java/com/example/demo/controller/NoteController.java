package com.example.demo.controller;

import java.util.HashMap;
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
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.entity.Note;
import com.example.demo.service.NoteService;

@RestController
@RequestMapping("/api/notes")
public class NoteController {
    
    @Autowired
    private NoteService noteService;

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getNoteById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Note note = noteService.getNoteById(id);
            response.put("success", true);
            response.put("data", note);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createNote(@RequestBody Note note) {
        Map<String, Object> response = new HashMap<>();
        try {
            Note created = noteService.createNote(note);
            response.put("success", true);
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAndCreateNote(
            @RequestParam Long subjectId,
            @RequestParam String noteName,
            @RequestParam Long userId,
            @RequestParam(required = false) MultipartFile[] files) {
        Map<String, Object> response = new HashMap<>();
        try {
            // TODO: 处理文件上传和AI生成笔记的逻辑
            // 这里暂时返回模拟数据，实际应该处理文件并调用AI服务
            Note note = new Note();
            note.setTitle(noteName);
            note.setContent("AI生成的笔记内容...（待实现文件处理和AI生成功能）");
            note.setSubjectId(subjectId);
            note.setUserId(userId);
            
            Note created = noteService.createNote(note);
            response.put("success", true);
            response.put("data", created);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateNote(
            @PathVariable Long id,
            @RequestBody Note note) {
        Map<String, Object> response = new HashMap<>();
        try {
            Note updated = noteService.updateNote(id, note);
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
    public ResponseEntity<Map<String, Object>> deleteNote(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            noteService.deleteNote(id);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}

