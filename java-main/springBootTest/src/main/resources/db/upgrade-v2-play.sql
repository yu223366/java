-- ============================================================
--  雾山五行 · 第二轮升级 B：宗门宝库 / 寄售 / 辩场点赞 / 修行 / 炼制 / 奇遇（可重复执行）
--    1) 宗门宝库库存 wushan_sect_stock（贡献点兑换）+ 兑换流水
--    2) 山友寄售 wushan_consign（灵石购买 / 以物换物）
--    3) 辩场点赞去重 wushan_discourse_like（谁赞多谁赢）
--    4) 练功 wushan_practice（30 秒一次）
--    5) 十分钟一轮的题目 wushan_question_slot / wushan_question_answer
--    6) 炼丹炼器阵法成功率 wushan_recipe.success_rate / craft_type + 新配方
--    7) 秘境奇遇 wushan_fortune_event + wushan_explore_log.fortune_json
--    8) 导航新增「炼器」「阵法」
--  依赖：先执行 upgrade-v2-items.sql
-- ============================================================
USE wushan;

-- ------------------------------------------------------------
-- 1) 宗门宝库：一宗一库，物品由宗门成员存入，用贡献点兑换
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_sect_stock (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  sect_id             BIGINT NOT NULL COMMENT '宗门 id',
  item_id             BIGINT NOT NULL COMMENT '物品 id（wushan_item）',
  item_title          VARCHAR(128) DEFAULT '' COMMENT '物品名（冗余）',
  count               INT NOT NULL DEFAULT 0 COMMENT '库存',
  contribution_price  INT NOT NULL DEFAULT 20 COMMENT '兑换所需贡献点',
  status              TINYINT NOT NULL DEFAULT 1 COMMENT '1=在架 0=下架',
  create_time         DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_sect_item (sect_id, item_id),
  KEY idx_sect (sect_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宗门宝库库存';

CREATE TABLE IF NOT EXISTS wushan_sect_stock_log (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  sect_id        BIGINT NOT NULL,
  user_id        BIGINT NOT NULL,
  username       VARCHAR(32) DEFAULT '',
  item_title     VARCHAR(128) DEFAULT '',
  amount         INT NOT NULL DEFAULT 0 COMMENT '贡献点变动（兑换为负）',
  action         VARCHAR(16) DEFAULT 'redeem' COMMENT 'redeem 兑换 / deposit 存入',
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宗门宝库流水';

-- 给已有宗门各灌 12 件基础藏品（按品阶定贡献点：品阶等第 × 20）
INSERT INTO wushan_sect_stock (sect_id, item_id, item_title, count, contribution_price, status)
SELECT s.id, i.id, i.title,
       CASE i.grade_key WHEN 'sheng' THEN 1 WHEN 'tian' THEN 2 WHEN 'di' THEN 3 ELSE 6 END,
       (SELECT grade_rank FROM wushan_grade WHERE grade_key = i.grade_key) * 20,
       1
FROM wushan_sect s
CROSS JOIN (SELECT id, title, grade_key FROM wushan_item WHERE status = 1 ORDER BY id LIMIT 12) i
WHERE NOT EXISTS (SELECT 1 FROM wushan_sect_stock st WHERE st.sect_id = s.id AND st.item_id = i.id);

-- ------------------------------------------------------------
-- 2) 山友寄售：别人的物品可挂到宝库，用灵石买或以物换物
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_consign (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  seller_id       BIGINT NOT NULL,
  seller_name     VARCHAR(32) NOT NULL DEFAULT '',
  item_id         BIGINT NOT NULL COMMENT '寄售物品 id（wushan_item）',
  item_title      VARCHAR(128) DEFAULT '',
  count           INT NOT NULL DEFAULT 1,
  price_spirit    INT NOT NULL DEFAULT 0 COMMENT '灵石价（0=只换不卖）',
  barter_item_id  BIGINT DEFAULT NULL COMMENT '想换到的物品 id（NULL=任选）',
  barter_title    VARCHAR(128) DEFAULT '' COMMENT '想换到的物品名 / 口头约定',
  mode            TINYINT NOT NULL DEFAULT 0 COMMENT '0=只卖灵石 1=只以物换物 2=两者皆可',
  status          TINYINT NOT NULL DEFAULT 0 COMMENT '0=在架 1=已成交 2=已撤销 3=已取回',
  buyer_id        BIGINT DEFAULT NULL,
  buyer_name      VARCHAR(32) DEFAULT '',
  create_time     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_status (status),
  KEY idx_seller (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宝库山友寄售';

-- ------------------------------------------------------------
-- 3) 辩场点赞去重：一人对一条陈词只能赞一次，票数多的一方胜
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_discourse_like (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  discourse_id  BIGINT NOT NULL,
  user_id       BIGINT NOT NULL,
  create_time   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_once (discourse_id, user_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='辩场点赞（防刷）';

-- ------------------------------------------------------------
-- 4) 练功：每 30 秒一次，得灵石与修为
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_practice (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT NOT NULL,
  level          INT NOT NULL DEFAULT 1 COMMENT '练功重数',
  exp            INT NOT NULL DEFAULT 0 COMMENT '本重修为（满 100 升一重）',
  total_count    INT NOT NULL DEFAULT 0 COMMENT '累计练功次数',
  total_spirit   INT NOT NULL DEFAULT 0 COMMENT '累计所得灵石',
  last_time      DATETIME DEFAULT NULL COMMENT '上次练功时间',
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练功记录';

-- ------------------------------------------------------------
-- 5) 十分钟一轮的题目：题目按时间槽生成，作答按槽去重
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_question_slot (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  slot_key       VARCHAR(24) NOT NULL COMMENT '形如 2026-11-11-1430，十分钟一槽',
  question       VARCHAR(512) NOT NULL,
  options_json   VARCHAR(1024) NOT NULL,
  answer_idx     TINYINT NOT NULL,
  reward_spirit  INT NOT NULL DEFAULT 5,
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_slot (slot_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每旬一题（十分钟一轮）';

CREATE TABLE IF NOT EXISTS wushan_question_answer (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id        BIGINT NOT NULL,
  slot_key       VARCHAR(24) NOT NULL,
  answer_idx     TINYINT NOT NULL,
  is_correct     TINYINT NOT NULL DEFAULT 0,
  reward_spirit  INT NOT NULL DEFAULT 0,
  create_time    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_slot (user_id, slot_key),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每旬一题作答';

-- ------------------------------------------------------------
-- 6) 炼制成功率 + 三系分类
-- ------------------------------------------------------------
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_recipe ADD COLUMN success_rate INT NOT NULL DEFAULT 85 COMMENT ''成功率（百分比）''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_recipe' AND COLUMN_NAME='success_rate');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_recipe ADD COLUMN craft_type VARCHAR(16) NOT NULL DEFAULT ''danyao'' COMMENT ''danyao 炼丹 / lianqi 炼器 / zhenfa 阵法''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_recipe' AND COLUMN_NAME='craft_type');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 老配方：丹药 90→75（越贵的越难成），炼器 70
UPDATE wushan_recipe SET
  craft_type = CASE category WHEN 'faqi' THEN 'lianqi' ELSE 'danyao' END,
  success_rate = CASE
    WHEN category = 'faqi' THEN 70
    WHEN cost_spirit >= 90 THEN 75
    WHEN cost_spirit >= 60 THEN 80
    WHEN cost_spirit >= 35 THEN 85
    ELSE 90 END
WHERE craft_type = 'danyao' AND category <> 'zhenfa' AND id <= 5;

-- 新配方：炼器 4 条 / 阵法 3 条 / 丹药 2 条（产物按名称取 id，材料亦按名称取 id）
-- 用 INSERT ... SELECT + JSON_OBJECT 组装 materials_json，避免依赖硬编码 id
INSERT INTO wushan_recipe
  (name, category, output_item_id, output_count, cost_spirit, materials_json, description, status, success_rate, craft_type)
SELECT x.nm, x.cat,
       (SELECT id FROM wushan_item WHERE title = x.out_title ORDER BY id LIMIT 1),
       1, x.cost,
       JSON_ARRAY(JSON_OBJECT('itemId', (SELECT id FROM wushan_item WHERE title = x.m1 ORDER BY id LIMIT 1), 'count', x.c1),
                  JSON_OBJECT('itemId', (SELECT id FROM wushan_item WHERE title = x.m2 ORDER BY id LIMIT 1), 'count', x.c2)),
       x.descr, 1, x.rate, x.cat
FROM (
  SELECT '青锋剑·初锻' nm, 'lianqi' cat, '青锋剑' out_title, 60 cost, '庚金砂' m1, 2 c1, '玄铁母' m2, 1 c2,
    '庚金砂提锋、玄铁母为骨，地心火连煅九日。火候一过则器裂，故成器只有七成把握。' descr, 70 rate
  UNION ALL SELECT '赤焰葫芦·封火', 'lianqi', '赤焰葫芦', 90, '离火晶', 2, '两仪砂', 1,
    '以离火晶作胆，两仪砂封口，封火时最易炸炉，须屏息一气呵成。', 60
  UNION ALL SELECT '避风铜铃·铸铃', 'lianqi', '避风铜铃', 30, '庚金砂', 1, '粗麻符纸', 1,
    '铜铃小而精巧，铃舌最考手艺，初学炼器者多从此物练起。', 85
  UNION ALL SELECT '掣电梭·淬光', 'lianqi', '掣电梭', 150, '离火晶', 2, '风灵羽', 2,
    '电光石髓早已难得，再以风灵羽提速，十炉九废，非宗师不敢开。', 45
  UNION ALL SELECT '小迷踪阵旗·成阵', 'zhenfa', '小迷踪阵旗', 40, '定风旗杆', 2, '两仪砂', 2,
    '五旗按梅花位刻纹，阵眼一偏则迷阵自散，成阵率约八成。', 80
  UNION ALL SELECT '离火焚天阵盘·刻纹', 'zhenfa', '离火焚天阵盘', 80, '阵纹墨', 1, '引雷针', 2,
    '赤铜盘上刻七十二道火纹，一笔错则全盘废，须以阵纹墨一气画完。', 65
  UNION ALL SELECT '两仪归元阵·定基', 'zhenfa', '两仪归元阵', 160, '息壤砂', 1, '两仪砂', 3,
    '息壤砂固界、两仪砂分阴阳，山门护界之阵，成阵最难。', 50
  UNION ALL SELECT '培元丹·简方', 'danyao', '培元丹', 25, '百年何首乌', 1, '寻常艾草', 2,
    '何首乌为君、陈艾为使，文火慢煎即可，最宜新手试炉。', 90
  UNION ALL SELECT '凝神丹·寒方', 'danyao', '凝神丹', 70, '玄冰莲子', 1, '阵纹墨', 1,
    '莲心寒、墨性凝，二物同炉须压住火候，稍猛则丹焦。', 75
) x
WHERE NOT EXISTS (SELECT 1 FROM wushan_recipe r WHERE r.name = x.nm)
  AND (SELECT id FROM wushan_item WHERE title = x.out_title ORDER BY id LIMIT 1) IS NOT NULL;

-- ------------------------------------------------------------
-- 7) 秘境奇遇：按权重随机触发，额外掉落
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS wushan_fortune_event (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  name         VARCHAR(64) NOT NULL COMMENT '奇遇名',
  event_desc   VARCHAR(512) DEFAULT '' COMMENT '奇遇描述',
  spirit       INT NOT NULL DEFAULT 0 COMMENT '额外灵石',
  item_title   VARCHAR(128) DEFAULT NULL COMMENT '额外掉落物品名',
  item_count   INT NOT NULL DEFAULT 0,
  weight       INT NOT NULL DEFAULT 1 COMMENT '抽取权重',
  grade_key    VARCHAR(16) DEFAULT 'xuan',
  status       TINYINT NOT NULL DEFAULT 1,
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='秘境奇遇事件库';

INSERT INTO wushan_fortune_event (name, event_desc, spirit, item_title, item_count, weight, grade_key, status)
SELECT x.nm, x.ds, x.sp, x.it, x.cnt, x.w, x.g, 1 FROM (
  SELECT '石壁遗偈' nm, '苔痕下露出一偈，读罢心头澄明，灵气自涌。' ds, 88 sp, NULL it, 0 cnt, 3 w, 'xuan' g
  UNION ALL SELECT '灵泉喷薄', '脚下石缝忽涌灵泉，饮之通体舒泰。', 120, NULL, 0, 2, 'di'
  UNION ALL SELECT '仙人遗囊', '枯骨旁一只旧囊，囊中尚存几味材料。', 30, '雷击木心', 2, 3, 'tian'
  UNION ALL SELECT '古阵残眼', '误触一处阵眼，阵纹流转间落下两仪砂。', 20, '两仪砂', 3, 4, 'huang'
  UNION ALL SELECT '药王旧圃', '崖后竟有一畦无人看管的药圃，采得百年何首乌。', 40, '百年何首乌', 2, 3, 'xuan'
  UNION ALL SELECT '雷池淬体', '一道落雷擦身而过，皮肉无损，反觉神清。', 160, '雷纹芝', 1, 1, 'tian'
  UNION ALL SELECT '山灵赠砂', '山灵现身，赠一撮息壤砂作过路礼。', 60, '息壤砂', 1, 1, 'sheng'
  UNION ALL SELECT '剑冢拾锋', '断剑堆里拾得一块尚有余锋的玄铁母。', 25, '玄铁母', 2, 3, 'di'
) x
WHERE NOT EXISTS (SELECT 1 FROM wushan_fortune_event f WHERE f.name = x.nm);

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_explore_log ADD COLUMN fortune_json VARCHAR(512) DEFAULT NULL COMMENT ''奇遇记录''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_explore_log' AND COLUMN_NAME='fortune_json');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ------------------------------------------------------------
-- 8) 导航入口：炼器、阵法（前端 HomePanel 会从后端拉导航覆盖）
-- ------------------------------------------------------------
INSERT INTO wushan_nav (nav_key, label, en, sort, status)
SELECT 'lianqi', '炼器', 'forging', 13, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan_nav WHERE nav_key = 'lianqi');
INSERT INTO wushan_nav (nav_key, label, en, sort, status)
SELECT 'zhenfa', '阵法', 'formation', 14, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan_nav WHERE nav_key = 'zhenfa');
