package com.example.demo.mapper;

import com.example.demo.entity.Exam;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ExamMapper {
    List<Exam> findByNoteId(Long noteId);
    Exam findById(Long id);
    int insert(Exam exam);
    int update(Exam exam);
    int deleteById(Long id);
}

