-- ============================================================
--  雾山五行 · 首页导航内容升级（论道堂 / 市集）
--  说明：在已有 schema-home.sql 基础上的增量脚本，可重复执行前的注意事项：
--    * 新增分类（丹药 / 法器）走 INSERT ... 仅在不存在时插入
--    * 新增论道语料若干条
--    * 新建 wushan_market_item 市集物品表（DROP 后重建并灌种子）
--    * 新建 wushan_trade_log 交易日志表
-- ============================================================
USE wushan;

-- ------------------------------------------------------------
-- 1) 分类树扩充：修炼资源下新增「丹药 / 法器」二级分类
-- ------------------------------------------------------------
INSERT INTO wushan_category (id, cat_key, name, en, parent_id, cat_level, sort)
SELECT 10, 'danyao', '丹药', 'pill', 3, 2, 3 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan_category WHERE cat_key = 'danyao');
INSERT INTO wushan_category (id, cat_key, name, en, parent_id, cat_level, sort)
SELECT 11, 'faqi', '法器', 'artifact', 3, 2, 4 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan_category WHERE cat_key = 'faqi');

-- 配套搜索条目（保证分级分类搜索在丹药/法器下有内容）
INSERT INTO wushan_item (title, summary, element, grade_key, category_id, sort) VALUES
('培元丹',     '固本培元的常用丹药，性温无碍，人人可服。',   'mu',    'huang', 10, 1),
('凝神丹',     '宁心凝神，行功走火时一粒定惊。',             'shui',  'xuan',  10, 2),
('破障丹',     '冲击关口前的引路丹，破障一臂之力。',         'huo',   'di',    10, 3),
('洗髓丹',     '洗经伐髓，脱胎换骨，可遇不可求。',           'shui',  'tian',  10, 4),
('金创药',     '外伤速愈的寻常药散，行囊必备。',             'jin',   'fan',   10, 5),
('青锋剑',     '三尺青锋，削铁如泥，剑修常备。',             'jin',   'xuan',  11, 1),
('赤焰葫芦',   '内蕴一缕地心火，可放可收。',                 'huo',   'di',    11, 2),
('藏水青囊',   '囊中自有一汪寒潭，旱地亦可用。',             'shui',  'xuan',  11, 3),
('千钧印',     '一印压下重逾千钧，封敌断路。',               'tu',    'di',    11, 4),
('避风铜铃',   '风起铃动，先人一步知险。',                   'feng',  'huang', 11, 5);

-- ------------------------------------------------------------
-- 2) 论道堂语料扩充（配合论道分页列表）
-- ------------------------------------------------------------
INSERT INTO wushan_discourse (name, element, quote, zan, sort) VALUES
('观星客',     'taiji',  '仰观星，俯观心，星不语，心自明。',           19, 11),
('藏锋居士',   'jin',    '利刃在鞘方为利刃，人前藏锋是修行。',         17, 12),
('拾露童子',   'mu',     '晨露拾不满一碗，却洗亮了一双眼。',           15, 13),
('半盏道人',   'shui',   '茶斟半盏，话留三分，路才走得远。',           12, 14),
('燃灯叟',     'huo',    '灯芯烧的是油，亮的是别人夜里的路。',           9, 15),
('埋玉人',     'tu',     '玉埋土中千年，出土仍是玉。',                   8, 16),
('驭雷童',     'lei',    '怕雷的人听不见雷里的天机。',                   7, 17),
('流光君',     'dian',   '一瞬即是一生，莫负电光石火。',                 5, 18);

-- ------------------------------------------------------------
-- 3) 市集物品表（导航「市集」数据源）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_market_item;
CREATE TABLE wushan_market_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title       VARCHAR(128) NOT NULL                COMMENT '物品名',
  summary     VARCHAR(512) DEFAULT NULL            COMMENT '物品简介',
  element     VARCHAR(16)  DEFAULT NULL            COMMENT '五行 key',
  grade_key   VARCHAR(16)  DEFAULT NULL            COMMENT '品阶 key',
  category_id BIGINT       DEFAULT NULL            COMMENT '分类 id（关联 wushan_category）',
  price       INT          NOT NULL DEFAULT 0      COMMENT '售价（灵石）',
  stock       INT          NOT NULL DEFAULT 0      COMMENT '库存',
  seller      VARCHAR(32)  NOT NULL DEFAULT '雾山山货铺' COMMENT '摊主',
  sold        INT          NOT NULL DEFAULT 0      COMMENT '已售',
  status      TINYINT      NOT NULL DEFAULT 1      COMMENT '状态：1=在售 0=下架',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_element (element),
  KEY idx_grade (grade_key),
  KEY idx_category (category_id),
  KEY idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='市集物品';

