-- ============================================================
--  雾山五行 · 认证库 建库建表语句
--  数据库：wushan
--  字符集：utf8mb4（支持完整 Unicode / 表情）
--  说明：本文件为「文本留存」用的建库建表脚本，
--        已由后端在本地 MySQL（root/123456）执行建库建表。
--        如需在其它环境重建，直接执行本文件即可。
--
--  2026-09-23 调整：注册页改为「名号 + 暗语 + 图形符印」，
--        取消邮箱 / 手机号与六爻密印（短信邮箱验证码），
--        故移除 account、account_type 两列及 uk_account 唯一键。
--        老库升级见文件末尾的 ALTER 语句。
-- ============================================================

-- 1) 建库（若不存在）
CREATE DATABASE IF NOT EXISTS wushan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

-- 2) 使用库
USE wushan;

-- 3) 建表：山籍（用户表）
DROP TABLE IF EXISTS wushan_user;

CREATE TABLE wushan_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(32)  NOT NULL                COMMENT '名号（登录名 / 昵称）',
  password      VARCHAR(100) NOT NULL                COMMENT '暗语（BCrypt 密文存储）',
  element       VARCHAR(8)   DEFAULT NULL            COMMENT '本命五行 key：jin/mu/shui/huo/tu',
  element_label VARCHAR(8)   DEFAULT NULL            COMMENT '五行 label：金/木/水/火/土',
  agreed        TINYINT(1)   DEFAULT 0               COMMENT '是否允诺山规：1=是 0=否',
  agreed_at     DATETIME     DEFAULT NULL            COMMENT '允诺时间',
  status        TINYINT      DEFAULT 1               COMMENT '状态：1=正常 0=禁用',
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP                COMMENT '创建时间',
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='雾山山籍（用户表）';

-- ============================================================
--  附：老库在线升级（已建过旧表的环境执行这两行即可，无需重建）
-- ============================================================
-- ALTER TABLE wushan_user DROP INDEX uk_account;
-- ALTER TABLE wushan_user DROP COLUMN account, DROP COLUMN account_type;
