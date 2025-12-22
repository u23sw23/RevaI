-- 为 questions 表添加 options 和 explanation 字段
-- 如果字段已存在，会报错但不会影响其他操作

ALTER TABLE questions 
ADD COLUMN IF NOT EXISTS options TEXT COMMENT 'JSON格式存储选项：[{"value":"A","label":"A","text":"选项内容"},...]',
ADD COLUMN IF NOT EXISTS explanation TEXT COMMENT '题目解析';

