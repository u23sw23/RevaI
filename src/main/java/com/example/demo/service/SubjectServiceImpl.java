package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Note;
import com.example.demo.entity.Subject;
import com.example.demo.mapper.NoteMapper;
import com.example.demo.mapper.SubjectMapper;

@Service
public class SubjectServiceImpl implements SubjectService {
    
    @Autowired
    private SubjectMapper subjectMapper;
    
    @Autowired
    private NoteMapper noteMapper;

    @Override
    public List<Subject> getAllPublicSubjects() {
        List<Subject> subjects = subjectMapper.findAllPublic();
        // 为每个科目加载笔记列表
        for (Subject subject : subjects) {
            List<Note> notes = noteMapper.findBySubjectId(subject.getId());
            subject.setNotes(notes);
        }
        return subjects;
    }

    @Override
    public List<Subject> getAllSubjects(Long userId) {
        List<Subject> subjects = subjectMapper.findAllByUserId(userId);
        // 为每个科目加载笔记列表
        for (Subject subject : subjects) {
            List<Note> notes = noteMapper.findBySubjectId(subject.getId());
            subject.setNotes(notes);
        }
        return subjects;
    }

    @Override
    public Subject getSubjectById(Long id) {
        Subject subject = subjectMapper.findById(id);
        if (subject != null) {
            List<Note> notes = noteMapper.findBySubjectId(id);
            subject.setNotes(notes);
        }
        return subject;
    }

    @Override
    public Subject createSubject(Subject subject) {
        subject.setCreateTime(LocalDateTime.now());
        subject.setUpdateTime(LocalDateTime.now());
        // 如果没有设置可见性，默认为 private
        if (subject.getVisibility() == null || subject.getVisibility().isEmpty()) {
            subject.setVisibility("private");
        }
        subjectMapper.insert(subject);
        return subjectMapper.findById(subject.getId());
    }

    @Override
    public Subject updateSubject(Long id, Subject subject) {
        Subject existingSubject = subjectMapper.findById(id);
        if (existingSubject == null) {
            throw new RuntimeException("科目不存在");
        }
        
        if (subject.getName() != null) {
            existingSubject.setName(subject.getName());
        }
        if (subject.getDescription() != null) {
            existingSubject.setDescription(subject.getDescription());
        }
        if (subject.getVisibility() != null) {
            existingSubject.setVisibility(subject.getVisibility());
        }
        existingSubject.setUpdateTime(LocalDateTime.now());
        
        subjectMapper.update(existingSubject);
        return getSubjectById(id);
    }

    @Override
    @Transactional
    public void deleteSubject(Long id) {
        // 先删除该科目下的所有笔记
        noteMapper.deleteBySubjectId(id);
        // 再删除科目
        subjectMapper.delete(id);
    }
}

