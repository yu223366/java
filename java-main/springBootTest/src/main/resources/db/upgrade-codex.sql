-- ============================================================
--  雾山五行 · 藏经阁后端升级（灵根 / 秘境）
--  说明：在已有 schema-home.sql + upgrade-nav.sql 基础上的增量脚本：
--    * 新建 wushan_spirit_root 灵根表（藏经阁「灵根」页签数据源）
--    * 新建 wushan_realm      秘境表（藏经阁「秘境」页签数据源）
--    * 种子数据与前端常量 src/constants/codex.js（SPIRIT_ROOTS / REALMS）同口径
--  后端接口见 controller/CodexController.java：
--    GET /api/codex/items        条目（功法/心得/修炼资源，查 wushan_item）
--    GET /api/codex/spirit-roots 灵根列表 ?element=&grade=
--    GET /api/codex/realms       秘境列表 ?element=&grade=
-- ============================================================
USE wushan;

-- ------------------------------------------------------------
-- 1) 灵根表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_spirit_root;
CREATE TABLE wushan_spirit_root (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name        VARCHAR(64)  NOT NULL                COMMENT '灵根名',
  element     VARCHAR(16)  DEFAULT NULL            COMMENT '五行 key',
  grade_key   VARCHAR(16)  DEFAULT NULL            COMMENT '品阶 key',
  summary     VARCHAR(512) DEFAULT NULL            COMMENT '灵根简介',
  sort        INT          DEFAULT 0               COMMENT '排序（小在前）',
  status      TINYINT      DEFAULT 1               COMMENT '状态：1=收录 0=隐匿',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '收录时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_root_name (name),
  KEY idx_element (element),
  KEY idx_grade (grade_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='藏经阁灵根';

INSERT INTO wushan_spirit_root (name, element, grade_key, summary, sort) VALUES
('庚金灵根', 'jin',    'tian',  '锋锐天成，习金行功法一日千里，杀伐之气重。', 1),
('青木灵根', 'mu',     'tian',  '生生不息，疗伤自愈远胜常人，木行天授。',   2),
('弱水灵根', 'shui',   'tian',  '性灵近水，吐纳如鱼得渊，变化之资。',       3),
('离火灵根', 'huo',    'tian',  '火毒不侵，心火自燃，攻伐之资冠绝十行。',   4),
('息土灵根', 'tu',     'tian',  '根骨如山，稳如磐石，守御与耐力之最。',     5),
('罡风灵根', 'feng',   'di',    '身轻如燕，感风先知，身法一途天赋异禀。',   6),
('惊雷灵根', 'lei',    'di',    '引雷淬体，怒而愈强，天生气势慑人。',       7),
('掣电灵根', 'dian',   'di',    '反应如电，一点即通，最忌迟疑。',           8),
('混元灵根', 'taiji',  'sheng', '万法圆融，诸系皆通，百年难出一人。',       9),
('两仪灵根', 'yinyang','sheng', '阴阳同修，昼夜异能，圣品之资。',           10);

-- 补齐灵根品阶覆盖（2026-10-16）：凡/黄/玄原先全空，品阶筛选会出空态。
INSERT INTO wushan_spirit_root (name, element, grade_key, summary, sort) VALUES
('金铁杂根', 'jin',    'fan',   '微具金性，练金行事倍功半，胜在心志。',     11),
('静电微根', 'dian',   'fan',   '冬日指尖有麻意，聊胜于无，贵在自知。',     12),
('赤炎双根', 'huo',    'huang', '心头两点火种，攻伐稍逊离火，胜在稳定。',   13),
('疾风敏根', 'feng',   'huang', '感风即动，身法入门远胜常人。',             14),
('玄水真根', 'shui',   'xuan',  '水性通透，冰火同源，次弱水灵根一等的真根。', 15),
('紫霆真根', 'lei',    'xuan',  '引雷不伤，怒雷淬体比惊雷灵根温和。',       16);

-- ------------------------------------------------------------
-- 2) 秘境表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_realm;
CREATE TABLE wushan_realm (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  name        VARCHAR(64)  NOT NULL                COMMENT '秘境名',
  element     VARCHAR(16)  DEFAULT NULL            COMMENT '五行 key',
  grade_key   VARCHAR(16)  DEFAULT NULL            COMMENT '品阶 key',
  layers      VARCHAR(16)  DEFAULT NULL            COMMENT '层数（如：九层）',
  summary     VARCHAR(512) DEFAULT NULL            COMMENT '秘境简介',
  sort        INT          DEFAULT 0               COMMENT '排序（小在前）',
  status      TINYINT      DEFAULT 1               COMMENT '状态：1=开启 0=封闭',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '开启时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_realm_name (name),
  KEY idx_element (element),
  KEY idx_grade (grade_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='藏经阁秘境';

INSERT INTO wushan_realm (name, element, grade_key, layers, summary, sort) VALUES
('焚火秘境',   'huo',    'di',   '九层',   '地火奔涌，愈深愈烈，最深处传闻有火髓。',   1),
('离火洞天',   'huo',    'tian', '三层',   '天品秘境，非离火灵根者入内即灼。',       2),
('断剑冢',     'jin',    'di',   '七层',   '万剑朝冢，剑气森然，剑修圣地。',         3),
('白刃崖',     'jin',    'xuan', '五层',   '崖壁如刃，采药炼体两相宜。',             4),
('青木林深处', 'mu',     'xuan', '五层',   '草木有灵，迷径重重，需持本心而行。',     5),
('长生谷',     'mu',     'di',   '九层',   '药气氤氲，愈深药龄愈老。',               6),
('寒潭秘境',   'shui',   'xuan', '三层',   '潭底别有洞天，寒气刺骨。',               7),
('沧浪海眼',   'shui',   'tian', '一层',   '海眼之下暗流万千，一步一世界。',         8),
('地肺石窟',   'tu',     'di',   '十二层', '层峦入地，矿脉纵横，土行宝藏。',         9),
('息壤秘窟',   'tu',     'tian', '一层',   '天品秘境，见者有份，去者多留。',         10),
('风眼长廊',   'feng',   'xuan', '九层',   '罡风穿廊，身法试炼之地。',               11),
('青岚绝壁',   'feng',   'di',   '五层',   '岚气缠壁，攀者如御风。',                 12),
('落雷原',     'lei',    'di',   '七层',   '终年雷暴，引雷淬体者的乐园。',           13),
('九霄雷池',   'lei',    'tian', '一层',   '天品秘境，雷池之水可淬神魂。',           14),
('电光裂缝',   'dian',   'xuan', '三层',   '裂缝间电光游走，进退须快。',             15),
('掣电之隙',   'dian',   'di',   '一层',   '一瞬即达，一瞬即失，考验眼力。',         16),
('太极幻境',   'taiji',  'tian', '一层',   '阴阳双鱼流转，入境者各见本心。',         17),
('晦明谷',     'yinyang','di',   '三层',   '昼夜各半，阴阳消长，修炼互济功之地。',   18);

-- 补齐秘境品阶覆盖（2026-10-16）：凡/黄/圣原先全空，品阶筛选会出空态。
INSERT INTO wushan_realm (name, element, grade_key, layers, summary, sort) VALUES
('乱石迷径',   'tu',     'fan',  '一层',   '乱石堆成的天然迷径，试炼新人的第一关。', 19),
('萤火洞',     'huo',    'fan',  '一层',   '洞中萤火常年不熄，夜行训练之地。',       20),
('碧潭幽谷',   'shui',   'huang','三层',   '谷中碧潭连串，采药避暑两相宜。',         21),
('松涛林海',   'feng',   'huang','五层',   '风起松涛，练听风辨位的去处。',           22),
('太初混元境', 'taiji',  'sheng','一层',   '开山祖师坐化之地，一步一纪元。',         23),
('两仪归墟',   'yinyang','sheng','一层',   '阴阳二气在此归墟，互济功大成之地。',     24);
