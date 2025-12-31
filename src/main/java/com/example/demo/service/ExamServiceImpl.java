package com.example.demo.service;

import com.example.demo.entity.Exam;
import com.example.demo.entity.ExamAnswer;
import com.example.demo.entity.ExamAttempt;
import com.example.demo.entity.ExamSM2Stats;
import com.example.demo.entity.Note;
import com.example.demo.entity.Option;
import com.example.demo.entity.Question;
import com.example.demo.mapper.ExamAnswerMapper;
import com.example.demo.mapper.ExamAttemptMapper;
import com.example.demo.mapper.ExamMapper;
import com.example.demo.mapper.ExamSM2StatsMapper;
import com.example.demo.mapper.NoteMapper;
import com.example.demo.mapper.QuestionMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private ExamAttemptMapper examAttemptMapper;

    @Autowired
    private ExamSM2StatsMapper examSM2StatsMapper;

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
        
        if (aiQuestions == null || aiQuestions.isEmpty()) {
            throw new RuntimeException("AI未返回任何题目");
        }

        Note note = noteMapper.findById(noteId);
        if (note == null) {
            throw new RuntimeException("笔记不存在");
        }

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

        List<Question> questions = new ArrayList<>();
        for (Map<String, Object> q : aiQuestions) {
            Question question = new Question();
            question.setExamId(exam.getId());
            question.setTitle((String) q.get("stem"));

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
                
                List<Option> options = Arrays.asList(
                    new Option("True", "A", "True"),
                    new Option("False", "B", "False")
                );
                question.setOptions(options);
            }

            insertQuestionWithOptions(question);
            questions.add(question);
        }

        exam.setQuestions(questions);
        return exam;
    }

    @Override
    public List<Question> getExamQuestions(Long examId) {
        List<Question> questions = questionMapper.findByExamId(examId);
        
        for (Question q : questions) {
            parseOptionsFromJson(q);
        }
        return questions;
    }

    private void insertQuestionWithOptions(Question question) {
        try {
            
            String optionsJson = null;
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                optionsJson = objectMapper.writeValueAsString(question.getOptions());
            }
            question.setOptionsJson(optionsJson);

            questionMapper.insert(question);
        } catch (Exception e) {
            throw new RuntimeException("保存题目失败: " + e.getMessage(), e);
        }
    }

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
                
            }
        }
    }

    @Override
    @Transactional
    public Long saveUserAnswers(Long examId, Long userId, Map<Long, String> answers, Long attemptId) {
        
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
            
            if (existing != null) {
                answer.setId(existing.getId());
                examAnswerMapper.update(answer);
            } else {
                examAnswerMapper.insert(answer);
            }
        }

        return submitTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000;
    }

    @Override
    public List<ExamAnswer> getExamAnswers(Long examId, Long userId, Long attemptId) {
        List<ExamAnswer> allAnswers = examAnswerMapper.findByExamIdAndUserId(examId, userId);
        
        if (attemptId == null) {
            
            if (allAnswers.isEmpty()) {
                return new ArrayList<>();
            }
            
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
            
            ExamAttempt attempt = examAttemptMapper.findById(attemptId);
            if (attempt != null) {
                LocalDateTime targetTime = attempt.getSubmitTime();
                return allAnswers.stream()
                    .filter(a -> a.getSubmitTime().equals(targetTime))
                    .collect(java.util.stream.Collectors.toList());
            }
            
            LocalDateTime targetTime = LocalDateTime.ofEpochSecond(attemptId / 1000, 0, java.time.ZoneOffset.UTC);
            return allAnswers.stream()
                .filter(a -> a.getSubmitTime().equals(targetTime))
                .collect(java.util.stream.Collectors.toList());
        }
    }

    @Override
    public List<Map<String, Object>> getExamAttempts(Long examId, Long userId) {
        
        List<ExamAttempt> examAttempts = examAttemptMapper.findByExamIdAndUserId(examId, userId);
        
        List<Map<String, Object>> attempts = new ArrayList<>();
        for (ExamAttempt attempt : examAttempts) {
            
            List<ExamAnswer> answers = examAnswerMapper.findByExamIdAndUserId(examId, userId);
            
            List<ExamAnswer> attemptAnswers = answers.stream()
                .filter(a -> a.getSubmitTime().equals(attempt.getSubmitTime()))
                .collect(java.util.stream.Collectors.toList());
            
            Map<String, Object> attemptMap = new HashMap<>();
            attemptMap.put("id", attempt.getId());
            attemptMap.put("submitTime", attempt.getSubmitTime());
            attemptMap.put("createdAt", attempt.getSubmitTime());
            attemptMap.put("score", attempt.getTotalScore());
            attemptMap.put("maxScore", attempt.getMaxScore());
            attemptMap.put("percentage", attempt.getPercentage() != null ? attempt.getPercentage().doubleValue() : 0.0);
            attemptMap.put("hasSubmitted", true); 
            attemptMap.put("answers", attemptAnswers.stream().collect(java.util.stream.Collectors.toMap(
                a -> a.getQuestionId().toString(),
                ExamAnswer::getAnswer
            )));
            attempts.add(attemptMap);
        }

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

        for (Question q : questions) {
            if (q.getType().equals("open_question") || q.getType().equals("open question")) {
                
                continue;
            }
            maxScore += q.getPoints();
            String userAnswer = answers.get(q.getId());
            if (userAnswer != null && userAnswer.trim().equalsIgnoreCase(q.getCorrectAnswer().trim())) {
                totalScore += q.getPoints();
            }
        }

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
                answer.setScore(0); 
            }
            
            if (existing != null) {
                answer.setId(existing.getId());
                examAnswerMapper.update(answer);
            } else {
                examAnswerMapper.insert(answer);
            }
        }

        double percentageValue = maxScore > 0 ? (totalScore * 100.0 / maxScore) : 0;
        BigDecimal percentage = BigDecimal.valueOf(percentageValue).setScale(2, RoundingMode.HALF_UP);

        ExamAttempt attempt = new ExamAttempt();
        attempt.setExamId(examId);
        attempt.setUserId(userId);
        attempt.setTotalScore(totalScore);
        attempt.setMaxScore(maxScore);
        attempt.setPercentage(percentage);
        attempt.setSubmitTime(submitTime);
        examAttemptMapper.insert(attempt);

        updateSM2Stats(examId, userId, percentageValue, submitTime);
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalScore", totalScore);
        result.put("maxScore", maxScore);
        result.put("percentage", percentageValue);
        result.put("attemptId", attempt.getId());
        
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
        
        examMapper.deleteById(examId);
    }

    private int calculateQuality(double percentage) {
        if (percentage >= 90) return 5;
        if (percentage >= 70) return 4;
        if (percentage >= 50) return 3;
        if (percentage >= 30) return 2;
        if (percentage > 0) return 1;
        return 0;
    }

    private void updateSM2Stats(Long examId, Long userId, double percentage, LocalDateTime submitTime) {
        ExamSM2Stats stats = examSM2StatsMapper.findByExamIdAndUserId(examId, userId);
        int quality = calculateQuality(percentage);
        
        if (stats == null) {
            
            stats = new ExamSM2Stats();
            stats.setExamId(examId);
            stats.setUserId(userId);
            stats.setEaseFactor(BigDecimal.valueOf(2.5));
            stats.setIntervalDays(1);
            stats.setLastReviewDate(submitTime);
            stats.setNextReviewDate(submitTime.plusDays(1));
            examSM2StatsMapper.insert(stats);
        }

        BigDecimal ef = stats.getEaseFactor();
        if (quality >= 3) {
            double newEF = ef.doubleValue() + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02));
            ef = BigDecimal.valueOf(Math.max(1.3, newEF));
        }
        stats.setEaseFactor(ef);

        int newInterval;
        if (quality < 3) {
            newInterval = 1; 
        } else {
            int currentInterval = stats.getIntervalDays();
            if (currentInterval == 1) {
                newInterval = 6; 
            } else {
                newInterval = (int)(currentInterval * ef.doubleValue());
            }
        }
        stats.setIntervalDays(newInterval);
        stats.setLastReviewDate(submitTime);
        stats.setNextReviewDate(submitTime.plusDays(newInterval));
        
        examSM2StatsMapper.update(stats);
    }

    @Override
    public List<Map<String, Object>> getReviewExams(Long userId, int limit) {
        LocalDateTime now = LocalDateTime.now();
        List<ExamSM2Stats> sm2Stats = examSM2StatsMapper.findByUserId(userId);
        List<Map<String, Object>> reviewExams = new ArrayList<>();
        
        for (ExamSM2Stats stat : sm2Stats) {
            
            if (stat.getNextReviewDate() != null && 
                (stat.getNextReviewDate().isBefore(now) || stat.getNextReviewDate().isEqual(now))) {
                
                Exam exam = examMapper.findById(stat.getExamId());
                if (exam == null) continue;

                long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(stat.getNextReviewDate(), now);
                double priority = 1.0 + Math.min(overdueDays / 10.0, 1.0); 

                List<ExamAttempt> attempts = examAttemptMapper.findByExamIdAndUserId(stat.getExamId(), userId);
                Object lastSubmitTime = null;
                Object lastPercentage = null;
                if (!attempts.isEmpty()) {
                    ExamAttempt lastAttempt = attempts.get(0); 
                    lastSubmitTime = lastAttempt.getSubmitTime();
                    lastPercentage = lastAttempt.getPercentage() != null ? 
                        lastAttempt.getPercentage().doubleValue() : null;
                }
                
                Map<String, Object> reviewExam = new HashMap<>();
                reviewExam.put("exam", exam);
                reviewExam.put("priority", priority);
                reviewExam.put("lastSubmitTime", lastSubmitTime);
                reviewExam.put("lastPercentage", lastPercentage);
                reviewExam.put("attemptCount", attempts.size());
                reviewExam.put("nextReviewDate", stat.getNextReviewDate());
                reviewExams.add(reviewExam);
            }
        }

        if (reviewExams.isEmpty()) {
            return getReviewExamsFallback(userId, limit);
        }

        reviewExams.sort((a, b) -> Double.compare((Double) b.get("priority"), (Double) a.get("priority")));
        return reviewExams.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }

    private List<Map<String, Object>> getReviewExamsFallback(Long userId, int limit) {
        List<Map<String, Object>> statistics = examAttemptMapper.getReviewStatisticsByUserId(userId);
        if (statistics == null || statistics.isEmpty()) {
            return new ArrayList<>();
        }
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        List<Map<String, Object>> reviewExams = new ArrayList<>();
        
        for (Map<String, Object> stat : statistics) {
            Object examIdObj = stat.get("exam_id");
            if (examIdObj == null || !(examIdObj instanceof Number)) continue;
            
            Long examId = ((Number) examIdObj).longValue();
            Object lastSubmitTimeObj = stat.get("last_submit_time");
            
            if (lastSubmitTimeObj != null) {
                LocalDateTime lastSubmitTime = (LocalDateTime) lastSubmitTimeObj;
                if (lastSubmitTime.isAfter(todayStart) || lastSubmitTime.isEqual(todayStart)) {
                    continue;
                }
            }
            
            Exam exam = examMapper.findById(examId);
            if (exam == null) continue;
            
            double priority = lastSubmitTimeObj == null ? 1.0 : 0.5;
            
            Map<String, Object> reviewExam = new HashMap<>();
            reviewExam.put("exam", exam);
            reviewExam.put("priority", priority);
            reviewExam.put("lastSubmitTime", lastSubmitTimeObj);
            reviewExam.put("lastPercentage", stat.get("last_percentage"));
            reviewExam.put("attemptCount", stat.getOrDefault("attempt_count", 0));
            reviewExams.add(reviewExam);
        }
        
        reviewExams.sort((a, b) -> Double.compare((Double) b.get("priority"), (Double) a.get("priority")));
        return reviewExams.stream().limit(limit).collect(java.util.stream.Collectors.toList());
    }

}

