/**
 * AI 小精灵后端服务
 *
 * 功能：
 * 1. 代理 DeepSeek API 请求，保护 API Key 安全
 * 2. 文件上传 + AI 生成笔记
 *
 * 启动方式：
 *   1. 先创建 .env 文件，填入 DEEPSEEK_API_KEY
 *   2. npm install
 *   3. npm start
 */

import express from 'express'
import cors from 'cors'
import multer from 'multer'
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
import 'dotenv/config'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const app = express()
const PORT = process.env.PORT || 3001

// 从环境变量读取 API Key（安全！）
const DEEPSEEK_API_KEY = process.env.DEEPSEEK_API_KEY

if (!DEEPSEEK_API_KEY) {
  console.error('❌ 错误：请在 .env 文件中配置 DEEPSEEK_API_KEY')
  console.error('   示例：DEEPSEEK_API_KEY=sk-xxxxxxxx')
  process.exit(1)
}

// 创建上传目录
const uploadDir = path.join(__dirname, 'uploads')
if (!fs.existsSync(uploadDir)) {
  fs.mkdirSync(uploadDir, { recursive: true })
}

// 配置 multer 文件上传
const storage = multer.diskStorage({
  destination: (req, file, cb) => {
    cb(null, uploadDir)
  },
  filename: (req, file, cb) => {
    // 处理中文文件名
    const originalName = Buffer.from(file.originalname, 'latin1').toString('utf8')
    const uniqueSuffix = Date.now() + '-' + Math.round(Math.random() * 1e9)
    const ext = path.extname(originalName)
    cb(null, uniqueSuffix + ext)
  },
})

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB 限制
  // 先尽量放宽类型限制，避免前端上传时报错导致返回 HTML
})

// 中间件
app.use(cors()) // 允许跨域（前端调用需要）
app.use(express.json()) // 解析 JSON 请求体

/**
 * AI 聊天接口
 *
 * 请求格式：
 * POST /api/ai/chat
 * Body: { "messages": [{ "role": "user", "content": "你好" }, ...] }
 *
 * 返回格式：
 * { "content": "AI 的回复内容" }
 */
app.post('/api/ai/chat', async (req, res) => {
  try {
    const { messages } = req.body
    if (!messages || !Array.isArray(messages)) {
      return res.status(400).json({ error: '请求格式错误：需要 messages 数组' })
    }
    console.log(`📨 收到请求，消息数量: ${messages.length}`)

    // 调用 DeepSeek API
    const response = await fetch('https://api.deepseek.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${DEEPSEEK_API_KEY}`,
        'Content-Type': 'application/json; charset=utf-8',
        Accept: 'application/json',
      },
      body: Buffer.from(
        JSON.stringify({
          model: 'deepseek-chat',
          messages,
          max_tokens: 4096,
          temperature: 0.7,
        }),
        'utf8'
      ),
    })

    if (!response.ok) {
      const errorText = await response.text()
      console.error(`❌ DeepSeek API 错误: ${response.status}`, errorText)
      return res.status(response.status).json({
        error: `DeepSeek API 错误: ${response.status}`,
        detail: errorText,
      })
    }

    const data = await response.json()
    const content = data.choices?.[0]?.message?.content
    if (!content) {
      console.error('❌ DeepSeek 返回格式异常:', data)
      return res.status(500).json({ error: 'AI 返回格式异常' })
    }

    console.log(`✅ AI 回复成功，长度: ${content.length} 字符`)
    // 返回给前端
    res.json({ content })
  } catch (error) {
    console.error('❌ 服务器错误:', error.message)
    if (error?.stack) {
      console.error(error.stack)
    }
    res.status(500).json({ error: '服务器内部错误' })
  }
})

// 健康检查接口
app.get('/api/health', (req, res) => {
  res.json({ status: 'ok', message: 'AI 小精灵后端服务运行中' })
})

/**
 * 读取文件内容（支持 txt、md、pdf，扩展支持 ppt/pptx）
 */
