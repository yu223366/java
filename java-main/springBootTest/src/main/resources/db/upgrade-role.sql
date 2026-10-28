-- ============================================================
--  升级：wushan_user 新增权限字段 role
--  0 = 管理员（执事，可登录后台管理系统）
--  1 = 普通山友（注册默认，无后台入口、无法登录后台）
--  启动脚本（启动-后端.cmd）探测到缺列时自动执行本脚本
-- ============================================================
USE wushan;

-- 1) 新增权限字段（默认值 1：普通山友；历史数据一律视为普通山友）
ALTER TABLE wushan_user
  ADD COLUMN role TINYINT NOT NULL DEFAULT 1 COMMENT '权限：0=管理员(执事) 1=普通山友' AFTER status;

-- 2) 旧库密码为 BCrypt 密文，与明文比对不兼容且无法还原，
--    统一重置为明文 wushan123（暗语规则：8~20 位，兼含字母与数字）
UPDATE wushan_user SET password = 'wushan123' WHERE password LIKE '$2a$%' OR password LIKE '$2b$%' OR password LIKE '$2y$%';

-- 3) 内置执事账号：admin / admin123（role=0）
--    uk_username 唯一键保证幂等：已存在则忽略
INSERT IGNORE INTO wushan_user (username, password, element, element_label, agreed, agreed_at, status, role)
VALUES ('admin', 'admin123', 'huo', '火', 1, NOW(), 1, 0);

-- 4) 提升其他山友为执事（示例，按需手工执行）：
-- UPDATE wushan_user SET role = 0 WHERE username = '名号';
