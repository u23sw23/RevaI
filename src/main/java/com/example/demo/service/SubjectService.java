package com.example.demo.service;

import com.example.demo.entity.Subject;

import java.util.List;

public interface SubjectService {
    List<Subject> getAllSubjects(Long userId);
    List<Subject> getAllPublicSubjects();
    Subject getSubjectById(Long id);
    Subject createSubject(Subject subject);
    Subject updateSubject(Long id, Subject subject);
    void deleteSubject(Long id);
}