async function readFileContent(filePath, mimetype) {
  try {
    if (mimetype === 'text/plain' || mimetype === 'text/markdown' || filePath.endsWith('.md')) {
      return fs.readFileSync(filePath, 'utf-8')
    }
    if (mimetype === 'application/pdf') {
      // 动态导入 pdf-parse
      const pdfParse = (await import('pdf-parse')).default
      const dataBuffer = fs.readFileSync(filePath)
      const data = await pdfParse(dataBuffer)
      return data.text
    }
    // PPT / PPTX：尝试解析文本内容（需要在 server 目录安装 pptx-parser 或类似库）
    if (
      filePath.endsWith('.ppt') ||
      filePath.endsWith('.pptx') ||
      mimetype.includes('presentation')
    ) {
      try {
        // 这里使用动态导入的方式，避免在未安装依赖时导致整个服务崩溃
        // 建议在 server 目录执行：npm install pptx-parser
        const pptxParser = (await import('pptx-parser')).default
        const result = await pptxParser(filePath)
        // 不同库的返回结构可能不同，这里做一个尽量安全的提取
        const texts = []
        if (Array.isArray(result)) {
          // 形如 [{text: '...'}, ...]
          result.forEach((item) => {
            if (item && typeof item.text === 'string') {
              texts.push(item.text)
            }
          })
        } else if (result && Array.isArray(result.slides)) {
          result.slides.forEach((slide) => {
            if (Array.isArray(slide.texts)) {
              slide.texts.forEach((t) => {
                if (t && typeof t === 'string') texts.push(t)
              })
            }
          })
        }
        const joined = texts.join('\n').trim()
        if (joined) return joined
      } catch (e) {
        console.error('PPT 解析失败:', e.message)
        // 解析失败时继续走下面逻辑，最终会提示用户转换格式
      }
      return '[PPT 文件暂未能正确解析，请优先尝试将 PPT 导出为 PDF 或 TXT 上传。]'
    }
    // 对于图片，返回提示信息（后续可以接入 OCR）
    if (mimetype.startsWith('image/')) {
      return '[图片文件，暂不支持自动提取文字，请手动输入关键内容]'
    }
    // doc/docx 暂时返回提示
    if (mimetype.includes('word') || mimetype.includes('document')) {
      return '[Word 文件，建议转为 PDF 或 TXT 格式上传]'
    }
    return ''
  } catch (err) {
    console.error('读取文件失败:', err.message)
    return ''
  }
}

/**
 * 文件上传 + AI 生成笔记接口
 *
 * 请求格式：
 * POST /api/ai/generate-note
 * Content-Type: multipart/form-data
 * - files: 上传的文件（支持多个）
 * - noteName: 笔记名称
 * - subjectName: 科目名称
 *
 * 返回格式：
 * { "note": { "title": "...", "content": "..." } }
 */
