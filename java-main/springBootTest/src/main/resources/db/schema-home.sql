-- ============================================================
--  雾山五行 · 首页后端 建表语句（搜索 + 首页展示）
--  数据库：wushan（utf8mb4）
--  说明：首页后端所需的全部表 ——
--    1. wushan_category   分类表（parent_id 自关联，支持分级）
--    2. wushan_grade     品阶表（rank 分级：凡→黄→玄→地→天→圣）
--    3. wushan_item      条目表（搜索对象：功法 / 心得 / 修炼资源…）
--    4. wushan_nav       顶部导航
--    5. wushan_hot_keyword 热门搜索词
--    6. wushan_announcement 山门公告（跑马灯）
--    7. wushan_scene     山中四时图卷
--    8. wushan_discourse 山友论道榜
--    9. wushan_search_log 搜索日志（可统计热词）
--  种子数据与前端 mock（src/api/home.js、src/constants/codex.js）口径一致，
--  前后端切换后展示效果不变。
-- ============================================================

CREATE DATABASE IF NOT EXISTS wushan
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE wushan;

-- ------------------------------------------------------------
-- 1) 分类表（自关联分级：一级 / 二级）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_category;
CREATE TABLE wushan_category (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  cat_key     VARCHAR(32) NOT NULL                COMMENT '分类 key（英文，供前端传参）',
  name        VARCHAR(32) NOT NULL                COMMENT '分类名（中文）',
  en          VARCHAR(64) DEFAULT NULL            COMMENT '英文名',
  parent_id   BIGINT      NOT NULL DEFAULT 0      COMMENT '父分类 id：0=顶级',
  cat_level   TINYINT     NOT NULL DEFAULT 1      COMMENT '层级：1=一级 2=二级',
  sort        INT         DEFAULT 0               COMMENT '排序（小在前）',
  status      TINYINT     DEFAULT 1               COMMENT '状态：1=启用 0=下架',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_cat_key (cat_key),
  KEY idx_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宝库分类（分级）';

INSERT INTO wushan_category (id, cat_key, name, en, parent_id, cat_level, sort) VALUES
  (1, 'gongfa',  '功法',     'technique', 0, 1, 1),
  (2, 'xinde',   '心得',     'insight',   0, 1, 2),
  (3, 'ziyuan',  '修炼资源', 'resource',  0, 1, 3),
  (4, 'xinfa',   '心法',     'mind',      1, 2, 1),
  (5, 'shenfa',  '身法',     'movement',  1, 2, 2),
  (6, 'shufa',   '术法',     'skill',     1, 2, 3),
  (7, 'zhenfa',  '阵法',     'formation', 1, 2, 4),
  (8, 'lingyao', '灵药',     'herb',      3, 2, 1),
  (9, 'liancai', '炼材',     'material',  3, 2, 2);

-- ------------------------------------------------------------
-- 2) 品阶表（rank 1~6 由低到高，支持“该品及以上”式分级筛选）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_grade;
CREATE TABLE wushan_grade (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  grade_key   VARCHAR(16) NOT NULL                COMMENT '品阶 key：fan/huang/xuan/di/tian/sheng',
  name        VARCHAR(16) NOT NULL                COMMENT '品阶名：凡品/黄品/玄品/地品/天品/圣品',
  grade_rank  TINYINT     NOT NULL                COMMENT '等第：1=凡 … 6=圣',
  color       VARCHAR(16) DEFAULT NULL            COMMENT '前端展示色',
  sort        INT         DEFAULT 0               COMMENT '排序',
  status      TINYINT     DEFAULT 1               COMMENT '状态：1=启用 0=下架',
  PRIMARY KEY (id),
  UNIQUE KEY uk_grade_key (grade_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='品阶（分级）';

INSERT INTO wushan_grade (id, grade_key, name, grade_rank, color, sort) VALUES
  (1, 'fan',   '凡品', 1, '#9aa08c', 1),
  (2, 'huang', '黄品', 2, '#e8c372', 2),
  (3, 'xuan',  '玄品', 3, '#3a8fd0', 3),
  (4, 'di',    '地品', 4, '#9d7bf0', 4),
  (5, 'tian',  '天品', 5, '#e8412b', 5),
  (6, 'sheng', '圣品', 6, '#f2e34d', 6);

-- ------------------------------------------------------------
-- 3) 条目表（搜索对象）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_item;
CREATE TABLE wushan_item (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title       VARCHAR(128) NOT NULL                COMMENT '条目名',
  summary     VARCHAR(512) DEFAULT NULL            COMMENT '条目简介',
  element     VARCHAR(16)  DEFAULT NULL            COMMENT '五行 key：jin/mu/shui/huo/tu/feng/lei/dian/taiji/yinyang',
  grade_key   VARCHAR(16)  DEFAULT NULL            COMMENT '品阶 key',
  category_id BIGINT      NOT NULL                COMMENT '分类 id（关联 wushan_category）',
  sort        INT          DEFAULT 0              COMMENT '排序（小在前）',
  status      TINYINT      DEFAULT 1              COMMENT '状态：1=上架 0=下架',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_element (element),
  KEY idx_category (category_id),
  KEY idx_grade (grade_key),
  KEY idx_title (title)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='宝库条目（搜索对象）';

INSERT INTO wushan_item (title, summary, element, grade_key, category_id, sort) VALUES
/* ---- 火 ---- */
('焚天七绝焰',   '七式连环，焰随心走，至刚至烈，火行至高功法。', 'huo', 'tian', 6, 1),
('心火燎原功',   '以内火淬炼经脉，痛而后明，进境极快。',         'huo', 'di',   4, 2),
('引火诀',       '引外火为己用，火行入门之基，稳而无毒。',       'huo', 'xuan', 4, 3),
('《火候记》',   '文火武火，全凭一心，论火行修炼的节奏。',       'huo', 'huang', 2, 4),
('《明心一炬》', '火不照外物，照的是本心，心明则焰清。',         'huo', 'di',   2, 5),
('地心火髓',     '地脉深处的火之精华，炼体炼器两相宜。',         'huo', 'tian', 9, 6),
('朱果',         '赤红果实，服之火行亲和大增。',                 'huo', 'xuan', 8, 7),
('火纹石',       '石纹如焰，铺设火阵的常用材料。',               'huo', 'huang', 9, 8),
/* ---- 金 ---- */
('庚金百斩诀',   '百斩一意，斩物斩念，金行至高攻伐之法。',       'jin', 'tian', 6, 1),
('金刚不坏体',   '淬体如铁，横练金身，守御第一。',               'jin', 'di',   4, 2),
('锐金剑指',     '指出如剑，锋锐凝于一线，破甲穿石。',           'jin', 'xuan', 6, 3),
('《断金录》',   '当断不断，反受其乱，论金行之决断。',           'jin', 'huang', 2, 4),
('《锋藏于鞘》', '利刃在鞘方为利刃，藏锋是金行的第一课。',       'jin', 'di',   2, 5),
('庚金精',       '万年金气凝成的精粒，锻造上品兵刃之基。',       'jin', 'tian', 9, 6),
('白玉髓',       '温润如玉而坚逾铁，炼器辅材。',                 'jin', 'xuan', 9, 7),
('剑心草',       '叶如剑形，服之坚定剑心，不易动摇。',           'jin', 'huang', 8, 8),
/* ---- 木 ---- */
('青木长生功',   '生生不息，伤损自愈，木行至高养生法。',         'mu', 'tian', 4, 1),
('万藤缠身手',   '柔藤千变，缠困绞锁，以柔克刚。',               'mu', 'di',   6, 2),
('木灵吐纳法',   '晨昏吐纳，与草木共鸣，固本培元。',             'mu', 'xuan', 4, 3),
('《本草问答》', '以问答体论山中药性，采药必读。',               'mu', 'huang', 2, 4),
('《生生不息论》','损而不折，折而复生，木行的大道。',            'mu', 'di',   2, 5),
('千年灵芝',     '固本培元第一灵药，重伤亦可续命。',             'mu', 'tian', 8, 6),
('木灵乳',       '古树流出的乳白津液，温养经脉。',               'mu', 'xuan', 8, 7),
('固元参',       '常见的培元参类，价平效稳。',                   'mu', 'huang', 8, 8),
/* ---- 水 ---- */
('沧浪十七叠',   '后浪推前浪，十七叠力，水行至高攻法。',         'shui', 'tian', 6, 1),
('弱水诀',       '至柔至韧，滴水穿石，绵长不休。',               'shui', 'di',   4, 2),
('凝冰指',       '点水成冰，寒气侵体，迟滞敌形。',               'shui', 'xuan', 6, 3),
('《观水笔记》', '水无常形，随物赋形，论顺势而为。',             'shui', 'huang', 2, 4),
('《静水流深》', '不动的是表面，奔涌的是深处。',                 'shui', 'di',   2, 5),
('寒潭玉液',     '千年寒潭底的一捧玉液，洗经伐髓。',             'shui', 'tian', 8, 6),
('水云纱',       '入水不濡，织衣可避水火。',                     'shui', 'xuan', 9, 7),
('净水珠',       '化浊为清，行路必备的珠子。',                   'shui', 'huang', 8, 8),
/* ---- 土 ---- */
('厚土不灭体',   '身融大地，不动如山，土行至高守御。',           'tu', 'tian', 4, 1),
('山岳印',       '一印压下，重逾千钧，封地断路。',               'tu', 'di',   6, 2),
('撼地桩',       '扎桩于地，借大地之力发劲。',                   'tu', 'xuan', 4, 3),
('《守拙篇》',   '大巧若拙，守得住才有以后。',                   'tu', 'huang', 2, 4),
('《大地无言》', '土不言语，载万物而行。',                       'tu', 'di',   2, 5),
('息壤',         '生生不息的神土，一撮可成一丘。',               'tu', 'tian', 9, 6),
('黄精玉髓',     '土中精气所结，健脾固本。',                     'tu', 'xuan', 8, 7),
('千年窑土',     '筑炉炼器的上好底料。',                         'tu', 'huang', 9, 8),
/* ---- 风 ---- */
('大衍游风身法', '身与风合，来去无踪，风行至高身法。',           'feng', 'tian', 5, 1),
('罡风九变',     '九个方位随心切换，攻守转换极快。',             'feng', 'di',   5, 2),
('听风步',       '踏风而行，步随音动。',                         'feng', 'xuan', 5, 3),
('《风语集》',   '听风辨位，未动先知。',                         'feng', 'huang', 2, 4),
('《无痕之道》', '风过千山，不留一物，故能过千山。',             'feng', 'di',   2, 5),
('风灵羽',       '风灵蜕下的羽衣，御风而行的凭依。',             'feng', 'tian', 9, 6),
('青岚叶',       '叶落而风起，练身法常用。',                     'feng', 'xuan', 8, 7),
('追风草',       '煎汤沐足，轻身健步。',                         'feng', 'huang', 8, 8),
/* ---- 雷 ---- */
('九霄引雷诀',   '引天雷入体淬炼，雷行至高法门，凶险亦大。',     'lei', 'tian', 4, 1),
('紫霄神雷',     '掌心凝紫雷，一击震魂。',                       'lei', 'di',   6, 2),
('惊雷指',       '蓄而忽发，指落雷鸣。',                         'lei', 'xuan', 6, 3),
('《雷起于渊》', '雷霆之怒，蓄于至静。',                         'lei', 'huang', 2, 4),
('《一线之决》', '雷行修的是决断，一线之间定生死。',             'lei', 'di',   2, 5),
('雷击木心',     '历雷不死的老木之心，镇宅辟邪。',               'lei', 'tian', 9, 6),
('紫霄髓',       '雷云深处的凝结物，淬炼神魂。',                 'lei', 'xuan', 8, 7),
('引雷针',       '布阵引雷的凡铁长针。',                         'lei', 'huang', 9, 8),
/* ---- 电 ---- */
('掣电无影剑',   '剑出如电，影未现而剑已收，电行至高剑法。',     'dian', 'tian', 6, 1),
('电光淬体术',   '以电淬髓，速度与反应俱进。',                   'dian', 'di',   4, 2),
('掠光诀',       '借光而遁，瞬息十丈。',                         'dian', 'xuan', 5, 3),
('《一瞬观照》', '电光石火，照见本来。',                         'dian', 'huang', 2, 4),
('《迅烈如霹雳》','迟疑是电行唯一的天敌。',                      'dian', 'di',   2, 5),
('电光石髓',     '闪电击石所留的髓纹，服之神速。',               'dian', 'tian', 8, 6),
('掣电梭',       '掷出即至的暗器，肉眼难追。',                   'dian', 'xuan', 9, 7),
('明澈露',       '洗目之露，视物极明。',                         'dian', 'huang', 8, 8),
/* ---- 太极 ---- */
('太极玄功 · 周天图', '周天运转，万法归一，太极至高心法。',      'taiji', 'tian', 4, 1),
('阴阳鱼推手',   '听劲化劲，后发先至。',                         'taiji', 'di',   6, 2),
('揽雀尾十八式', '起承转合，式式圆转。',                         'taiji', 'xuan', 6, 3),
('《圆转论》',   '至刚易折，圆转者久。',                         'taiji', 'huang', 2, 4),
('《万化归一》', '千招万式，尽头是一个圆。',                     'taiji', 'di',   2, 5),
('太极玉璧',     '璧上阴阳流转，参悟圆融之理。',                 'taiji', 'tian', 9, 6),
('圆转石',       '握之掌心自转，静心之石。',                     'taiji', 'xuan', 9, 7),
('两仪砂',       '阴阳两色的细砂，布两仪阵之基。',               'taiji', 'huang', 9, 8),
/* ---- 阴阳 ---- */
('阴阳互济功',   '阴阳二气互为其根，阴阳至高法门。',             'yinyang', 'tian', 4, 1),
('消长指',       '指分晦明，一指之内判阴阳。',                   'yinyang', 'di',   6, 2),
('双鱼映月诀',   '月下修行，映月观心。',                         'yinyang', 'xuan', 4, 3),
('《负阴抱阳》', '孤阳不生，独阴不长。',                         'yinyang', 'huang', 2, 4),
('《消长之衡》', '盛极必衰，衡者恒存。',                         'yinyang', 'di',   2, 5),
('阴阳双鱼佩',   '佩分两半，合则气机圆满。',                     'yinyang', 'tian', 9, 6),
('晦朔草',       '晦日朔日各开一花的奇草。',                     'yinyang', 'xuan', 8, 7),
('子午露',       '子午二时收取的露水，调和气机。',               'yinyang', 'huang', 8, 8),
/* ---- 首页 mock 库（雾山秘录等，保证与前端 mock 搜索结果一致） ---- */
('雾山秘录 · 卷一',     '五山武星之火卷，载炽烈心明之法。',     'huo',    'di',   4, 11),
('金石锻造录',           '锋锐决断，金行兵刃锻造全解。',         'jin',    'huang', 2, 12),
('山涧灵草图鉴',         '生发仁厚，草木图谱与采撷时令。',       'mu',     'xuan', 8, 13),
('五行阵法纲要',         '灵动智略，水行布阵之纲要。',           'shui',   'di',   7, 14),
('厚土寻脉记',           '厚重承载，寻地脉走向之实录。',         'tu',     'huang', 2, 15),
('淬火之术十二式',       '以火淬锋，十二式口诀与图解。',         'huo',    'xuan', 6, 16),
('雾山五行总纲',         '五山武星总纲，入境者必读。',           'huo',    'tian', 2, 17),
('御风身法 · 行无踪',    '疾速无形，踏叶不惊的轻功总诀。',       'feng',   'di',   5, 18),
('紫雷诀',               '震赫果决，引九霄雷气入体的法门。',     'lei',    'xuan', 4, 19),
('掣电心法',             '迅烈明澈，以电悟道的观想法门。',       'dian',   'xuan', 4, 20),
('太极图谱',             '圆融万化，阴阳鱼推手与套路图解。',     'taiji',  'xuan', 7, 21),
('阴阳双鱼石拓本',       '相济消长，山中古碑的双鱼拓片。',       'yinyang','xuan', 9, 22);

-- 补齐品阶矩阵（2026-10-16）：保证任意 元素×品阶 组合至少 1 条，
-- 圣品原先全空、凡品仅金创药 1 条，点筛选会出现「阁中无此藏」。
INSERT INTO wushan_item (title, summary, element, grade_key, category_id, sort) VALUES
/* ---- 凡品（凡俗启蒙之物，每个元素各 1 条） ---- */
('《生火十二诀》',        '最寻常的引火口诀，灶膛火房皆可用。',     'huo',    'fan',   6, 31),
('山间青笋',              '寻常时蔬，充饥解乏，采药人的口粮。',     'mu',     'fan',   8, 32),
('净水砂囊',              '粗砂滤水的皮囊，行路常备。',             'shui',   'fan',   9, 33),
('《夯歌集》',            '筑墙打夯的号子，练气自打夯起。',         'tu',     'fan',   2, 34),
('追风蒲扇',              '凡人纳凉的蒲扇，练听风的第一步。',       'feng',   'fan',   11, 35),
('《数雷篇》',            '雷夜数雷记数，练的是心静。',             'lei',    'fan',   2, 36),
('《辨光小识》',          '分辨天光与电光的入门小册。',             'dian',   'fan',   2, 37),
('晨操十式',              '山民晨练的十式体操，太极由此入门。',     'taiji',  'fan',   5, 38),
('《时辰歌诀》',          '歌诀记昼夜时辰，阴阳启蒙读物。',         'yinyang','fan',   2, 39),
/* ---- 圣品（每个元素各 1 条，与前端 GRADES 圣品筛选呼应） ---- */
('《雾山火经 · 无字卷》', '火行至典，无字而心传，历代掌峰亲授。',   'huo',    'sheng', 4, 41),
('万兵朝宗诀',            '天下兵刃气机归宗一诀，金行圣典。',       'jin',    'sheng', 6, 42),
('建木神种',              '传说建木的一粒种子，落地生根千里。',     'mu',     'sheng', 9, 43),
('沧溟真解',              '万水归海的真解，观之可通水性本源。',     'shui',   'sheng', 4, 44),
('《厚德载物 · 山经》',   '土行圣典，载物配德，非大毅力者不可读。', 'tu',     'sheng', 2, 45),
('罡风轮转图',            '天罡轮转之图，参透者风行万里。',         'feng',   'sheng', 5, 46),
('九霄雷帝玺',            '传说雷帝遗玺，掌雷罚之权柄。',           'lei',    'sheng', 11, 47),
('掣电神行符',            '一符贴身，身法快过电光。',               'dian',   'sheng', 11, 48),
('《太极本源论》',        '万化归一之论，太极一脉的源头经卷。',     'taiji',  'sheng', 2, 49),
('太初阴阳葫',            '葫中自成阴阳二气，天地未分之象。',       'yinyang','sheng', 11, 50);

-- ------------------------------------------------------------
-- 4) 顶部导航
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_nav;
CREATE TABLE wushan_nav (
  id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  nav_key     VARCHAR(32) NOT NULL                COMMENT '导航 key',
  label       VARCHAR(32) NOT NULL                COMMENT '中文名',
  en          VARCHAR(64) DEFAULT NULL            COMMENT '英文名',
  sort        INT         DEFAULT 0               COMMENT '排序',
  status      TINYINT     DEFAULT 1               COMMENT '状态：1=启用 0=隐藏',
  PRIMARY KEY (id),
  UNIQUE KEY uk_nav_key (nav_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='首页导航';

INSERT INTO wushan_nav (nav_key, label, en, sort) VALUES
  ('guanshan', '观山', 'scenery',   1),
  -- 2026-10-23 改名：藏经 → 宝库（前端导航 key 同步 baoku）
  ('baoku',    '宝库', 'treasury',  2),
  ('lundao',   '论道', 'discourse', 3),
  ('shiji',    '市集', 'market',    4);

-- 已建库跑过旧种子的，执行下面这条在线改名（无需重建表）：
-- UPDATE wushan_nav SET nav_key='baoku', label='宝库', en='treasury' WHERE nav_key='cangjing';

-- ------------------------------------------------------------
-- 5) 热门搜索词
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_hot_keyword;
CREATE TABLE wushan_hot_keyword (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  keyword      VARCHAR(64)  NOT NULL                COMMENT '热词',
  element      VARCHAR(16)  DEFAULT NULL            COMMENT '五行 key',
  search_count INT          DEFAULT 0               COMMENT '搜索次数（可由搜索日志统计刷新）',
  sort         INT          DEFAULT 0               COMMENT '排序',
  status       TINYINT      DEFAULT 1               COMMENT '状态：1=展示 0=隐藏',
  PRIMARY KEY (id),
  UNIQUE KEY uk_keyword (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='热门搜索词';

INSERT INTO wushan_hot_keyword (keyword, element, search_count, sort) VALUES
  ('五行阵法', 'shui',   520, 1),
  ('雾山秘录', 'huo',    486, 2),
  ('淬火之术', 'huo',    402, 3),
  ('山涧灵草', 'mu',     377, 4),
  ('金石锻造', 'jin',    301, 5),
  ('厚土寻脉', 'tu',     288, 6),
  ('御风身法', 'feng',   250, 7),
  ('紫雷诀',   'lei',    233, 8),
  ('掣电心法', 'dian',   210, 9),
  ('太极图谱', 'taiji',  198, 10),
  ('阴阳双鱼', 'yinyang',175, 11);

-- ------------------------------------------------------------
-- 6) 山门公告（跑马灯）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_announcement;
CREATE TABLE wushan_announcement (
  id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  text        VARCHAR(512) NOT NULL                COMMENT '公告内容',
  sort        INT          DEFAULT 0               COMMENT '排序',
  status      TINYINT      DEFAULT 1               COMMENT '状态：1=展示 0=隐藏',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='山门公告';

INSERT INTO wushan_announcement (text, sort) VALUES
  ('今日卯时东岭雾散，观山道上可见云海，宜出行。',                 1),
  ('宝库新录《五行总纲》批注三卷，山友可借阅。',                 2),
  ('论道台今晚戌时开讲：火行「心明」之辨。',                       3),
  ('市集三日内以物易物，免山税。',                                 4);

-- ------------------------------------------------------------
-- 7) 山中四时图卷
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_scene;
CREATE TABLE wushan_scene (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  scene_key   VARCHAR(32) NOT NULL                COMMENT '图卷 key',
  title       VARCHAR(64) NOT NULL                COMMENT '图卷名',
  en          VARCHAR(64) DEFAULT NULL            COMMENT '英文名',
  season      VARCHAR(8)  DEFAULT NULL            COMMENT '季节：春/夏/秋/冬',
  summary     VARCHAR(512) DEFAULT NULL           COMMENT '图卷描述',
  tint        VARCHAR(16) DEFAULT NULL            COMMENT '主色调（前端展示）',
  sort        INT         DEFAULT 0               COMMENT '排序',
  status      TINYINT     DEFAULT 1               COMMENT '状态：1=展示 0=隐藏',
  PRIMARY KEY (id),
  UNIQUE KEY uk_scene_key (scene_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='山中四时图卷';

INSERT INTO wushan_scene (scene_key, title, en, season, summary, tint, sort) VALUES
  ('spring', '春山新霁', 'spring', '春', '草木生发，山色初醒，涧水初涨。', '#4caf6e', 1),
  ('summer', '夏山听雨', 'summer', '夏', '骤雨过岭，雾气蒸腾，雷走云低。', '#3a8fd0', 2),
  ('autumn', '秋山落叶', 'autumn', '秋', '金风肃杀，层林尽染，雁字南归。', '#e8c372', 3),
  ('winter', '冬山覆雪', 'winter', '冬', '千山素裹，炉火围坐，星火可亲。', '#ece4d2', 4);

-- ------------------------------------------------------------
-- 8) 山友论道榜
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_discourse;
CREATE TABLE wushan_discourse (
  id          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  name        VARCHAR(32)  NOT NULL                COMMENT '山友名号',
  element     VARCHAR(16)  DEFAULT NULL            COMMENT '五行 key',
  quote       VARCHAR(255) DEFAULT NULL            COMMENT '论道语录',
  zan         INT          DEFAULT 0               COMMENT '点赞数',
  sort        INT          DEFAULT 0              COMMENT '排序',
  status      TINYINT      DEFAULT 1              COMMENT '状态：1=上榜 0=下榜',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='山友论道榜';

INSERT INTO wushan_discourse (name, element, quote, zan, sort) VALUES
  ('焚舟子',     'huo',    '火不焚舟，舟自渡；心不明镜，镜自昏。', 128, 1),
  ('淬金生',     'jin',    '金生于土而锋于砺，人成于事而明于过。', 96,  2),
  ('溪山客',     'shui',   '水行不争先，争的是滔滔不绝。',         87,  3),
  ('扶桑叟',     'mu',     '草木一秋，尚且向光而立。',             64,  4),
  ('厚土翁',     'tu',     '土不言语，载万物而行。',               52,  5),
  ('听风散人',   'feng',   '风过千山，不留一物，故能过千山。',     47,  6),
  ('掌雷真人',   'lei',    '雷起于沉默，决于一线。',               39,  7),
  ('掣电行者',   'dian',   '电光一瞬，照见本心。',                 33,  8),
  ('圆转师太',   'taiji',  '至刚易折，至柔无形，圆转者久。',       28,  9),
  ('负阴君',     'yinyang','孤阳不生，独阴不长，相济者昌。',       21,  10);

-- ------------------------------------------------------------
-- 9) 搜索日志（热词统计 / 检索分析用）
-- ------------------------------------------------------------
DROP TABLE IF EXISTS wushan_search_log;
CREATE TABLE wushan_search_log (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       DEFAULT NULL            COMMENT '搜索者 id（未登录为空）',
  username    VARCHAR(32)  DEFAULT NULL            COMMENT '搜索者名号',
  keyword     VARCHAR(128) NOT NULL                COMMENT '搜索词',
  element     VARCHAR(16)  DEFAULT NULL            COMMENT '筛选五行',
  grade_key   VARCHAR(16)  DEFAULT NULL            COMMENT '筛选品阶',
  category_id BIGINT       DEFAULT NULL            COMMENT '筛选分类',
  result_cnt  INT          DEFAULT 0               COMMENT '命中条数',
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
  PRIMARY KEY (id),
  KEY idx_keyword (keyword),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='搜索日志';
