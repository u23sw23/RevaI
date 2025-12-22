package com.example.demo.service;

import com.example.demo.entity.Exam;
import com.example.demo.entity.ExamAnswer;
import com.example.demo.entity.Note;
import com.example.demo.entity.Option;
import com.example.demo.entity.Question;
import com.example.demo.mapper.ExamAnswerMapper;
import com.example.demo.mapper.ExamMapper;
import com.example.demo.mapper.NoteMapper;
import com.example.demo.mapper.QuestionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class ExamServiceImpl implements ExamService {

    @Autowired
    private ExamMapper examMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private ExamAnswerMapper examAnswerMapper;

    @Autowired
    private NoteMapper noteMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exam getExamByNoteId(Long noteId) {
        List<Exam> exams = examMapper.findByNoteId(noteId);
        if (exams != null && !exams.isEmpty()) {
            Exam exam = exams.get(0);
            exam.setQuestions(getExamQuestions(exam.getId()));
            return exam;
        }
        return null;
    }

    @Override
    public List<Exam> getExamsByNoteId(Long noteId) {
        List<Exam> exams = examMapper.findByNoteId(noteId);
        // 为每个考试加载题目
        for (Exam exam : exams) {
            exam.setQuestions(getExamQuestions(exam.getId()));
        }
        return exams;
    }

    @Override
    public Exam getExamById(Long id) {
        Exam exam = examMapper.findById(id);
        if (exam != null) {
            exam.setQuestions(getExamQuestions(id));
        }
        return exam;
    }

    @Override
    @Transactional
    public Exam createExamFromAI(Long noteId, Long userId, String noteContent, int questionCount, String difficulty, List<Map<String, Object>> aiQuestions, String examName) {
        // aiQuestions 由Controller层传入，这里只负责保存到数据库
        if (aiQuestions == null || aiQuestions.isEmpty()) {
            throw new RuntimeException("AI未返回任何题目");
        }

        // 获取笔记信息
        Note note = noteMapper.findById(noteId);
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }

        // 创建Exam记录
        Exam exam = new Exam();
        if (examName != null && !examName.trim().isEmpty()) {
            exam.setTitle(examName);
        } else {
            exam.setTitle(note.getTitle() + " - 考试");
        }
        exam.setNoteId(noteId);
        exam.setUserId(userId);
        exam.setTotalQuestions(aiQuestions.size());
        exam.setCreateTime(LocalDateTime.now());
        exam.setUpdateTime(LocalDateTime.now());
        examMapper.insert(exam);

        // 保存题目
        List<Question> questions = new ArrayList<>();
        for (Map<String, Object> q : aiQuestions) {
            Question question = new Question();
            question.setExamId(exam.getId());
            question.setTitle((String) q.get("stem"));
            
            // 转换题目类型
            String type = (String) q.get("type");
            if ("single".equals(type)) {
                question.setType("single_choice");
            } else if ("true-false".equals(type)) {
                question.setType("true_false");
            } else if ("open".equals(type)) {
                question.setType("open_question");
            } else {
                question.setType(type);
            }
            
            question.setPoints(((Number) q.getOrDefault("points", 5)).intValue());
            question.setCorrectAnswer((String) q.get("answer"));
            question.setExplanation((String) q.getOrDefault("explanation", ""));
            
            // 处理选项（如果有）
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> optionsData = (List<Map<String, Object>>) q.get("options");
            if (optionsData != null && !optionsData.isEmpty()) {
                List<Option> options = new ArrayList<>();
                for (Map<String, Object> opt : optionsData) {
                    Option option = new Option(
                        (String) opt.get("value"),
                        (String) opt.get("label"),
                        (String) opt.get("text")
                    );
                    options.add(option);
                }
                question.setOptions(options);
            } else if ("true_false".equals(question.getType())) {
                // 判断题自动生成选项
                List<Option> options = Arrays.asList(
                    new Option("True", "A", "True"),
                    new Option("False", "B", "False")
                );
                question.setOptions(options);
            }
            
            // 将选项序列化为JSON并插入
            insertQuestionWithOptions(question);
            questions.add(question);
        }

        exam.setQuestions(questions);
        return exam;
    }

    @Override
    public List<Question> getExamQuestions(Long examId) {
        List<Question> questions = questionMapper.findByExamId(examId);
        // 从JSON字段解析选项
        for (Question q : questions) {
            parseOptionsFromJson(q);
        }
        return questions;
    }

    /**
     * 插入题目并处理选项的JSON序列化
     */
    private void insertQuestionWithOptions(Question question) {
        try {
            // 将选项序列化为JSON
            String optionsJson = null;
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                optionsJson = objectMapper.writeValueAsString(question.getOptions());
            }
            question.setOptionsJson(optionsJson);
            
            // 使用标准的insert方法
            questionMapper.insert(question);
        } catch (Exception e) {
            throw new RuntimeException("保存题目失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从JSON字段解析选项
     */
    private void parseOptionsFromJson(Question question) {
        if (question.getOptionsJson() != null && !question.getOptionsJson().trim().isEmpty()) {
            try {
                List<Map<String, Object>> optionsData = objectMapper.readValue(
                    question.getOptionsJson(), 
                    new TypeReference<List<Map<String, Object>>>() {}
                );
                List<Option> options = new ArrayList<>();
                for (Map<String, Object> opt : optionsData) {
                    Option option = new Option(
                        (String) opt.get("value"),
                        (String) opt.get("label"),
                        (String) opt.get("text")
                    );
                    options.add(option);
                }
                question.setOptions(options);
            } catch (Exception e) {
                // JSON解析失败，保持options为null
                System.err.println("解析选项JSON失败: " + e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public Long saveUserAnswers(Long examId, Long userId, Map<Long, String> answers, Long attemptId) {
        // 如果没有attemptId，使用当前时间作为attempt标识
        LocalDateTime submitTime = LocalDateTime.now();
        
        // 如果提供了attemptId，尝试从现有答案中获取submitTime（简化处理，实际应该从attempt表获取）
        // 这里为了简化，每次保存都使用当前时间，提交时才统一时间
        
        for (Map.Entry<Long, String> entry : answers.entrySet()) {
            ExamAnswer existing = examAnswerMapper.findByExamIdAndUserIdAndQuestionId(
                examId, userId, entry.getKey());
            
            ExamAnswer answer = new ExamAnswer();
            answer.setExamId(examId);
            answer.setUserId(userId);
            answer.setQuestionId(entry.getKey());
            answer.setAnswer(entry.getValue());
            answer.setSubmitTime(submitTime);
            
            if (existing != null) {
                answer.setId(existing.getId());
                examAnswerMapper.update(answer);
            } else {
                examAnswerMapper.insert(answer);
            }
        }
        
        // 返回attemptId（使用submitTime的hashCode作为临时ID，实际应该使用数据库生成的ID）
        // 这里简化处理，返回submitTime的毫秒数作为attemptId
        return submitTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
    }

    @Override
    public List<ExamAnswer> getExamAnswers(Long examId, Long userId, Long attemptId) {
        List<ExamAnswer> allAnswers = examAnswerMapper.findByExamIdAndUserId(examId, userId);
        
        if (attemptId == null) {
            // 如果没有指定attemptId，返回最新的答案
            if (allAnswers.isEmpty()) {
                return new ArrayList<>();
            }
            // 找到最新的submitTime
            LocalDateTime latestTime = allAnswers.stream()
                .map(ExamAnswer::getSubmitTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
            if (latestTime != null) {
                return allAnswers.stream()
                    .filter(a -> a.getSubmitTime().equals(latestTime))
                    .collect(java.util.stream.Collectors.toList());
            }
            return new ArrayList<>();
        } else {
            // 根据attemptId（实际上是submitTime的毫秒数）过滤
            LocalDateTime targetTime = LocalDateTime.ofEpochSecond(attemptId / 1000, 0, java.time.ZoneOffset.UTC);
            return allAnswers.stream()
                .filter(a -> a.getSubmitTime().equals(targetTime))
                .collect(java.util.stream.Collectors.toList());
        }
    }

    @Override
    public List<Map<String, Object>> getExamAttempts(Long examId, Long userId) {
        List<ExamAnswer> allAnswers = examAnswerMapper.findByExamIdAndUserId(examId, userId);
        
        // 按submitTime分组
        Map<LocalDateTime, List<ExamAnswer>> groupedByTime = allAnswers.stream()
            .collect(java.util.stream.Collectors.groupingBy(ExamAnswer::getSubmitTime));
        
        List<Map<String, Object>> attempts = new ArrayList<>();
        for (Map.Entry<LocalDateTime, List<ExamAnswer>> entry : groupedByTime.entrySet()) {
            LocalDateTime submitTime = entry.getKey();
            List<ExamAnswer> answers = entry.getValue();
            
            // 计算总分
            int totalScore = answers.stream()
                .mapToInt(a -> a.getScore() != null ? a.getScore() : 0)
                .sum();
            
            // 判断是否已提交（如果有分数，认为已提交）
            boolean hasSubmitted = totalScore > 0 || answers.stream().anyMatch(a -> a.getScore() != null);
            
            Map<String, Object> attempt = new HashMap<>();
            attempt.put("id", submitTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
            attempt.put("submitTime", submitTime);
            attempt.put("createdAt", submitTime);
            attempt.put("score", totalScore);
            attempt.put("hasSubmitted", hasSubmitted);
            attempt.put("answers", answers.stream().collect(java.util.stream.Collectors.toMap(
                a -> a.getQuestionId().toString(),
                ExamAnswer::getAnswer
            )));
            attempts.add(attempt);
        }
        
        // 按submitTime降序排序
        attempts.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("submitTime");
            LocalDateTime timeB = (LocalDateTime) b.get("submitTime");
            return timeB.compareTo(timeA);
        });
        
        return attempts;
    }

    @Override
    @Transactional
    public Map<String, Object> submitExam(Long examId, Long userId, Map<Long, String> answers, Long attemptId) {
        Exam exam = examMapper.findById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        
        List<Question> questions = getExamQuestions(examId);
        int totalScore = 0;
        int maxScore = 0;
        
        // 计算得分（只计算客观题）
        for (Question q : questions) {
            if (q.getType().equals("open_question") || q.getType().equals("open question")) {
                // 主观题不计入自动评分
                continue;
            }
            maxScore += q.getPoints();
            String userAnswer = answers.get(q.getId());
            if (userAnswer != null && userAnswer.trim().equalsIgnoreCase(q.getCorrectAnswer().trim())) {
                totalScore += q.getPoints();
            }
        }
        
        // 使用统一的submitTime保存所有答案
        LocalDateTime submitTime = LocalDateTime.now();
        for (Map.Entry<Long, String> entry : answers.entrySet()) {
            ExamAnswer existing = examAnswerMapper.findByExamIdAndUserIdAndQuestionId(
                examId, userId, entry.getKey());
            
            ExamAnswer answer = new ExamAnswer();
            answer.setExamId(examId);
            answer.setUserId(userId);
            answer.setQuestionId(entry.getKey());
            answer.setAnswer(entry.getValue());
            answer.setSubmitTime(submitTime);
            
            // 计算该题的得分
            Question q = questions.stream()
                .filter(question -> question.getId().equals(entry.getKey()))
                .findFirst()
                .orElse(null);
            if (q != null && !q.getType().equals("open_question") && !q.getType().equals("open question")) {
                if (entry.getValue() != null && entry.getValue().trim().equalsIgnoreCase(q.getCorrectAnswer().trim())) {
                    answer.setScore(q.getPoints());
                } else {
                    answer.setScore(0);
                }
            } else {
                answer.setScore(0); // 主观题暂时为0
            }
            
            if (existing != null) {
                answer.setId(existing.getId());
                examAnswerMapper.update(answer);
            } else {
                examAnswerMapper.insert(answer);
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", totalScore);
        result.put("maxScore", maxScore);
        result.put("percentage", maxScore > 0 ? (totalScore * 100.0 / maxScore) : 0);
        result.put("attemptId", submitTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000);
        
        return result;
    }

    @Override
    @Transactional
    public void deleteExam(Long examId, Long userId) {
        Exam exam = examMapper.findById(examId);
        if (exam == null) {
            throw new RuntimeException("考试不存在");
        }
        if (!exam.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此考试");
        }
        // 删除考试（级联删除题目和答案）
        examMapper.deleteById(examId);
    }

}

