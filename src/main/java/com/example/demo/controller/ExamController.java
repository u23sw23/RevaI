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

    /**
     * 根据笔记ID获取考试
     * GET /api/exams?noteId=xxx&userId=xxx&all=true (获取所有考试)
     * GET /api/exams?noteId=xxx&userId=xxx (获取第一个考试，兼容旧版本)
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getExamByNoteId(
            @RequestParam Long noteId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false, defaultValue = "false") Boolean all) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (Boolean.TRUE.equals(all)) {
                // 获取该笔记下的所有考试
                List<Exam> exams = examService.getExamsByNoteId(noteId);
                response.put("success", true);
                response.put("data", exams);
                return ResponseEntity.ok(response);
            } else {
                // 兼容旧版本：返回第一个考试
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

    /**
     * 根据考试ID获取考试详情
     * GET /api/exams/{id}
     */
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

    /**
     * 使用AI生成考试
     * POST /api/exams/generate
     */
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

            // 先调用AI生成题目
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

            // 创建Exam并保存题目（支持examName）
            Exam exam = examService.createExamFromAI(noteId, userId, noteContent, questionCount, difficulty, aiQuestions, examName);
            
            // 转换AI返回的题目格式为前端需要的格式
            List<Map<String, Object>> formattedQuestions = formatQuestionsForFrontend(exam.getQuestions(), aiQuestions);

            response.put("success", true);
            response.put("data", exam);
            response.put("questions", formattedQuestions);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 删除考试
     * DELETE /api/exams/{id}?userId=xxx
     */
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

    /**
     * 获取考试的所有作答记录
     * GET /api/exams/{examId}/attempts?userId=xxx
     */
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

    /**
     * 保存用户答案
     * POST /api/exams/{examId}/answers?userId=xxx&attemptId=xxx
     */
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

    /**
     * 获取作答记录的答案
     * GET /api/exams/{examId}/answers?userId=xxx&attemptId=xxx
     */
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

    /**
     * 提交考试
     * POST /api/exams/{examId}/submit?userId=xxx&attemptId=xxx
     */
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
            e.printStackTrace();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 格式化题目为前端需要的格式
     */
    private List<Map<String, Object>> formatQuestionsForFrontend(
            List<Question> questions, List<Map<String, Object>> aiQuestions) {
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            Map<String, Object> aiQ = i < aiQuestions.size() ? aiQuestions.get(i) : new HashMap<>();
            
            Map<String, Object> formatted = new HashMap<>();
            formatted.put("id", q.getId());
            formatted.put("title", q.getTitle());
            
            // 转换类型
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
            
            // 处理选项
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
                // 判断题自动生成选项
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

