package com.example.demo.service;

import com.example.demo.entity.Exam;
import com.example.demo.entity.ExamAnswer;
import com.example.demo.entity.Question;

import java.util.List;
import java.util.Map;

public interface ExamService {
    Exam getExamByNoteId(Long noteId);
    List<Exam> getExamsByNoteId(Long noteId);
    Exam getExamById(Long id);
    Exam createExamFromAI(Long noteId, Long userId, String noteContent, int questionCount, String difficulty, List<Map<String, Object>> aiQuestions, String examName);
    List<Question> getExamQuestions(Long examId);
    Long saveUserAnswers(Long examId, Long userId, Map<Long, String> answers, Long attemptId);
    List<ExamAnswer> getExamAnswers(Long examId, Long userId, Long attemptId);
    List<Map<String, Object>> getExamAttempts(Long examId, Long userId);
    Map<String, Object> submitExam(Long examId, Long userId, Map<Long, String> answers, Long attemptId);
    void deleteExam(Long examId, Long userId);
}

