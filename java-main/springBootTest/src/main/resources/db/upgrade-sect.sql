-- ============================================================
--  雾山五行 · 宗门（可重复执行）
-- ============================================================
USE wushan;

CREATE TABLE IF NOT EXISTS wushan_sect (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(32) NOT NULL COMMENT '宗门名',
  element     VARCHAR(16) NOT NULL DEFAULT 'huo' COMMENT '宗门五行',
  leader_id   BIGINT NOT NULL COMMENT '宗主 id',
  leader_name VARCHAR(32) NOT NULL COMMENT '宗主名号',
  notice      VARCHAR(255) DEFAULT '' COMMENT '宗门公告',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宗门';

CREATE TABLE IF NOT EXISTS wushan_sect_member (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  sect_id      BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  username     VARCHAR(32) NOT NULL,
  role         TINYINT NOT NULL DEFAULT 0 COMMENT '0=弟子 1=长老 2=宗主',
  contribution INT NOT NULL DEFAULT 0 COMMENT '贡献值',
  join_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sect_user (sect_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宗门成员';

INSERT INTO wushan.wushan_nav (nav_key, label, en, sort, status)
SELECT 'zongmen', '宗门', 'sect', 11, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan.wushan_nav WHERE nav_key='zongmen');
