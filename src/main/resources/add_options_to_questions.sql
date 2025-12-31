

ALTER TABLE questions 
ADD COLUMN IF NOT EXISTS options TEXT COMMENT 'JSON格式存储选项：[{"value":"A","label":"A","text":"选项内容"},...]',
ADD COLUMN IF NOT EXISTS explanation TEXT COMMENT '题目解析';

