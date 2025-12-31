package com.example.demo.mapper;

import com.example.demo.entity.ExamSM2Stats;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ExamSM2StatsMapper {
    ExamSM2Stats findByExamIdAndUserId(Long examId, Long userId);
    List<ExamSM2Stats> findByUserId(Long userId);
    int insert(ExamSM2Stats stats);
    int update(ExamSM2Stats stats);
}

