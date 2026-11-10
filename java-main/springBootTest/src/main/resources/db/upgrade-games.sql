-- ============================================================
--  雾山五行 · 山门玩法升级（可重复执行）
--  1) 灵石账户：wushan_user.spirit + wushan_spirit_log 流水
--  2) 测灵根：wushan_spirit_profile 测评记录
--  3) 每日签到：wushan_sign_in
--  4) 每日一题：wushan_daily_question / wushan_daily_answer
-- ============================================================
USE wushan;

-- ------------------------------------------------------------
-- 1) 灵石余额：此前项目只有「灵石」这个数字，没有账户
-- ------------------------------------------------------------
SET @ddl = (
  SELECT IF(COUNT(*) = 0,
    'ALTER TABLE wushan_user ADD COLUMN spirit INT NOT NULL DEFAULT 1000 COMMENT ''灵石余额''',
    'SELECT 1')
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = 'wushan' AND TABLE_NAME = 'wushan_user' AND COLUMN_NAME = 'spirit'
);
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 灵石流水：签到 / 答题 / 拍卖 / 市集 的收支都记一笔
CREATE TABLE IF NOT EXISTS wushan_spirit_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  amount      INT NOT NULL COMMENT '正数为收入，负数为支出',
  balance     INT NOT NULL DEFAULT 0 COMMENT '变动后余额',
  reason      VARCHAR(64) NOT NULL DEFAULT '' COMMENT '事由',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵石流水';

-- ------------------------------------------------------------
-- 2) 测灵根：一次测评一条记录
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_spirit_profile (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  root_name   VARCHAR(64) NOT NULL COMMENT '灵根名',
  element     VARCHAR(16) NOT NULL COMMENT '五行',
  grade_key   VARCHAR(16) NOT NULL COMMENT '灵根品阶',
  rare_level  TINYINT NOT NULL DEFAULT 1 COMMENT '稀有度 1普通 2罕见 3天品 4异变',
  score       INT NOT NULL DEFAULT 0 COMMENT '契合得分',
  summary     VARCHAR(512) DEFAULT '' COMMENT '灵根说明',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='灵根测评';

-- ------------------------------------------------------------
-- 3) 每日签到
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_sign_in (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id         BIGINT NOT NULL,
  sign_date       DATE NOT NULL,
  continuous_days INT NOT NULL DEFAULT 1 COMMENT '连续签到天数',
  reward_spirit   INT NOT NULL DEFAULT 0 COMMENT '本次所得灵石',
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_date (user_id, sign_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日签到';

-- ------------------------------------------------------------
-- 4) 每日一题：当天题目落库，保证所有山友同题
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_daily_question (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  question_date DATE NOT NULL,
  question      VARCHAR(255) NOT NULL,
  options_json  VARCHAR(1024) NOT NULL COMMENT '选项 JSON 数组',
  answer_idx    TINYINT NOT NULL,
  reward_spirit INT NOT NULL DEFAULT 20,
  create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_date (question_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日一题';

CREATE TABLE IF NOT EXISTS wushan_daily_answer (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id       BIGINT NOT NULL,
  question_date DATE NOT NULL,
  answer_idx    TINYINT NOT NULL,
  is_correct    TINYINT NOT NULL DEFAULT 0,
  reward_spirit INT NOT NULL DEFAULT 0,
  create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_date (user_id, question_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日一题作答';

-- ------------------------------------------------------------
-- 5) 导航入口：修行洞府（测灵根 / 签到 / 每日一题）
-- ------------------------------------------------------------
INSERT INTO wushan.wushan_nav (nav_key, label, en, sort, status)
SELECT 'xiuxing', '修行', 'cultivation', 6, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan.wushan_nav WHERE nav_key = 'xiuxing');
