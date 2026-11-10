-- ============================================================
--  雾山五行 · 拍卖行增强（可重复执行）
--  关注拍品：开拍时提醒关注者捡漏
-- ============================================================
USE wushan;

CREATE TABLE IF NOT EXISTS wushan_auction_watch (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  item_title  VARCHAR(128) NOT NULL COMMENT '关注的拍品名',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id),
  KEY idx_title (item_title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖关注';