INSERT INTO wushan_market_item (title, summary, element, grade_key, category_id, price, stock, seller, sold) VALUES
('千年灵芝',   '固本培元第一灵药，重伤亦可续命。',       'mu',    'tian',  8,  800,  2, '拾露童子',  3),
('固元参',     '常见的培元参类，价平效稳。',             'mu',    'huang', 8,   30, 50, '雾山山货铺', 21),
('朱果',       '赤红果实，服之火行亲和大增。',           'huo',   'xuan',  8,  120, 12, '燃灯叟',    7),
('寒潭玉液',   '千年寒潭底的一捧玉液，洗经伐髓。',       'shui',  'tian',  8,  660,  3, '半盏道人',  2),
('净水珠',     '化浊为清，行路必备的珠子。',             'shui',  'huang', 8,   25, 40, '雾山山货铺', 16),
('剑心草',     '叶如剑形，服之坚定剑心。',               'jin',   'huang', 8,   20, 60, '藏锋居士',  9),
('庚金精',     '万年金气凝成的精粒，锻造上品兵刃之基。', 'jin',   'tian',  9,  900,  1, '藏锋居士',  4),
('白玉髓',     '温润如玉而坚逾铁，炼器辅材。',           'jin',   'xuan',  9,  150,  8, '雾山山货铺', 5),
('火纹石',     '石纹如焰，铺设火阵的常用材料。',         'huo',   'huang', 9,   15, 80, '燃灯叟',   12),
('雷击木心',   '历雷不死的老木之心，镇宅辟邪。',         'lei',   'tian',  9,  750,  2, '驭雷童',    1),
('紫霄髓',     '雷云深处的凝结物，淬炼神魂。',           'lei',   'xuan',  9,  180,  6, '驭雷童',    3),
('息壤',       '生生不息的神土，一撮可成一丘。',         'tu',    'tian',  9, 1200,  1, '埋玉人',    0),
('风灵羽',     '风灵蜕下的羽衣，御风而行的凭依。',       'feng',  'tian',  9,  700,  2, '听风散人',  2),
('培元丹',     '固本培元的常用丹药，性温无碍。',         'mu',    'huang', 10,  40, 30, '雾山药庐', 18),
('凝神丹',     '宁心凝神，走火入魔时一粒定惊。',         'shui',  'xuan',  10,  90, 15, '雾山药庐',  8),
('破障丹',     '冲击关口前的引路丹。',                   'huo',   'di',    10, 260,  6, '雾山药庐',  2),
('洗髓丹',     '洗经伐髓，脱胎换骨，可遇不可求。',       'shui',  'tian',  10, 1500, 1, '雾山药庐',  0),
('青锋剑',     '三尺青锋，削铁如泥，剑修常备。',         'jin',   'xuan',  11, 200,  5, '藏锋居士',  3),
('赤焰葫芦',   '内蕴一缕地心火，可放可收。',             'huo',   'di',    11, 480,  3, '焚舟子',    1),
('藏水青囊',   '囊中自有一汪寒潭，旱地亦可用。',         'shui',  'xuan',  11, 160,  7, '半盏道人',  4),
('千钧印',     '一印压下重逾千钧，封敌断路。',           'tu',    'di',    11, 520,  2, '厚土翁',    1),
('避风铜铃',   '风起铃动，先人一步知险。',               'feng',  'huang', 11,  35, 25, '听风散人', 10),
-- 电行（补齐：原种子缺 dian / taiji / yinyang 三行，按元素筛选会空）
('电光石髓',   '闪电击石所留的髓纹，服之神速。',         'dian',   'tian',  8,  680,  4, '驭雷童',     1),
('掣电梭',     '掷出即至的暗器，肉眼难追。',             'dian',   'xuan',  11, 160,  6, '流光君',     2),
('明澈露',     '洗目之露，视物极明。',                   'dian',   'huang', 8,   22, 45, '雾山药庐',  13),
-- 太极
('太极玉璧',   '璧上阴阳流转，参悟圆融之理。',           'taiji',  'tian',  9,  720,  2, '圆转师太',   1),
('圆转石',     '握之掌心自转，静心之石。',               'taiji',  'xuan',  9,   145,  7, '圆转师太',   3),
('两仪砂',     '阴阳两色的细砂，布两仪阵之基。',         'taiji',  'huang', 9,    18, 60, '雾山山货铺', 22),
('太极道种',   '大道之种，得者可开一宗。',               'taiji',  'sheng', 9,  3000,  1, '圆转师太',   0),
-- 阴阳
('阴阳双鱼佩', '佩分两半，合则气机圆满。',               'yinyang','tian',  11, 860,  1, '负阴君',     2),
('晦朔草',     '晦日朔日各开一花的奇草。',               'yinyang','xuan',  8,   155,  5, '负阴君',     2),
('子午露',     '子午二时收取的露水，调和气机。',         'yinyang','huang', 8,    28, 38, '半盏道人',  9),
-- 凡品（补齐低端：原种子最低只到黄品）
('寻常艾草',   '山中最常见的艾草，灸疗入门。',           'mu',     'fan',   8,     6, 120, '雾山山货铺', 35),
('粗麻符纸',   '最寻常的符纸，画符练手用。',             'tu',     'fan',   9,     4, 200, '雾山山货铺', 48),
-- 圣品（补齐顶端：原种子最高只到天品）
('九转还魂丹', '传说中能起死回生的丹药，千年一出。',     'mu',     'sheng', 10, 2400,  1, '雾山药庐',   0);

-- ------------------------------------------------------------
-- 4) 交易日志（市集购买留痕，可统计热度）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_trade_log;
CREATE TABLE wushan_trade_log (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  item_id     BIGINT      NOT NULL                COMMENT '物品 id',
  item_title  VARCHAR(128) DEFAULT NULL            COMMENT '物品名（冗余，便于查账）',
  buyer_id    BIGINT      DEFAULT NULL            COMMENT '买家 id（未登录为空）',
  buyer       VARCHAR(32) DEFAULT NULL            COMMENT '买家名号',
  buy_count   INT         NOT NULL DEFAULT 1      COMMENT '购买数量',
  total_price INT         NOT NULL DEFAULT 0      COMMENT '总价（灵石）',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '成交时间',
  PRIMARY KEY (id),
  KEY idx_item (item_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='市集交易日志';
