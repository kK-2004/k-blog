-- 修复 admin_users 表的时间字段类型
-- 将 last_login_at 从 double/decimal 改为 datetime(3)
ALTER TABLE admin_users MODIFY COLUMN last_login_at datetime(3) DEFAULT NULL;

-- 同步修复 created_at 和 updated_at（确保它们也是正确的类型）
ALTER TABLE admin_users MODIFY COLUMN created_at datetime(3) NOT NULL;
ALTER TABLE admin_users MODIFY COLUMN updated_at datetime(3) NOT NULL;

-- 清空旧的错误格式的数据（可选）
UPDATE admin_users SET last_login_at = NULL WHERE last_login_at IS NOT NULL;
