package com.example.demo.controller;

import com.example.demo.entity.Exam;
import com.example.demo.entity.Question;
import com.example.demo.service.ExamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    @Autowired
    private com.example.demo.controller.AiController aiController;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getExamByNoteId(
            @RequestParam Long noteId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "false") Boolean all) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (Boolean.TRUE.equals(all)) {
                
                List<Exam> exams = examService.getExamsByNoteId(noteId);
                response.put("success", true);
                response.put("data", exams);
                return ResponseEntity.ok(response);
            } else {
                
                Exam exam = examService.getExamByNoteId(noteId);
                if (exam == null) {
                    response.put("success", false);
                    response.put("message", "该笔记还没有生成考试");
                    return ResponseEntity.ok(response);
                }
                response.put("success", true);
                response.put("data", exam);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getExamById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        try {
            Exam exam = examService.getExamById(id);
            if (exam == null) {
                response.put("success", false);
                response.put("message", "考试不存在");
                return ResponseEntity.badRequest().body(response);
            }
            response.put("success", true);
            response.put("data", exam);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, Object>> generateExam(@RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            Long noteId = Long.valueOf(request.get("noteId").toString());
            Long userId = Long.valueOf(request.get("userId").toString());
            String noteContent = (String) request.get("noteContent");
            Integer questionCount = (Integer) request.getOrDefault("questionCount", 8);
            String difficulty = (String) request.getOrDefault("difficulty", "medium");
            String examName = (String) request.getOrDefault("examName", null);

            if (noteContent == null || noteContent.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "笔记内容不能为空");
                return ResponseEntity.badRequest().body(response);
            }

            Map<String, Object> aiRequest = new HashMap<>();
            aiRequest.put("noteContent", noteContent);
            aiRequest.put("questionCount", questionCount);
            aiRequest.put("difficulty", difficulty);
            aiRequest.put("selectedTypes", request.getOrDefault("selectedTypes", 
                    java.util.Arrays.asList("single", "true-false", "open")));

            ResponseEntity<Map<String, Object>> aiResponse = aiController.generateExam(aiRequest);
            
            if (!aiResponse.getStatusCode().is2xxSuccessful()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> aiBody = (Map<String, Object>) aiResponse.getBody();
                response.put("success", false);
                response.put("message", aiBody != null ? aiBody.get("error") : "AI生成失败");
                return ResponseEntity.badRequest().body(response);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> aiBody = (Map<String, Object>) aiResponse.getBody();
            if (aiBody == null || !aiBody.containsKey("questions")) {
                response.put("success", false);
                response.put("message", "AI未返回任何题目");
                return ResponseEntity.badRequest().body(response);
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> aiQuestions = (List<Map<String, Object>>) aiBody.get("questions");

            Exam exam = examService.createExamFromAI(noteId, userId, noteContent, questionCount, difficulty, aiQuestions, examName);

            List<Map<String, Object>> formattedQuestions = formatQuestionsForFrontend(exam.getQuestions(), aiQuestions);

            response.put("success", true);
            response.put("data", exam);
            response.put("questions", formattedQuestions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteExam(
            @PathVariable Long id,
            @RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            examService.deleteExam(id, userId);
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{examId}/attempts")
    public ResponseEntity<Map<String, Object>> getExamAttempts(
            @PathVariable Long examId,
            @RequestParam Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> attempts = examService.getExamAttempts(examId, userId);
            response.put("success", true);
            response.put("data", attempts);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{examId}/answers")
    public ResponseEntity<Map<String, Object>> saveAnswers(
            @PathVariable Long examId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long attemptId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> answersMap = (Map<String, String>) request.get("answers");
            Map<Long, String> answers = new HashMap<>();
            for (Map.Entry<String, String> entry : answersMap.entrySet()) {
                answers.put(Long.valueOf(entry.getKey()), entry.getValue());
            }
            
            Long newAttemptId = examService.saveUserAnswers(examId, userId, answers, attemptId);
            response.put("success", true);
            if (newAttemptId != null) {
                response.put("attemptId", newAttemptId);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/{examId}/answers")
    public ResponseEntity<Map<String, Object>> getAnswers(
            @PathVariable Long examId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long attemptId) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<com.example.demo.entity.ExamAnswer> answers = examService.getExamAnswers(examId, userId, attemptId);
            response.put("success", true);
            response.put("data", answers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{examId}/submit")
    public ResponseEntity<Map<String, Object>> submitExam(
            @PathVariable Long examId,
            @RequestParam Long userId,
            @RequestParam(required = false) Long attemptId,
            @RequestBody Map<String, Object> request) {
        Map<String, Object> response = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> answersMap = (Map<String, String>) request.get("answers");
            Map<Long, String> answers = new HashMap<>();
            for (Map.Entry<String, String> entry : answersMap.entrySet()) {
                answers.put(Long.valueOf(entry.getKey()), entry.getValue());
            }
            
            Map<String, Object> result = examService.submitExam(examId, userId, answers, attemptId);
            response.put("success", true);
            response.putAll(result);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/review")
    public ResponseEntity<Map<String, Object>> getReviewExams(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "5") int limit) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> reviewExams = examService.getReviewExams(userId, limit);
            response.put("success", true);
            response.put("data", reviewExams);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            response.put("error", e.getClass().getName());
            return ResponseEntity.badRequest().body(response);
        }
    }

    private List<Map<String, Object>> formatQuestionsForFrontend(
            List<Question> questions, List<Map<String, Object>> aiQuestions) {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            Map<String, Object> aiQ = i < aiQuestions.size() ? aiQuestions.get(i) : new HashMap<>();
            
            Map<String, Object> formatted = new HashMap<>();
            formatted.put("id", q.getId());
            formatted.put("title", q.getTitle());

            String type = q.getType();
            if ("single_choice".equals(type)) {
                formatted.put("type", "single choice");
            } else if ("true_false".equals(type)) {
                formatted.put("type", "true-false");
            } else if ("open_question".equals(type)) {
                formatted.put("type", "open question");
            } else {
                formatted.put("type", type);
            }
            
            formatted.put("points", q.getPoints());
            formatted.put("correctAnswer", q.getCorrectAnswer());
            formatted.put("explanation", aiQ.getOrDefault("explanation", ""));

            if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                List<Map<String, Object>> options = new java.util.ArrayList<>();
                for (com.example.demo.entity.Option opt : q.getOptions()) {
                    Map<String, Object> optionMap = new HashMap<>();
                    optionMap.put("value", opt.getValue());
                    optionMap.put("label", opt.getLabel());
                    optionMap.put("text", opt.getText());
                    options.add(optionMap);
                }
                formatted.put("options", options);
            } else if ("true-false".equals(formatted.get("type"))) {
                
                List<Map<String, Object>> options = new java.util.ArrayList<>();
                options.add(Map.of("value", "True", "label", "A", "text", "True"));
                options.add(Map.of("value", "False", "label", "B", "text", "False"));
                formatted.put("options", options);
            }
            
            result.add(formatted);
        }
        
        return result;
    }
}