app.post('/api/ai/generate-note', upload.array('files', 10), async (req, res) => {
  try {
    const files = req.files || []
    const { noteName, subjectName } = req.body

    if (files.length === 0) {
      return res.status(400).json({ error: '请上传至少一个文件' })
    }

    console.log(`📁 收到 ${files.length} 个文件，准备生成笔记...`)

    // 读取所有文件内容
    let allContent = ''
    for (const file of files) {
      const content = await readFileContent(file.path, file.mimetype)
      if (content) {
        const originalName = Buffer.from(file.originalname, 'latin1').toString('utf8')
        allContent += `\n\n--- 文件: ${originalName} ---\n${content}`
      }
      // 删除临时文件
      try {
        fs.unlinkSync(file.path)
      } catch (e) {
        // ignore
      }
    }

    if (!allContent.trim()) {
      return res.status(400).json({ error: '无法读取文件内容，请检查文件格式' })
    }

    // 构建 AI 提示词，生成结构化笔记（更详细，但仍尽量写完）
    const systemPrompt = `You are a professional study assistant specializing in creating high-quality, **detailed and complete** study notes.

Your task is to transform the user's study materials into **well-structured, exam-ready notes**.

## CRITICAL REQUIREMENTS:

### 1. COMPLETENESS (most important)
- **Include ALL key concepts, definitions, theorems and main conclusions** from the source material.
- If the source covers multiple topics, cover ALL of them.
- Include important dates, names and formulas when they are essential.

### 2. LEVEL OF DETAIL
- For each important concept, use **3–6 sentences** to explain:
  - the definition / idea,
  - why it matters,
  - how it is used (briefly).
- For core theorems / principles, you may add:
  - short intuition,
  - typical application scenario,
  - one simple example if space allows.
- Avoid long derivations or very lengthy examples, but make sure the student can understand and review from these notes alone.

### 3. STRUCTURE
- Use clear Markdown hierarchy: # for main title, ## for sections, ### for subsections.
- Group related points under the same subsection, and use numbered / bullet lists to organize them.
- When the chapter is long, prefer more sections + subsections instead of huge paragraphs.

### 4. HIGHLIGHTING & REVIEW
- Use **bold** for key terms, definitions and exam-critical conclusions.
- Use simple tables when it helps compare or contrast concepts.
- Add a short **\"Key Takeaways\"** section at the end that lists 5–10 bullet points summarizing the chapter.

### 5. FORMAT
- Write in the same language as the source material.
- Use Markdown format only.
- Do NOT add any meta comments about how you created the notes.
- Start directly with the content.`

    const userPrompt = `Please create detailed and complete study notes from the following materials:

Subject: ${subjectName || 'General'}
Topic: ${noteName || 'Study Notes'}

=== SOURCE MATERIALS ===
${allContent}
=== END OF MATERIALS ===

Make sure all important concepts are included, and explain each important point clearly (around 3–6 sentences), so that a student can review and understand the topic using these notes alone.`

    console.log('🤖 正在调用 AI 生成笔记...')

    // 调用 DeepSeek API 生成笔记
    const response = await fetch('https://api.deepseek.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${DEEPSEEK_API_KEY}`,
        'Content-Type': 'application/json; charset=utf-8',
        Accept: 'application/json',
      },
      body: Buffer.from(
        JSON.stringify({
          model: 'deepseek-chat',
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt },
          ],
          max_tokens: 8192,
          temperature: 0.3, // 低温度，输出更稳定
        }),
        'utf8'
      ),
    })

    if (!response.ok) {
      const errorText = await response.text()
      console.error(`❌ DeepSeek API 错误: ${response.status}`, errorText)
      return res.status(response.status).json({
        error: `AI 生成失败: ${response.status}`,
        detail: errorText,
      })
    }

    const data = await response.json()
    const noteContent = data.choices?.[0]?.message?.content

    if (!noteContent) {
      console.error('❌ DeepSeek 返回格式异常:', data)
      return res.status(500).json({ error: 'AI 返回格式异常' })
    }

    console.log(`✅ 笔记生成成功，长度: ${noteContent.length} 字符`)

    // 返回生成的笔记
    res.json({
      note: {
        title: noteName || '学习笔记',
        content: noteContent,
        subjectName: subjectName || '',
        createdAt: new Date().toISOString(),
      },
    })
  } catch (error) {
    console.error('❌ 生成笔记失败:', error.message)
    if (error?.stack) {
      console.error(error.stack)
    }
    res.status(500).json({ error: '生成笔记失败：' + error.message })
  }
})

/**
 * AI 生成测试题接口
 *
 * 请求格式：
 * POST /api/ai/generate-exam
 * Body: {
 *   noteContent: string,      // 笔记内容（Markdown 或纯文本）
 *   questionCount: number,    // 期望题目数量
 *   difficulty: 'easy' | 'medium' | 'hard'
 * }
 *
 * 返回格式：
 * { questions: [ { id, type, stem, options?, answer, explanation?, points } ] }
 */
