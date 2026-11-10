-- ============================================================
--  雾山五行 · 辩场 + 求购挂单（可重复执行）
--  1) wushan_discourse 增加辩论字段：topic 主题 / side 立场
--  2) wushan_wanted 求购挂单
-- ============================================================
USE wushan;

-- ------------------------------------------------------------
-- 1) 辩论：普通语录 topic 为空、side=0；辩论语录按 topic 分正反两方
-- ------------------------------------------------------------
SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE wushan_discourse ADD COLUMN topic VARCHAR(64) DEFAULT NULL COMMENT ''辩论主题（为空即普通论道）''',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'wushan' AND TABLE_NAME = 'wushan_discourse' AND COLUMN_NAME = 'topic'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE wushan_discourse ADD COLUMN side TINYINT NOT NULL DEFAULT 0 COMMENT ''立场：0=正方 1=反方''',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'wushan' AND TABLE_NAME = 'wushan_discourse' AND COLUMN_NAME = 'side'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2) 求购挂单：山友挂出想收的东西，附最高愿付灵石
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_wanted (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  username    VARCHAR(32) NOT NULL COMMENT '求购人名号',
  title       VARCHAR(128) NOT NULL COMMENT '想求购之物',
  max_price   INT NOT NULL DEFAULT 0 COMMENT '愿付最高灵石',
  note        VARCHAR(255) DEFAULT '' COMMENT '备注',
  status      TINYINT NOT NULL DEFAULT 0 COMMENT '0=求购中 1=已成交 2=已撤销',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_status (status),
  KEY idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='求购挂单';
