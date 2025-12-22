-- 为 questions 表添加 options 和 explanation 字段
-- 使用方法：在 MySQL 客户端中执行此 SQL 脚本

-- 检查字段是否存在，如果不存在则添加（MySQL 8.0+）
-- 如果使用 MySQL 5.7，请使用下面的简单版本

-- MySQL 8.0+ 版本（推荐）
SET @dbname = DATABASE();
SET @tablename = 'questions';
SET @columnname1 = 'options';
SET @columnname2 = 'explanation';
SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname1)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname1, ' TEXT COMMENT ''JSON格式存储选项''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

SET @preparedStatement = (SELECT IF(
  (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE
      (TABLE_SCHEMA = @dbname)
      AND (TABLE_NAME = @tablename)
      AND (COLUMN_NAME = @columnname2)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname2, ' TEXT COMMENT ''题目解析''')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- MySQL 5.7 简单版本（如果上面的复杂版本不工作，使用这个）
-- ALTER TABLE questions 
-- ADD COLUMN options TEXT COMMENT 'JSON格式存储选项',
-- ADD COLUMN explanation TEXT COMMENT '题目解析';

