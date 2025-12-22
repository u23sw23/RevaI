package com.example.demo.mapper;

import com.example.demo.entity.Note;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoteMapper {
    List<Note> findBySubjectId(Long subjectId);
    Note findById(Long id);
    int insert(Note note);
    int update(Note note);
    int delete(Long id);
    int deleteBySubjectId(Long subjectId);
}

