package com.example.demo.service;

import java.util.List;
import java.util.Map;

import com.example.demo.entity.Subject;

public interface SubjectService {
    List<Subject> getAllSubjects(Long userId);
    Map<String, List<Subject>> getAllSubjectsSeparated(Long userId);
    List<Subject> getAllPublicSubjects();
    List<Subject> searchPublicSubjects(String keyword);
    Subject getSubjectById(Long id);
    Subject createSubject(Subject subject);
    Subject updateSubject(Long id, Subject subject);
    void deleteSubject(Long id);

    Subject cloneSubject(Long subjectId, Long targetUserId);
}

