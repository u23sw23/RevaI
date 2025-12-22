-- 为已存在的 subjects 表添加 visibility 字段
-- 如果表已存在，执行此脚本添加字段

USE demo2;

ALTER TABLE subjects 
ADD COLUMN IF NOT EXISTS visibility VARCHAR(10) NOT NULL DEFAULT 'private' AFTER user_id;

-- 为 visibility 字段添加索引
CREATE INDEX IF NOT EXISTS idx_visibility ON subjects(visibility);

