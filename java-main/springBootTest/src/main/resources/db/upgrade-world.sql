-- ============================================================
--  雾山五行 · 山门世界玩法（可重复执行）
--  背包 / 秘境探索 / 炼丹炼器 / 五行问对决 / 传书私信
-- ============================================================
USE wushan;

-- ------------------------------------------------------------
-- 1) 背包：秘境掉落、炼丹消耗都靠它
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_user_bag (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id     BIGINT NOT NULL,
  item_id     BIGINT NOT NULL,
  count       INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_item (user_id, item_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='山友背包';

-- ------------------------------------------------------------
-- 2) 秘境探索记录
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_explore_log (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id      BIGINT NOT NULL,
  realm_id     BIGINT NOT NULL,
  realm_name   VARCHAR(64) DEFAULT '',
  path_json    VARCHAR(1024) DEFAULT '' COMMENT '经历的事件路径',
  rewards_json VARCHAR(512) DEFAULT '' COMMENT '所得物品与灵石',
  cost_spirit  INT NOT NULL DEFAULT 0,
  create_time  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秘境探索记录';

-- ------------------------------------------------------------
-- 3) 炼丹炼器配方
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_recipe (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  name            VARCHAR(64) NOT NULL,
  category        VARCHAR(16) NOT NULL DEFAULT 'danyao' COMMENT 'danyao 丹药 / faqi 炼器',
  output_item_id  BIGINT NOT NULL,
  output_count    INT NOT NULL DEFAULT 1,
  cost_spirit     INT NOT NULL DEFAULT 0,
  materials_json  VARCHAR(512) NOT NULL DEFAULT '[]' COMMENT '[{"itemId":6,"count":1}]',
  description     VARCHAR(512) DEFAULT '',
  status          TINYINT NOT NULL DEFAULT 1,
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='炼丹炼器配方';

-- 配方种子：材料取炼材条目，产物取丹药 / 法器条目
INSERT INTO wushan.wushan_recipe (name, category, output_item_id, output_count, cost_spirit, materials_json, description)
SELECT '回元丹', 'danyao', 112, 1, 20, '[{"itemId":6,"count":1},{"itemId":95,"count":1}]', '以地心火髓佐净水砂囊，寻常修行者亦可服用。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM wushan.wushan_recipe WHERE name='回元丹');

INSERT INTO wushan.wushan_recipe (name, category, output_item_id, output_count, cost_spirit, materials_json, description)
SELECT '凝神丹', 'danyao', 113, 1, 35, '[{"itemId":15,"count":1},{"itemId":71,"count":1}]', '白玉髓与圆转石相融，服之神魂凝定。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM wushan.wushan_recipe WHERE name='凝神丹');

INSERT INTO wushan.wushan_recipe (name, category, output_item_id, output_count, cost_spirit, materials_json, description)
SELECT '破障丹', 'danyao', 114, 1, 60, '[{"itemId":54,"count":1},{"itemId":56,"count":1}]', '雷击木心与引雷针共冶，可破修行关隘。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM wushan.wushan_recipe WHERE name='破障丹');

INSERT INTO wushan.wushan_recipe (name, category, output_item_id, output_count, cost_spirit, materials_json, description)
SELECT '洗髓丹', 'danyao', 115, 1, 90, '[{"itemId":70,"count":1},{"itemId":72,"count":1}]', '太极玉璧入炉，两仪砂点睛，洗髓伐毛。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM wushan.wushan_recipe WHERE name='洗髓丹');

INSERT INTO wushan.wushan_recipe (name, category, output_item_id, output_count, cost_spirit, materials_json, description)
SELECT '追风扇', 'faqi', 97, 1, 50, '[{"itemId":46,"count":2}]', '风灵羽双片入扇，御风而行。'
FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM wushan.wushan_recipe WHERE name='追风扇');

-- ------------------------------------------------------------
-- 4) 五行问对决
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_duel (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  challenger_id   BIGINT NOT NULL,
  challenger_name VARCHAR(32) NOT NULL,
  opponent_id     BIGINT NOT NULL,
  opponent_name   VARCHAR(32) NOT NULL,
  status          TINYINT NOT NULL DEFAULT 0 COMMENT '0=进行中 1=已结束',
  question_json   VARCHAR(2048) DEFAULT '' COMMENT '题库快照',
  my_score        INT NOT NULL DEFAULT 0,
  op_score        INT NOT NULL DEFAULT 0,
  result          VARCHAR(16) DEFAULT '' COMMENT 'win / lose / draw',
  reward_spirit   INT NOT NULL DEFAULT 0,
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_challenger (challenger_id),
  KEY idx_opponent (opponent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='五行问对决';

-- ------------------------------------------------------------
-- 5) 传书私信
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_message (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  from_id     BIGINT NOT NULL,
  from_name   VARCHAR(32) NOT NULL,
  to_id       BIGINT NOT NULL,
  to_name     VARCHAR(32) NOT NULL,
  content     VARCHAR(512) NOT NULL,
  is_read     TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_to (to_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='传书私信';

-- ------------------------------------------------------------
-- 6) 导航入口
-- ------------------------------------------------------------
INSERT INTO wushan.wushan_nav (nav_key, label, en, sort, status)
SELECT 'mijing', '秘境', 'realm', 7, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan.wushan_nav WHERE nav_key='mijing');

INSERT INTO wushan.wushan_nav (nav_key, label, en, sort, status)
SELECT 'liandan', '炼丹', 'alchemy', 8, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan.wushan_nav WHERE nav_key='liandan');

INSERT INTO wushan.wushan_nav (nav_key, label, en, sort, status)
SELECT 'duijue', '对决', 'duel', 9, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan.wushan_nav WHERE nav_key='duijue');

INSERT INTO wushan.wushan_nav (nav_key, label, en, sort, status)
SELECT 'chuanshu', '传书', 'letters', 10, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan.wushan_nav WHERE nav_key='chuanshu');
