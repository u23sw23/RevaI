package com.example.demo.mapper;

import com.example.demo.entity.ExamAttempt;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Map;

@Mapper
public interface ExamAttemptMapper {
    List<ExamAttempt> findByExamIdAndUserId(Long examId, Long userId);
    ExamAttempt findById(Long id);
    int insert(ExamAttempt examAttempt);
    int update(ExamAttempt examAttempt);
    int deleteByExamIdAndUserId(Long examId, Long userId);
    List<Map<String, Object>> getReviewStatisticsByUserId(Long userId);
}

