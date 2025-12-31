package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.Group;
import com.example.demo.entity.Note;
import com.example.demo.entity.Subject;
import com.example.demo.mapper.GroupMapper;
import com.example.demo.mapper.GroupMemberMapper;
import com.example.demo.mapper.NoteMapper;
import com.example.demo.mapper.SubjectMapper;

@Service
public class SubjectServiceImpl implements SubjectService {
    
    @Autowired
    private SubjectMapper subjectMapper;
    
    @Autowired
    private NoteMapper noteMapper;
    
    @Autowired
    private GroupMemberMapper groupMemberMapper;
    
    @Autowired
    private GroupMapper groupMapper;

    @Override
    public List<Subject> getAllPublicSubjects() {
        List<Subject> subjects = subjectMapper.findAllPublic();
        for (Subject subject : subjects) {
            List<Note> notes = noteMapper.findBySubjectId(subject.getId());
            subject.setNotes(notes);
        }
        return subjects;
    }

    @Override
    public List<Subject> searchPublicSubjects(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }
        List<Subject> subjects = subjectMapper.searchPublic(keyword.trim());
        for (Subject subject : subjects) {
            List<Note> notes = noteMapper.findBySubjectId(subject.getId());
            subject.setNotes(notes);
        }
        return subjects;
    }

    @Override
    public List<Subject> getAllSubjects(Long userId) {
        List<Subject> subjects = subjectMapper.findAllByUserId(userId);
        
        List<Long> groupIds = groupMemberMapper.findGroupIdsByUserId(userId);
        if (groupIds != null && !groupIds.isEmpty()) {
            Set<Long> creatorIds = new HashSet<>();
            for (Long groupId : groupIds) {
                Group group = groupMapper.findById(groupId);
                if (group != null && group.getCreatorId() != null && !group.getCreatorId().equals(userId)) {
                    creatorIds.add(group.getCreatorId());
                }
            }
            
            if (!creatorIds.isEmpty()) {
                List<Long> creatorIdList = new ArrayList<>(creatorIds);
                List<Subject> groupSubjects = subjectMapper.findByGroupCreatorIds(creatorIdList, "Group: ");
                
                Set<Long> existingSubjectIds = subjects.stream()
                    .map(Subject::getId)
                    .collect(Collectors.toSet());
                
                for (Subject groupSubject : groupSubjects) {
                    if (!existingSubjectIds.contains(groupSubject.getId())) {
                        subjects.add(groupSubject);
                    }
                }
            }
        }
        
        for (Subject subject : subjects) {
            List<Note> notes = noteMapper.findBySubjectId(subject.getId());
            subject.setNotes(notes);
        }
        return subjects;
    }

    @Override
    public Map<String, List<Subject>> getAllSubjectsSeparated(Long userId) {
        Map<String, List<Subject>> result = new HashMap<>();
        
        List<Subject> personalSubjects = subjectMapper.findAllByUserId(userId);
        personalSubjects = personalSubjects.stream()
            .filter(s -> s.getName() == null || !s.getName().startsWith("Group: "))
            .collect(Collectors.toList());
        
        List<Subject> groupSubjects = new ArrayList<>();
        List<Long> groupIds = groupMemberMapper.findGroupIdsByUserId(userId);
        if (groupIds != null && !groupIds.isEmpty()) {
            Set<Long> creatorIds = new HashSet<>();
            for (Long groupId : groupIds) {
                Group group = groupMapper.findById(groupId);
                if (group != null && group.getCreatorId() != null) {
                    creatorIds.add(group.getCreatorId());
                }
            }
            
            if (!creatorIds.isEmpty()) {
                List<Long> creatorIdList = new ArrayList<>(creatorIds);
                groupSubjects = subjectMapper.findByGroupCreatorIds(creatorIdList, "Group: ");
            }
        }
        
        for (Subject subject : personalSubjects) {
            List<Note> notes = noteMapper.findBySubjectId(subject.getId());
            subject.setNotes(notes);
        }
        for (Subject subject : groupSubjects) {
            List<Note> notes = noteMapper.findBySubjectId(subject.getId());
            subject.setNotes(notes);
        }
        
        result.put("personal", personalSubjects);
        result.put("group", groupSubjects);
        return result;
    }

    @Override
    public Subject getSubjectById(Long id) {
        Subject subject = subjectMapper.findById(id);
        if (subject != null) {
            List<Note> notes = noteMapper.findBySubjectId(id);
            subject.setNotes(notes);
        }
        return subject;
    }

    @Override
    public Subject createSubject(Subject subject) {
        subject.setCreateTime(LocalDateTime.now());
        subject.setUpdateTime(LocalDateTime.now());
        if (subject.getVisibility() == null || subject.getVisibility().isEmpty()) {
            subject.setVisibility("private");
        }
        subjectMapper.insert(subject);
        return subjectMapper.findById(subject.getId());
    }

    @Override
    public Subject updateSubject(Long id, Subject subject) {
        Subject existingSubject = subjectMapper.findById(id);
        if (existingSubject == null) {
            throw new RuntimeException("科目不存在");
        }
        
        if (subject.getName() != null) {
            existingSubject.setName(subject.getName());
        }
        if (subject.getDescription() != null) {
            existingSubject.setDescription(subject.getDescription());
        }
        if (subject.getVisibility() != null) {
            existingSubject.setVisibility(subject.getVisibility());
        }
        existingSubject.setUpdateTime(LocalDateTime.now());
        
        subjectMapper.update(existingSubject);
        return getSubjectById(id);
    }

    @Override
    @Transactional
    public void deleteSubject(Long id) {
        noteMapper.deleteBySubjectId(id);
        subjectMapper.delete(id);
    }

    @Override
    @Transactional
    public Subject cloneSubject(Long subjectId, Long targetUserId) {
        Subject source = getSubjectById(subjectId);
        if (source == null) {
            throw new RuntimeException("要克隆的科目不存在");
        }
        if (!"public".equalsIgnoreCase(source.getVisibility())) {
            throw new RuntimeException("只能克隆公开的科目");
        }

        Subject cloned = new Subject();
        cloned.setName(source.getName());
        cloned.setDescription(source.getDescription());
        cloned.setUserId(targetUserId);
        cloned.setVisibility("clone");
        cloned.setCreateTime(LocalDateTime.now());
        cloned.setUpdateTime(LocalDateTime.now());
        subjectMapper.insert(cloned);

        List<Note> sourceNotes = noteMapper.findBySubjectId(source.getId());
        if (sourceNotes != null && !sourceNotes.isEmpty()) {
            for (Note note : sourceNotes) {
                Note clonedNote = new Note();
                clonedNote.setTitle(note.getTitle());
                clonedNote.setContent(note.getContent());
                clonedNote.setSubjectId(cloned.getId());
                clonedNote.setUserId(targetUserId);
                clonedNote.setCreateTime(LocalDateTime.now());
                clonedNote.setUpdateTime(LocalDateTime.now());
                noteMapper.insert(clonedNote);
            }
        }

        return getSubjectById(cloned.getId());
    }
}

