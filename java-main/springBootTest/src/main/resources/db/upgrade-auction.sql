-- ============================================================
--  雾山五行 · 拍卖行数据库升级（可重复执行）
--  1) 给物品表增加拍卖字段：0=未拍卖（默认） 1=已拍卖
--  2) 场次 / 出价流水 / 成交 / 在线用户 / 开拍通知 五张表
--  执行：mysql -uroot -p --default-character-set=utf8mb4 < upgrade-auction.sql
-- ============================================================
USE wushan;

-- ------------------------------------------------------------
-- 1) 宝库条目（功法 / 法宝 / 丹药都在 wushan_item）
--    实体 Item.java 已有 auction 字段，这里把列补上
-- ------------------------------------------------------------
SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE wushan_item ADD COLUMN auction TINYINT NOT NULL DEFAULT 0 COMMENT ''拍卖状态：0=未拍卖 1=已拍卖''',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'wushan' AND TABLE_NAME = 'wushan_item' AND COLUMN_NAME = 'auction'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 市集物品同样带拍卖字段（供后续把市集拍品纳入拍卖池）
SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE wushan_market_item ADD COLUMN auction TINYINT NOT NULL DEFAULT 0 COMMENT ''拍卖状态：0=未拍卖 1=已拍卖''',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'wushan' AND TABLE_NAME = 'wushan_market_item' AND COLUMN_NAME = 'auction'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 2) 拍卖场次：每 10 分钟一场
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_auction_round (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  round_no    INT NOT NULL COMMENT '场次号（递增）',
  status      TINYINT NOT NULL DEFAULT 0 COMMENT '0=未开始 1=进行中 2=已结束',
  item_count  INT NOT NULL DEFAULT 0 COMMENT '本场拍品件数',
  start_time  DATETIME NULL,
  end_time    DATETIME NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_round_no (round_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖场次';

-- ------------------------------------------------------------
-- 3) 出价流水：谁、加了多少、加价后总价
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_auction_bid (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  round_no         INT NOT NULL COMMENT '场次号',
  item_id          BIGINT NOT NULL COMMENT '拍品 id',
  item_title       VARCHAR(128) DEFAULT '' COMMENT '拍品名（冗余便于展示）',
  user_id          BIGINT NOT NULL,
  username         VARCHAR(32) NOT NULL COMMENT '出价人名号',
  increment_amount INT NOT NULL COMMENT '本次加价额：10/50/100/500',
  price            INT NOT NULL COMMENT '加价后的当前价',
  bid_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_round (round_no),
  KEY idx_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖出价流水';

-- ------------------------------------------------------------
-- 4) 成交记录：落槌归出价最高者
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_auction_result (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  round_no     INT NOT NULL,
  item_id      BIGINT NOT NULL,
  item_title   VARCHAR(128) DEFAULT '',
  winner_id    BIGINT NULL COMMENT '流拍时为 NULL',
  winner_name  VARCHAR(32) DEFAULT '' COMMENT '流拍时为空',
  final_price  INT NOT NULL DEFAULT 0,
  hammer_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_round (round_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖成交记录';

-- ------------------------------------------------------------
-- 5) 在线用户：只有注册且近期活跃的用户可参与 / 收到通知
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_online_user (
  user_id          BIGINT PRIMARY KEY,
  username         VARCHAR(32) NOT NULL,
  last_active_time DATETIME NOT NULL,
  KEY idx_active (last_active_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖行在线用户';

-- ------------------------------------------------------------
-- 6) 开拍通知：提前 1 分钟写给所有在线用户
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_auction_notice (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  round_no     INT NOT NULL,
  content      VARCHAR(255) NOT NULL,
  notify_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_round (round_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='拍卖开拍通知';
