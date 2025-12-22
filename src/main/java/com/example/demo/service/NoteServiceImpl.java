package com.example.demo.service;

import com.example.demo.entity.Note;
import com.example.demo.mapper.NoteMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NoteServiceImpl implements NoteService {
    
    @Autowired
    private NoteMapper noteMapper;

    @Override
    public List<Note> getNotesBySubjectId(Long subjectId) {
        return noteMapper.findBySubjectId(subjectId);
    }

    @Override
    public Note getNoteById(Long id) {
        return noteMapper.findById(id);
    }

    @Override
    public Note createNote(Note note) {
        note.setCreateTime(LocalDateTime.now());
        note.setUpdateTime(LocalDateTime.now());
        noteMapper.insert(note);
        return noteMapper.findById(note.getId());
    }

    @Override
    public Note updateNote(Long id, Note note) {
        Note existingNote = noteMapper.findById(id);
        if (existingNote == null) {
            throw new RuntimeException("笔记不存在");
        }
        
        if (note.getTitle() != null) {
            existingNote.setTitle(note.getTitle());
        }
        if (note.getContent() != null) {
            existingNote.setContent(note.getContent());
        }
        existingNote.setUpdateTime(LocalDateTime.now());
        
        noteMapper.update(existingNote);
        return noteMapper.findById(id);
    }

    @Override
    public void deleteNote(Long id) {
        noteMapper.delete(id);
    }
}

