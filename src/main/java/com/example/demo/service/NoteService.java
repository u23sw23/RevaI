package com.example.demo.service;

import com.example.demo.entity.Note;

import java.util.List;

public interface NoteService {
    List<Note> getNotesBySubjectId(Long subjectId);
    Note getNoteById(Long id);
    Note createNote(Note note);
    Note updateNote(Long id, Note note);
    void deleteNote(Long id);
}

