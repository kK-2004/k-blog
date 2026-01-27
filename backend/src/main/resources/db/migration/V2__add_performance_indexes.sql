-- =============================================
-- 性能优化索引
-- 创建日期: 2026-01-27
-- 说明: 为常用查询添加索引，提高查询性能
-- =============================================

-- =============================================
-- 1. 文章表索引 (posts)
-- =============================================

-- 按创建时间降序查询（用于文章列表）
CREATE INDEX idx_posts_created_at ON posts(created_at DESC);

-- 按作者查询（用于获取某用户的文章列表）
CREATE INDEX idx_posts_author ON posts(author);

-- 组合索引：按作者和创建时间查询（用于获取某用户的文章列表按时间排序）
CREATE INDEX idx_posts_author_created ON posts(author, created_at DESC);

-- =============================================
-- 2. 评论表索引 (post_comments)
-- =============================================

-- 按文章 ID 查询评论（最常用的查询）
CREATE INDEX idx_comments_post_id ON post_comments(post_id);

-- 按父评论 ID 查询回复（用于展示嵌套评论）
CREATE INDEX idx_comments_parent_id ON post_comments(parent_id);

-- 组合索引：按文章和创建时间查询（用于评论列表按时间排序）
CREATE INDEX idx_comments_post_created ON post_comments(post_id, created_at ASC);

-- =============================================
-- 索引验证查询
-- =============================================

-- 查看表的索引情况
-- SHOW INDEX FROM posts;
-- SHOW INDEX FROM post_comments;
-- SHOW INDEX FROM admin_users;

-- 分析查询执行计划
-- EXPLAIN SELECT * FROM posts ORDER BY created_at DESC LIMIT 10;
-- EXPLAIN SELECT * FROM post_comments WHERE post_id = 1 ORDER BY created_at ASC;
