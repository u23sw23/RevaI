package com.example.demo.mapper;

import com.example.demo.entity.Question;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface QuestionMapper {
    List<Question> findByExamId(Long examId);
    Question findById(Long id);
    int insert(Question question);
    int update(Question question);
    int deleteByExamId(Long examId);
}