app.post('/api/ai/generate-exam', async (req, res) => {
  try {
    const {
      noteContent,
      questionCount = 8,
      difficulty = 'medium',
      // 由前端传入的期望题型（仅作为提示，不改变输出 JSON 结构）
      selectedTypes = ['single', 'true-false', 'open'],
    } = req.body || {}

    if (!noteContent || typeof noteContent !== 'string') {
      return res.status(400).json({ error: 'noteContent is required' })
    }

    console.log(`📝 生成试卷，请求题目数量: ${questionCount}, 难度: ${difficulty}`)

    const systemPrompt = `You are an experienced exam designer. Based on the given study notes, 
you will create a high-quality exam that checks real understanding.

You MUST output **valid JSON only**, no explanation text, using this exact TypeScript-like schema:

{
  "questions": Array<{
    "id": string;
    "type": "single" | "true-false" | "open";
    "stem": string;
    "options"?: Array<{ "value": string; "label": string; "text": string }>;
    "answer": string;
    "explanation": string;
    "points": number;
  }>
}

## CRITICAL REQUIREMENTS:

### Question Types:
1. **single** (single choice): Must have 4 options (A, B, C, D). Each option has { "value": "A", "label": "A", "text": "option content" }.
2. **true-false**: The "answer" must be exactly "True" or "False". Do NOT include options array for true-false questions (the frontend will generate them).
3. **open**: No options needed, just stem and answer.

### Explanation Quality (VERY IMPORTANT):
Each explanation must be **detailed and educational** (4-6 sentences), including:
- WHY the correct answer is correct (with reasoning or evidence from the notes)
- WHY the wrong options are incorrect (for single choice questions)
- Any key concept or background knowledge that helps understand the answer
- A brief tip for remembering or applying this knowledge

### Other Requirements:
- Cover ALL the main concepts in the notes, not just the first part.
- Mix question types: about 50% single choice, 30% true-false, 20% open questions.
- For single choice questions, make distractors plausible but clearly wrong.
- Points should be between 2 and 8 depending on difficulty and question type.
- Difficulty level hint:
  - easy: more direct recall questions.
  - medium: mix of recall and understanding.
  - hard: more application or multi-step reasoning.`

    const userPrompt = `Please create an exam from the following study notes.

Question count: about ${questionCount}
Difficulty: ${difficulty}
Preferred question types (hint from frontend, you MUST still follow the JSON schema above):
- Selected types: ${Array.isArray(selectedTypes) ? selectedTypes.join(', ') : String(selectedTypes)}

Please:
- Prioritize the selected types when distributing questions.
- If a type is not included in the selected list, avoid generating that type unless absolutely necessary to cover key concepts.

=== STUDY NOTES START ===
${noteContent}
=== STUDY NOTES END ===

Return ONLY JSON following the schema above. Do NOT wrap it in markdown or add any extra text.`

    console.log('🤖 正在调用 AI 生成试卷...')

    const response = await fetch('https://api.deepseek.com/v1/chat/completions', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${DEEPSEEK_API_KEY}`,
        'Content-Type': 'application/json; charset=utf-8',
        Accept: 'application/json',
      },
      body: Buffer.from(
        JSON.stringify({
          model: 'deepseek-chat',
          messages: [
            { role: 'system', content: systemPrompt },
            { role: 'user', content: userPrompt },
          ],
          max_tokens: 4096,
          temperature: 0.4,
        }),
        'utf8'
      ),
    })

    if (!response.ok) {
      const errorText = await response.text()
      console.error(`❌ DeepSeek 生成试卷错误: ${response.status}`, errorText)
      return res.status(response.status).json({
        error: `AI 生成试卷失败: ${response.status}`,
        detail: errorText,
      })
    }

    const data = await response.json()
    let raw = data.choices?.[0]?.message?.content

    if (!raw) {
      console.error('❌ DeepSeek 返回格式为空:', data)
      return res.status(500).json({ error: 'AI 返回格式异常（空内容）' })
    }

    let parsed
    try {
      // 有些情况下模型会包一层 ```json ```，或在前后加说明文字，这里做一次“提纯”
      let cleaned = String(raw).trim()
      // 去掉 markdown 代码块包裹
      if (cleaned.startsWith('```')) {
        cleaned = cleaned.replace(/^```[a-zA-Z]*\s*/, '').replace(/```$/, '').trim()
      }
      // 只截取第一个 { 到 最后一个 } 之间的内容，防止前后有额外文字
      const firstBrace = cleaned.indexOf('{')
      const lastBrace = cleaned.lastIndexOf('}')
      if (firstBrace !== -1 && lastBrace !== -1 && lastBrace > firstBrace) {
        cleaned = cleaned.slice(firstBrace, lastBrace + 1)
      }
      parsed = JSON.parse(cleaned)
    } catch (e) {
      console.error('❌ 解析 AI 返回的 JSON 失败:', e.message)
      console.error('原始内容:', raw.slice(0, 300))
      return res.status(500).json({ error: 'AI 返回内容不是合法 JSON，请稍后重试' })
    }

    if (!parsed.questions || !Array.isArray(parsed.questions) || !parsed.questions.length) {
      return res.status(500).json({ error: 'AI 未返回任何题目，请尝试缩短笔记或减少题目数量' })
    }

    console.log(`✅ 试卷生成成功，题目数量: ${parsed.questions.length}`)

    res.json({ questions: parsed.questions })
  } catch (error) {
    console.error('❌ 生成试卷失败:', error.message)
    if (error?.stack) {
      console.error(error.stack)
    }
    res.status(500).json({ error: '生成试卷失败：' + error.message })
  }
})

// 统一错误处理中间件（确保返回 JSON，而不是 HTML）
app.use((err, req, res, next) => {
  console.error('全局错误捕获:', err.message)
  res.status(500).json({ error: err.message || '服务器内部错误' })
})

// 启动服务器
app.listen(PORT, () => {
  console.log('')
  console.log('🤖 AI 小精灵后端服务已启动')
  console.log(`📍 地址: http://localhost:${PORT}`)
  console.log(`🔗 AI 接口: http://localhost:${PORT}/api/ai/chat`)
  console.log('')
  console.log('✅ API Key 已从环境变量加载（安全）')
  console.log('')
})

