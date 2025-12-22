package com.example.demo.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private Environment environment;
    
    private String deepseekApiKey;

    @Autowired(required = false)
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiController() {
        if (restTemplate == null) {
            this.restTemplate = new RestTemplate();
        }
    }
    
    /**
     * 获取 DeepSeek API Key
     * 优先级：deepseek.api.key > DEEPSEEK_API_KEY 环境变量
     */
    private String getDeepseekApiKey() {
        if (deepseekApiKey == null) {
            // 先从 application.properties 或系统属性中读取
            if (environment != null) {
                deepseekApiKey = environment.getProperty("deepseek.api.key");
                // 如果为空，再从环境变量中读取
                if (deepseekApiKey == null || deepseekApiKey.trim().isEmpty()) {
                    deepseekApiKey = environment.getProperty("DEEPSEEK_API_KEY");
                }
            }
            
            // 如果还是为空，尝试从系统属性中读取
            if (deepseekApiKey == null || deepseekApiKey.trim().isEmpty()) {
                deepseekApiKey = System.getProperty("deepseek.api.key");
            }
            if (deepseekApiKey == null || deepseekApiKey.trim().isEmpty()) {
                deepseekApiKey = System.getProperty("DEEPSEEK_API_KEY");
            }
            
            // 最后尝试从系统环境变量中读取
            if (deepseekApiKey == null || deepseekApiKey.trim().isEmpty()) {
                deepseekApiKey = System.getenv("DEEPSEEK_API_KEY");
            }
            if (deepseekApiKey == null || deepseekApiKey.trim().isEmpty()) {
                deepseekApiKey = System.getenv("deepseek.api.key");
            }
        }
        return deepseekApiKey;
    }

    /**
     * AI 聊天接口
     * POST /api/ai/chat
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> request) {
        try {
            // 检查 API Key 是否配置
            String apiKey = getDeepseekApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "DeepSeek API Key 未配置。请在 application.properties 中设置 deepseek.api.key，或设置环境变量 DEEPSEEK_API_KEY");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            @SuppressWarnings("unchecked")
            List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
            
            if (messages == null || messages.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "请求格式错误：需要 messages 数组");
                return ResponseEntity.badRequest().body(error);
            }

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "deepseek-chat");
            payload.put("messages", messages);
            payload.put("max_tokens", 4096);
            payload.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.deepseek.com/v1/chat/completions",
                    entity,
                    Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();
            if (body == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "AI 接口返回为空");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "AI 返回格式异常");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }

            @SuppressWarnings("unchecked")
            Map<String, String> message = (Map<String, String>) choices.get(0).get("message");
            String content = message.get("content");

            Map<String, Object> result = new HashMap<>();
            result.put("content", content);
            return ResponseEntity.ok(result);
        } catch (org.springframework.web.client.HttpClientErrorException.Unauthorized e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "DeepSeek API Key 无效或已过期。请检查配置的 API Key 是否正确。");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.contains("401")) {
                error.put("error", "DeepSeek API Key 无效或未配置。请在 application.properties 中设置 deepseek.api.key，或设置环境变量 DEEPSEEK_API_KEY");
            } else {
                error.put("error", "AI 接口请求失败：" + errorMsg);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 文件上传 + AI 生成笔记接口
     * POST /api/ai/generate-note
     */
    @PostMapping(value = "/generate-note", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> generateNote(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "noteName", required = false) String noteName,
            @RequestParam(value = "subjectName", required = false) String subjectName) {
        
        System.out.println("收到生成笔记请求，文件数量: " + (files != null ? files.length : 0));
        
        try {
            if (files == null || files.length == 0) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "请上传至少一个文件");
                return ResponseEntity.badRequest().body(error);
            }

            // 读取所有文件内容
            List<String> contentList = new ArrayList<>();
            for (MultipartFile file : files) {
                try {
                    String content = readFileContent(file.getBytes(), file.getOriginalFilename());
                    contentList.add("--- 文件: " + file.getOriginalFilename() + " ---\n" + content);
                } catch (Exception e) {
                    contentList.add("--- 文件: " + file.getOriginalFilename() + " ---\n[读取失败: " + e.getMessage() + "]");
                }
            }

            String allContent = String.join("\n\n", contentList);
            
            if (!StringUtils.hasText(allContent.trim())) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "无法读取文件内容，请检查文件格式");
                return ResponseEntity.badRequest().body(error);
            }

            // 构建 AI 提示词
            String systemPrompt = buildNoteSystemPrompt();
            String userPrompt = buildNoteUserPrompt(subjectName, noteName, allContent);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "deepseek-chat");
            payload.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            payload.put("max_tokens", 8192);
            payload.put("temperature", 0.3);

            String apiKey = getDeepseekApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "DeepSeek API Key 未配置。请在 application.properties 中设置 deepseek.api.key，或设置环境变量 DEEPSEEK_API_KEY");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.deepseek.com/v1/chat/completions",
                    entity,
                    Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();
            if (body == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "AI 接口返回为空");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "AI 返回格式异常");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }

            @SuppressWarnings("unchecked")
            Map<String, String> message = (Map<String, String>) choices.get(0).get("message");
            String noteContent = message.get("content");

            Map<String, Object> result = new HashMap<>();
            Map<String, Object> note = new HashMap<>();
            note.put("title", noteName != null ? noteName : "学习笔记");
            note.put("content", noteContent);
            note.put("subjectName", subjectName != null ? subjectName : "");
            note.put("createdAt", new Date().toInstant().toString());
            result.put("note", note);
            System.out.println("笔记生成成功");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            System.err.println("生成笔记失败: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            String errorMsg = e.getMessage();
            if (errorMsg == null) {
                errorMsg = "未知错误";
            }
            // 如果是数据库相关错误，提供更友好的提示
            if (errorMsg.contains("SQL") || errorMsg.contains("database") || errorMsg.contains("Unknown column")) {
                error.put("error", "数据库错误：请确保已执行数据库迁移脚本，添加 options 和 explanation 字段到 questions 表。SQL: ALTER TABLE questions ADD COLUMN options TEXT, ADD COLUMN explanation TEXT;");
            } else {
                error.put("error", "生成笔记失败：" + errorMsg);
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * AI 生成测试题接口
     * POST /api/ai/generate-exam
     */
    @PostMapping("/generate-exam")
    public ResponseEntity<Map<String, Object>> generateExam(@RequestBody Map<String, Object> request) {
        try {
            String noteContent = (String) request.get("noteContent");
            Integer questionCount = (Integer) request.getOrDefault("questionCount", 8);
            String difficulty = (String) request.getOrDefault("difficulty", "medium");
            @SuppressWarnings("unchecked")
            List<String> selectedTypes = (List<String>) request.getOrDefault("selectedTypes", 
                    Arrays.asList("single", "true-false", "open"));

            if (noteContent == null || noteContent.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "noteContent is required");
                return ResponseEntity.badRequest().body(error);
            }

            String systemPrompt = buildExamSystemPrompt();
            String userPrompt = buildExamUserPrompt(noteContent, questionCount, difficulty, selectedTypes);

            Map<String, Object> payload = new HashMap<>();
            payload.put("model", "deepseek-chat");
            payload.put("messages", Arrays.asList(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            ));
            payload.put("max_tokens", 4096);
            payload.put("temperature", 0.4);

            String apiKey = getDeepseekApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "DeepSeek API Key 未配置。请在 application.properties 中设置 deepseek.api.key，或设置环境变量 DEEPSEEK_API_KEY");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.deepseek.com/v1/chat/completions",
                    entity,
                    Map.class
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> body = response.getBody();
            if (body == null) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "AI 接口返回为空");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
            if (choices == null || choices.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "AI 返回格式异常");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }

            @SuppressWarnings("unchecked")
            Map<String, String> message = (Map<String, String>) choices.get(0).get("message");
            String raw = message.get("content");

            // 解析 JSON
            String cleaned = raw.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\s*", "").replaceAll("```$", "").trim();
            }
            int firstBrace = cleaned.indexOf('{');
            int lastBrace = cleaned.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                cleaned = cleaned.substring(firstBrace, lastBrace + 1);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> parsed = objectMapper.readValue(cleaned, Map.class);
            
            if (parsed == null || !parsed.containsKey("questions")) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "AI 未返回任何题目，请尝试缩短笔记或减少题目数量");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("questions", parsed.get("questions"));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "生成试卷失败：" + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 读取文件内容
     */
    private String readFileContent(byte[] bytes, String filename) throws IOException {
        if (filename == null) {
            return "";
        }
        String lowerName = filename.toLowerCase();
        
        if (lowerName.endsWith(".txt") || lowerName.endsWith(".md")) {
            return new String(bytes, StandardCharsets.UTF_8);
        }
        
        if (lowerName.endsWith(".pdf")) {
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        }
        
        if (lowerName.endsWith(".ppt") || lowerName.endsWith(".pptx")) {
            return "[PPT 文件暂未能正确解析，请优先尝试将 PPT 导出为 PDF 或 TXT 上传。]";
        }
        
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".png")) {
            return "[图片文件，暂不支持自动提取文字，请手动输入关键内容]";
        }
        
        if (lowerName.endsWith(".doc") || lowerName.endsWith(".docx")) {
            return "[Word 文件，建议转为 PDF 或 TXT 格式上传]";
        }
        
        return "";
    }

    private String buildNoteSystemPrompt() {
        return "You are a professional study assistant specializing in creating high-quality, **detailed and complete** study notes.\n\n" +
                "Your task is to transform the user's study materials into **well-structured, exam-ready notes**.\n\n" +
                "## CRITICAL REQUIREMENTS:\n\n" +
                "### 1. COMPLETENESS (most important)\n" +
                "- **Include ALL key concepts, definitions, theorems and main conclusions** from the source material.\n" +
                "- If the source covers multiple topics, cover ALL of them.\n" +
                "- Include important dates, names and formulas when they are essential.\n\n" +
                "### 2. LEVEL OF DETAIL\n" +
                "- For each important concept, use **3–6 sentences** to explain:\n" +
                "  - the definition / idea,\n" +
                "  - why it matters,\n" +
                "  - how it is used (briefly).\n" +
                "- For core theorems / principles, you may add:\n" +
                "  - short intuition,\n" +
                "  - typical application scenario,\n" +
                "  - one simple example if space allows.\n" +
                "- Avoid long derivations or very lengthy examples, but make sure the student can understand and review from these notes alone.\n\n" +
                "### 3. STRUCTURE\n" +
                "- Use clear Markdown hierarchy: # for main title, ## for sections, ### for subsections.\n" +
                "- Group related points under the same subsection, and use numbered / bullet lists to organize them.\n" +
                "- When the chapter is long, prefer more sections + subsections instead of huge paragraphs.\n\n" +
                "### 4. HIGHLIGHTING & REVIEW\n" +
                "- Use **bold** for key terms, definitions and exam-critical conclusions.\n" +
                "- Use simple tables when it helps compare or contrast concepts.\n" +
                "- Add a short **\"Key Takeaways\"** section at the end that lists 5–10 bullet points summarizing the chapter.\n\n" +
                "### 5. FORMAT\n" +
                "- Write in the same language as the source material.\n" +
                "- Use Markdown format only.\n" +
                "- Do NOT add any meta comments about how you created the notes.\n" +
                "- Start directly with the content.";
    }

    private String buildNoteUserPrompt(String subjectName, String noteName, String allContent) {
        return "Please create detailed and complete study notes from the following materials:\n\n" +
                "Subject: " + (subjectName != null ? subjectName : "General") + "\n" +
                "Topic: " + (noteName != null ? noteName : "Study Notes") + "\n\n" +
                "=== SOURCE MATERIALS ===\n" +
                allContent + "\n" +
                "=== END OF MATERIALS ===\n\n" +
                "Make sure all important concepts are included, and explain each important point clearly (around 3–6 sentences), so that a student can review and understand the topic using these notes alone.";
    }

    private String buildExamSystemPrompt() {
        return "You are an experienced exam designer. Based on the given study notes, " +
                "you will create a high-quality exam that checks real understanding.\n\n" +
                "You MUST output **valid JSON only**, no explanation text, using this exact TypeScript-like schema:\n\n" +
                "{\n" +
                "  \"questions\": Array<{\n" +
                "    \"id\": string;\n" +
                "    \"type\": \"single\" | \"true-false\" | \"open\";\n" +
                "    \"stem\": string;\n" +
                "    \"options\"?: Array<{ \"value\": string; \"label\": string; \"text\": string }>;\n" +
                "    \"answer\": string;\n" +
                "    \"explanation\": string;\n" +
                "    \"points\": number;\n" +
                "  }>\n" +
                "}\n\n" +
                "## CRITICAL REQUIREMENTS:\n\n" +
                "### Question Types:\n" +
                "1. **single** (single choice): Must have 4 options (A, B, C, D). Each option has { \"value\": \"A\", \"label\": \"A\", \"text\": \"option content\" }.\n" +
                "2. **true-false**: The \"answer\" must be exactly \"True\" or \"False\". Do NOT include options array for true-false questions (the frontend will generate them).\n" +
                "3. **open**: No options needed, just stem and answer.\n\n" +
                "### Explanation Quality (VERY IMPORTANT):\n" +
                "Each explanation must be **detailed and educational** (4-6 sentences), including:\n" +
                "- WHY the correct answer is correct (with reasoning or evidence from the notes)\n" +
                "- WHY the wrong options are incorrect (for single choice questions)\n" +
                "- Any key concept or background knowledge that helps understand the answer\n" +
                "- A brief tip for remembering or applying this knowledge\n\n" +
                "### Other Requirements:\n" +
                "- Cover ALL the main concepts in the notes, not just the first part.\n" +
                "- Mix question types: about 50% single choice, 30% true-false, 20% open questions.\n" +
                "- For single choice questions, make distractors plausible but clearly wrong.\n" +
                "- Points should be between 2 and 8 depending on difficulty and question type.\n" +
                "- Difficulty level hint:\n" +
                "  - easy: more direct recall questions.\n" +
                "  - medium: mix of recall and understanding.\n" +
                "  - hard: more application or multi-step reasoning.";
    }

    private String buildExamUserPrompt(String noteContent, int questionCount, String difficulty, List<String> selectedTypes) {
        return "Please create an exam from the following study notes.\n\n" +
                "Question count: about " + questionCount + "\n" +
                "Difficulty: " + difficulty + "\n" +
                "Preferred question types (hint from frontend, you MUST still follow the JSON schema above):\n" +
                "- Selected types: " + String.join(", ", selectedTypes) + "\n\n" +
                "Please:\n" +
                "- Prioritize the selected types when distributing questions.\n" +
                "- If a type is not included in the selected list, avoid generating that type unless absolutely necessary to cover key concepts.\n\n" +
                "=== STUDY NOTES START ===\n" +
                noteContent + "\n" +
                "=== STUDY NOTES END ===\n\n" +
                "Return ONLY JSON following the schema above. Do NOT wrap it in markdown or add any extra text.";
    }
}
