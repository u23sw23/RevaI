package com.example.demo.mapper;

import com.example.demo.entity.ExamAnswer;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ExamAnswerMapper {
    List<ExamAnswer> findByExamIdAndUserId(Long examId, Long userId);
    ExamAnswer findByExamIdAndUserIdAndQuestionId(Long examId, Long userId, Long questionId);
    int insert(ExamAnswer examAnswer);
    int update(ExamAnswer examAnswer);
    int deleteByExamIdAndUserId(Long examId, Long userId);
}

