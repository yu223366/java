-- ============================================================
--  雾山五行 · 第二轮升级 A：宝库物品体系 + 市集三铺（可重复执行）
--    1) 新增分类：丹药材料 / 炼器材料 / 阵法材料 / 阵法 / 武器 / 法宝 / 市集专供
--    2) wushan_item 补 详细介绍 detail / 获取途径 acquire / 用途 effect / 品质基准价
--    3) 新分类物品种子（宝库页签可见）
--    4) wushan_market_item 补 详情 / 摊位 shop_key / 背包引用 item_ref / 品质基准价
--    5) 市集专供新物品（宝库查不到，只在三个材料铺有售），按品质用灵石定价
--
--  品质基准价：凡 10 · 黄 30 · 玄 100 · 地 300 · 天 900 · 圣 2700（灵石）
-- ============================================================
USE wushan;

-- ------------------------------------------------------------
-- 1) 分类扩充
--    注意：已存在 cat_key='zhenfa'（功法下的二级「阵法」），故器物类另用 zhenfa_wu
-- ------------------------------------------------------------
INSERT INTO wushan_category (cat_key, name, en, parent_id, cat_level, sort, status)
SELECT 'dan_cai', '丹药材料', 'pill material', 3, 2, 5, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan_category WHERE cat_key = 'dan_cai');
INSERT INTO wushan_category (cat_key, name, en, parent_id, cat_level, sort, status)
SELECT 'qi_cai', '炼器材料', 'forge material', 3, 2, 6, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan_category WHERE cat_key = 'qi_cai');
INSERT INTO wushan_category (cat_key, name, en, parent_id, cat_level, sort, status)
SELECT 'zhen_cai', '阵法材料', 'array material', 3, 2, 7, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan_category WHERE cat_key = 'zhen_cai');
INSERT INTO wushan_category (cat_key, name, en, parent_id, cat_level, sort, status)
SELECT 'zhenfa_wu', '阵法', 'formation', 3, 2, 8, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan_category WHERE cat_key = 'zhenfa_wu');
INSERT INTO wushan_category (cat_key, name, en, parent_id, cat_level, sort, status)
SELECT 'wuqi', '武器', 'weapon', 3, 2, 9, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan_category WHERE cat_key = 'wuqi');
INSERT INTO wushan_category (cat_key, name, en, parent_id, cat_level, sort, status)
SELECT 'fabao', '法宝', 'treasure', 3, 2, 10, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan_category WHERE cat_key = 'fabao');
-- 市集专供：宝库页签不列这一分类，因此「宝库没有、市集才有」
INSERT INTO wushan_category (cat_key, name, en, parent_id, cat_level, sort, status)
SELECT 'shiji_only', '市集专供', 'market only', 3, 2, 11, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM wushan_category WHERE cat_key = 'shiji_only');

-- ------------------------------------------------------------
-- 2) 幂等加列：物品详情 / 获取途径 / 用途 / 品质基准价
-- ------------------------------------------------------------
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_item ADD COLUMN detail VARCHAR(1024) DEFAULT NULL COMMENT ''详细介绍''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_item' AND COLUMN_NAME='detail');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_item ADD COLUMN acquire VARCHAR(255) DEFAULT NULL COMMENT ''获取途径''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_item' AND COLUMN_NAME='acquire');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_item ADD COLUMN effect VARCHAR(255) DEFAULT NULL COMMENT ''用途与功效''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_item' AND COLUMN_NAME='effect');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_item ADD COLUMN grade_price INT NOT NULL DEFAULT 0 COMMENT ''品质基准价（灵石）''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_item' AND COLUMN_NAME='grade_price');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 所有条目按品阶写入基准价（市集、宗门宝库定价都以此为锚）
UPDATE wushan_item SET grade_price = CASE grade_key
  WHEN 'fan' THEN 10 WHEN 'huang' THEN 30 WHEN 'xuan' THEN 100
  WHEN 'di' THEN 300 WHEN 'tian' THEN 900 WHEN 'sheng' THEN 2700 ELSE 30 END
WHERE grade_price = 0 OR grade_price IS NULL;

-- 老条目补「详细介绍 / 获取途径 / 用途」：按分类给不同出处，正文含本名与简介，件件不同
UPDATE wushan_item SET
  detail = CONCAT('【', title, '】', IFNULL(summary, ''),
    ' 雾山旧录载：此物属',
    CASE category_id
      WHEN 4 THEN '心法一脉，重在养气明心，久习可令内息自生。'
      WHEN 5 THEN '身法一脉，讲的是步随身转、意在形先。'
      WHEN 6 THEN '术法一脉，施为一式皆有所本，不可贪多。'
      WHEN 7 THEN '阵法一脉，需先明方位生克，再谈布设。'
      WHEN 2 THEN '心得杂录，多为山中前辈片语，读之不必尽信。'
      WHEN 8 THEN '灵药之属，采撷有时、炮制有法，生用多半暴殄。'
      WHEN 9 THEN '炼材之属，质地随品阶而异，入炉前须先辨火候。'
      WHEN 10 THEN '丹药之属，一炉成败全在文武火之间。'
      WHEN 11 THEN '法器之属，器成需养，久用方与主人气机相合。'
      ELSE '山中所出，可入囊、可入炉、可易物。'
    END,
    ' 品阶', IFNULL((SELECT g.name FROM wushan_grade g WHERE g.grade_key = wushan_item.grade_key), '凡品'),
    '，基准价约 ', grade_price, ' 灵石。'),
  acquire = CASE category_id
    WHEN 4 THEN '宗门宝库贡献兑换 · 秘境石室参悟 · 拍卖行偶见'
    WHEN 5 THEN '宗门宝库贡献兑换 · 秘境拓印 · 山友寄售'
    WHEN 6 THEN '宗门宝库贡献兑换 · 秘境遗府 · 山友寄售'
    WHEN 7 THEN '阵法炉炼制 · 市集阵法材料铺 · 宗门宝库'
    WHEN 2 THEN '观山论道拾遗 · 宗门宝库贡献兑换'
    WHEN 8 THEN '市集丹药材料铺 · 秘境灵泉采撷 · 山友寄售'
    WHEN 9 THEN '市集炼器材料铺 · 秘境遗府 · 山友寄售'
    WHEN 10 THEN '炼丹炉炼制 · 市集药铺 · 宗门宝库'
    WHEN 11 THEN '炼器炉炼制 · 拍卖行 · 宗门宝库'
    ELSE '市集购买 · 宗门宝库兑换 · 秘境所得'
  END,
  effect = CASE category_id
    WHEN 8 THEN '炼丹主材 / 服食培元'
    WHEN 9 THEN '炼器、布阵通用材料'
    WHEN 10 THEN '服食：疗伤、凝神、破关'
    WHEN 11 THEN '佩戴或御使：攻守辅助'
    WHEN 7 THEN '布设阵法之图谱'
    ELSE '参悟研读 / 收藏 / 兑换'
  END
WHERE detail IS NULL OR detail = '';

-- ------------------------------------------------------------
-- 3) 新分类物品种子（宝库可见）
--    category_id 用子查询按 cat_key 取，避免依赖自增 id
-- ------------------------------------------------------------
INSERT INTO wushan_item (title, summary, element, grade_key, category_id, sort, detail, acquire, effect, grade_price, status)
SELECT x.t, x.s, x.e, x.g,
       (SELECT id FROM wushan_category WHERE cat_key = x.c),
       x.so, x.d, x.a, x.f, x.p, x.st
FROM (
  SELECT '百年何首乌' t, '药龄百年，补气第一。' s, 'mu' e, 'xuan' g, 'dan_cai' c, 1 so,
    '【百年何首乌】生于青木林深处背阴湿壁，百年方成，切开可见云纹。入炉需文火慢焙，猛火则药性尽散，是培元类丹方的主药。' d,
    '市集丹药材料铺（玄品 100 灵石）· 秘境长生谷采撷 · 山友寄售' a, '炼丹主材：培元、固本' f, 100 p, 1 st
  UNION ALL SELECT '赤焰灵菇', '火气内蕴，炼火丹必备。', 'huo', 'huang', 'dan_cai', 2,
    '【赤焰灵菇】只在焚火秘境温泉边生长，伞盖赤红，摘下后三日内药性不散。与寒性药材同炉可平火毒。',
    '市集丹药材料铺（黄品 30 灵石）· 秘境萤火洞', '炼丹主材：火行丹药', 30, 1
  UNION ALL SELECT '玄冰莲子', '寒潭所结，凝神定魄。', 'shui', 'di', 'dan_cai', 3,
    '【玄冰莲子】寒潭千年不化之冰中结出的莲实，握之刺骨。研末入丹可压走火入魔之躁气，是凝神丹的定鼎之材。',
    '市集丹药材料铺（地品 300 灵石）· 秘境寒潭底', '炼丹主材：凝神、定魄', 300, 1
  UNION ALL SELECT '雷纹芝', '雷击之后方生，药性烈。', 'lei', 'tian', 'dan_cai', 4,
    '【雷纹芝】落雷原古木遭雷击而不死，次年根旁生此芝，表面有天然雷纹。药性极烈，非破障类丹方不可轻用。',
    '秘境落雷原奇遇 · 拍卖行 · 山友寄售', '炼丹主材：破障、淬体', 900, 1
  UNION ALL SELECT '寻常艾草', '最寻常的灸疗药材。', 'mu', 'fan', 'dan_cai', 5,
    '【寻常艾草】山道两旁随处可见，端午采者最佳。多作练手药材，也是金创药的底料。',
    '市集丹药材料铺（凡品 10 灵石）· 观山道旁', '炼丹辅料：灸疗、止血', 10, 1
  UNION ALL SELECT '庚金砂', '锻造兵刃的粗料。', 'jin', 'huang', 'qi_cai', 1,
    '【庚金砂】断剑冢外溪底淘出的金砂，颗粒不匀却极耐火。掺入凡铁可提锋锐，是炼器入门第一料。',
    '市集炼器材料铺（黄品 30 灵石）· 秘境断剑冢', '炼器材料：提锋、增锐', 30, 1
  UNION ALL SELECT '玄铁母', '一炉之骨，重逾常铁十倍。', 'jin', 'di', 'qi_cai', 2,
    '【玄铁母】地肺石窟深处所出，色黑如墨，入水即沉。须以地心火连煅九日方熔，成器后不易卷刃。',
    '市集炼器材料铺（地品 300 灵石）· 秘境地肺石窟', '炼器主材：器骨', 300, 1
  UNION ALL SELECT '离火晶', '内蕴地火，淬火不熄。', 'huo', 'xuan', 'qi_cai', 3,
    '【离火晶】火山口凝成的赤晶，置于炉中可省三分柴火。淬火时投入一枚，器身自带火纹。',
    '市集炼器材料铺（玄品 100 灵石）· 秘境焚火秘境', '炼器材料：淬火、附火', 100, 1
  UNION ALL SELECT '风灵羽', '轻若无物，御器可加速。', 'feng', 'tian', 'qi_cai', 4,
    '【风灵羽】风灵蜕羽，百年一落。嵌于剑柄或扇骨，可令御器之速倍增，炼器师视若珍宝。',
    '秘境风眼长廊 · 拍卖行 · 山友寄售', '炼器材料：轻身、提速', 900, 1
  UNION ALL SELECT '粗麻符纸', '画符练手，最寻常。', 'tu', 'fan', 'qi_cai', 5,
    '【粗麻符纸】粗麻浆所造，吸墨而不洇。初学画符者一日可耗百张，铺子里按刀卖。',
    '市集炼器材料铺（凡品 10 灵石）', '炼器辅料：符箓底纸', 10, 1
  UNION ALL SELECT '两仪砂', '黑白二色，布阵定基。', 'taiji', 'huang', 'zhen_cai', 1,
    '【两仪砂】阴阳两色细砂，撒地自成双鱼之形。两仪阵、太极阵皆以它定基，缺之则阵眼不稳。',
    '市集阵法材料铺（黄品 30 灵石）· 太极幻境', '阵法材料：定阵眼', 30, 1
  UNION ALL SELECT '引雷针', '凡铁长针，引雷入阵。', 'lei', 'fan', 'zhen_cai', 2,
    '【引雷针】凡铁锻的细针，一端尖一端钝。插于阵眼可引天雷，雷阵最基础的物件。',
    '市集阵法材料铺（凡品 10 灵石）', '阵法材料：引雷、导气', 10, 1
  UNION ALL SELECT '定风旗杆', '罡风不折，阵旗之骨。', 'feng', 'xuan', 'zhen_cai', 3,
    '【定风旗杆】青岚绝壁的刚性竹所制，八级罡风中不断。阵旗用它，风阵方能立得住。',
    '市集阵法材料铺（玄品 100 灵石）· 秘境青岚绝壁', '阵法材料：阵旗、抗风', 100, 1
  UNION ALL SELECT '息壤砂', '一撮可成丘，阵基自固。', 'tu', 'tian', 'zhen_cai', 4,
    '【息壤砂】息壤碾成的细砂，遇灵气自生，撒一圈便是天然阵基。山门大阵多用此物，价高且不常有。',
    '秘境息壤秘窟奇遇 · 拍卖行 · 宗门宝库', '阵法材料：阵基、固界', 900, 1
  UNION ALL SELECT '阵纹墨', '以灵兽血研，画阵不褪。', 'shui', 'di', 'zhen_cai', 5,
    '【阵纹墨】寒潭玉液调灵兽血所研，画于石上百年不褪。地品以上阵图非它不可。',
    '市集阵法材料铺（地品 300 灵石）· 山友寄售', '阵法材料：绘阵纹', 300, 1
  UNION ALL SELECT '庚金百斩阵图', '剑气成阵，百斩一意。', 'jin', 'tian', 'zhenfa_wu', 1,
    '【庚金百斩阵图】以三十六柄断剑为桩，按百斩诀方位布设，阵成则剑气自生。图为残卷，需阵师自行补足三处阵眼。',
    '阵法炉炼制 · 宗门宝库贡献兑换 · 拍卖行', '可布设攻伐大阵', 900, 1
  UNION ALL SELECT '两仪归元阵', '阴阳互济，守御第一。', 'yinyang', 'di', 'zhenfa_wu', 2,
    '【两仪归元阵】双鱼方位各设一阵眼，攻守相生，入阵者力越猛反噬越重。山门护界常用。',
    '阵法炉炼制 · 宗门宝库贡献兑换', '可布设守御大阵', 300, 1
  UNION ALL SELECT '离火焚天阵盘', '阵纹刻于盘，随地可展。', 'huo', 'xuan', 'zhenfa_wu', 3,
    '【离火焚天阵盘】巴掌大的赤铜盘，刻七十二道火纹，注入灵石即可展开丈许火阵。便于携带，威力有限。',
    '阵法炉炼制 · 市集法器铺 · 山友寄售', '便携阵盘，一次性布阵', 100, 1
  UNION ALL SELECT '小迷踪阵旗', '五旗插地，迷径自生。', 'feng', 'huang', 'zhenfa_wu', 4,
    '【小迷踪阵旗】五面青旗按梅花位插下，十丈内雾气自生。多用于秘境试炼，非杀阵。',
    '阵法炉炼制 · 市集法器铺', '布设迷阵，困而不伤', 30, 1
  UNION ALL SELECT '青锋剑', '三尺青锋，削铁如泥。', 'jin', 'xuan', 'wuqi', 1,
    '【青锋剑】庚金砂与玄铁母合锻，剑身有水纹。剑修入门第一器，用久了能与主人气机相合。',
    '炼器炉炼制 · 市集法器铺（玄品 100 灵石）', '近战兵刃，金行加成', 100, 1
  UNION ALL SELECT '断岳斩', '重刀无锋，一斩开山。', 'tu', 'di', 'wuqi', 2,
    '【断岳斩】厚背重刀，重逾百斤，不追求锋锐，靠势压人。土行修士惯用。',
    '炼器炉炼制 · 宗门宝库贡献兑换', '重型兵刃，破甲', 300, 1
  UNION ALL SELECT '掣电梭', '掷出即至，肉眼难追。', 'dian', 'tian', 'wuqi', 3,
    '【掣电梭】电光石髓为芯，梭身细如手指，掷出带一线电光。暗器中的极品，一梭难求。',
    '炼器炉炼制 · 拍卖行 · 宗门宝库', '投掷暗器，先手', 900, 1
  UNION ALL SELECT '柴刀', '山民砍柴之物。', 'jin', 'fan', 'wuqi', 4,
    '【柴刀】铁匠随手打的粗刀，刃口常有豁。虽不入品，劈柴、防身、练力都够用。',
    '市集杂货铺（凡品 10 灵石）', '凡兵，练力用', 10, 1
  UNION ALL SELECT '赤焰葫芦', '内蕴一缕地心火。', 'huo', 'di', 'fabao', 1,
    '【赤焰葫芦】离火晶为胆、赤铜为壳，拔塞便吐一缕地心火，可放可收。炼丹师常拿来当随身炉火。',
    '炼器炉炼制 · 市集法器铺 · 拍卖行', '御使火气，辅助炼丹', 300, 1
  UNION ALL SELECT '阴阳双鱼佩', '佩分两半，合则气机圆满。', 'yinyang', 'tian', 'fabao', 2,
    '【阴阳双鱼佩】一佩两半，两人各执其一，合则气机互济。传说同门各佩半枚，千里可感对方安危。',
    '炼器炉炼制 · 宗门宝库贡献兑换 · 拍卖行', '佩饰法宝，气机互济', 900, 1
  UNION ALL SELECT '藏水青囊', '囊中自有一汪寒潭。', 'shui', 'xuan', 'fabao', 3,
    '【藏水青囊】水云纱缝成的青囊，内藏一小片寒潭水境，旱地行路不愁无水。',
    '炼器炉炼制 · 市集法器铺', '储物类法宝，取水', 100, 1
  UNION ALL SELECT '避风铜铃', '风起铃动，先人一步知险。', 'feng', 'huang', 'fabao', 4,
    '【避风铜铃】铃舌以定风旗杆余料制成，风未至而铃先响。夜行秘境者多挂一枚在腰间。',
    '炼器炉炼制 · 市集法器铺', '预警类法宝', 30, 1
  UNION ALL SELECT '阵法纲要残页', '阵眼方位总纲，缺三页。', 'taiji', 'huang', 'zhenfa_wu', 9,
    '【阵法纲要残页】雾山旧藏阵法总纲的残页，前四页讲方位生克，后三页已佚。初学阵法者多以此入门。' ,
    '宗门宝库贡献兑换 · 市集阵法材料铺', '阵法研读', 30, 1
) x
WHERE NOT EXISTS (SELECT 1 FROM wushan_item i WHERE i.title = x.t);

-- ------------------------------------------------------------
-- 4) 市集表补列
-- ------------------------------------------------------------
SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_market_item ADD COLUMN detail VARCHAR(1024) DEFAULT NULL COMMENT ''详细介绍''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_market_item' AND COLUMN_NAME='detail');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_market_item ADD COLUMN acquire VARCHAR(255) DEFAULT NULL COMMENT ''获取途径''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_market_item' AND COLUMN_NAME='acquire');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_market_item ADD COLUMN shop_key VARCHAR(16) NOT NULL DEFAULT ''zahuo'' COMMENT ''摊位''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_market_item' AND COLUMN_NAME='shop_key');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_market_item ADD COLUMN item_ref BIGINT DEFAULT NULL COMMENT ''对应背包物品 id''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_market_item' AND COLUMN_NAME='item_ref');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ddl = (SELECT IF(COUNT(*) = 0,
  'ALTER TABLE wushan_market_item ADD COLUMN grade_price INT NOT NULL DEFAULT 0 COMMENT ''品质基准价''', 'SELECT 1')
  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='wushan' AND TABLE_NAME='wushan_market_item' AND COLUMN_NAME='grade_price');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 老货补详情 / 摊位 / 品质基准价 / 背包引用
UPDATE wushan_market_item SET
  grade_price = CASE grade_key
    WHEN 'fan' THEN 10 WHEN 'huang' THEN 30 WHEN 'xuan' THEN 100
    WHEN 'di' THEN 300 WHEN 'tian' THEN 900 WHEN 'sheng' THEN 2700 ELSE 30 END,
  shop_key = CASE category_id
    WHEN 8 THEN 'dan_cai' WHEN 9 THEN 'qi_cai' WHEN 10 THEN 'danyao'
    WHEN 11 THEN 'faqi' ELSE 'zahuo' END,
  detail = CONCAT('【', title, '】', IFNULL(summary, ''),
    ' 摊主按品阶定价：', CASE grade_key
      WHEN 'fan' THEN '凡品' WHEN 'huang' THEN '黄品' WHEN 'xuan' THEN '玄品'
      WHEN 'di' THEN '地品' WHEN 'tian' THEN '天品' WHEN 'sheng' THEN '圣品' ELSE '凡品' END,
    '，基准价 ', CASE grade_key
      WHEN 'fan' THEN 10 WHEN 'huang' THEN 30 WHEN 'xuan' THEN 100
      WHEN 'di' THEN 300 WHEN 'tian' THEN 900 WHEN 'sheng' THEN 2700 ELSE 30 END,
    ' 灵石；此价随货色上下浮动，购得即入背包，可作炼丹、炼器、布阵之用。'),
  acquire = '雾山市集 · 按品阶以灵石购买（购得入背包）',
  item_ref = (SELECT id FROM wushan_item WHERE title = wushan_market_item.title ORDER BY id LIMIT 1)
WHERE detail IS NULL OR detail = '';

-- ------------------------------------------------------------
-- 5) 市集专供新物品（宝库里搜不到；三铺各 4~5 件，按品质定价）
-- ------------------------------------------------------------
INSERT INTO wushan_market_item
  (title, summary, element, grade_key, category_id, price, stock, seller, sold, status,
   detail, acquire, shop_key, item_ref, grade_price)
SELECT x.t, x.s, x.e, x.g,
       (SELECT id FROM wushan_category WHERE cat_key = 'shiji_only'),
       x.p, x.stk, x.seller, 0, 1, x.d, x.a, x.shop,
       (SELECT id FROM wushan_item WHERE title = x.ref ORDER BY id LIMIT 1), x.gp
FROM (
  SELECT '九蒸何首乌' t, '九蒸九晒，药性尽出。' s, 'mu' e, 'di' g, 300 p, 8 stk, '雾山药庐' seller,
    '【九蒸九晒何首乌】市集药庐独门炮制，百年何首乌经九次蒸晒，燥性尽去，直接入炉可省一半火候。宝库只收原株，不收炮制品。' d,
    '丹药材料铺 · 按地品 300 灵石购买' a, 'dan_cai' shop, '百年何首乌' ref, 300 gp
  UNION ALL SELECT '蜜炼赤焰膏', '灵菇熬膏，火性温而不烈。', 'huo', 'xuan', 100, 14, '雾山药庐',
    '【蜜炼赤焰膏】赤焰灵菇以山蜜慢熬成膏，火性温和，最宜给初学火行的山友作药引。宝库无此炮制品。',
    '丹药材料铺 · 按玄品 100 灵石购买', 'dan_cai', '赤焰灵菇', 100
  UNION ALL SELECT '寒潭莲心粉', '玄冰莲子研末，即取即用。', 'shui', 'tian', 900, 3, '半盏道人',
    '【寒潭莲心粉】玄冰莲子去壳研末，以玉钵低温碾成，凝神丹最贵的一味。铺子每月只出三罐。',
    '丹药材料铺 · 按天品 900 灵石购买', 'dan_cai', '玄冰莲子', 900
  UNION ALL SELECT '陈年艾绒', '三年陈艾，灸疗上品。', 'mu', 'fan', 10, 90, '雾山山货铺',
    '【陈年艾绒】寻常艾草晒足三年再捣成绒，火力温透，练手最省。宝库只录原草。',
    '丹药材料铺 · 按凡品 10 灵石购买', 'dan_cai', '寻常艾草', 10
  UNION ALL SELECT '雷纹芝切片', '烈性稍减，破障丹可用。', 'lei', 'tian', 900, 2, '驭雷童',
    '【雷纹芝切片】整芝切薄片封蜡，药性减了三分却更好拿捏，破障丹的稳妥之选。',
    '丹药材料铺 · 按天品 900 灵石购买', 'dan_cai', '雷纹芝', 900
  UNION ALL SELECT '精炼庚金锭', '淘洗三遍，杂质尽去。', 'jin', 'xuan', 100, 12, '藏锋居士',
    '【精炼庚金锭】庚金砂淘洗熔铸成锭，一锭抵三包粗砂，炼器时省去一遍熔料功夫。',
    '炼器材料铺 · 按玄品 100 灵石购买', 'qi_cai', '庚金砂', 100
  UNION ALL SELECT '玄铁母坯', '已开炉锻过一回的器坯。', 'jin', 'di', 300, 5, '藏锋居士',
    '【玄铁母坯】玄铁母初锻成坯，捶打去了一半杂质，拿回去只需定型淬火。',
    '炼器材料铺 · 按地品 300 灵石购买', 'qi_cai', '玄铁母', 300
  UNION ALL SELECT '离火晶屑', '碎晶装瓶，淬火一次一瓶。', 'huo', 'huang', 30, 40, '焚舟子',
    '【离火晶屑】整晶切下边角料，效力不及整块，胜在价平。淬一次火用一瓶正好。',
    '炼器材料铺 · 按黄品 30 灵石购买', 'qi_cai', '离火晶', 30
  UNION ALL SELECT '符纸一刀', '百张一刀，练符足用。', 'tu', 'fan', 10, 60, '雾山山货铺',
    '【符纸一刀】粗麻符纸一百张扎成一刀，铺子按刀出货，比零买省三成。',
    '炼器材料铺 · 按凡品 10 灵石购买', 'qi_cai', '粗麻符纸', 10
  UNION ALL SELECT '风灵羽管', '羽管中空，嵌器加速。', 'feng', 'tian', 900, 2, '听风散人',
    '【风灵羽管】整羽最硬的一段羽管，中空而韧，嵌入剑柄可御风提速。',
    '炼器材料铺 · 按天品 900 灵石购买', 'qi_cai', '风灵羽', 900
  UNION ALL SELECT '阵纹墨一锭', '研好的墨锭，加水即画。', 'shui', 'di', 300, 6, '圆转师太',
    '【阵纹墨一锭】阵纹墨压成墨锭，随身带、加水即研，画阵不必再背墨罐。',
    '阵法材料铺 · 按地品 300 灵石购买', 'zhen_cai', '阵纹墨', 300
  UNION ALL SELECT '两仪砂囊', '一囊定一阵，便携。', 'taiji', 'huang', 30, 30, '圆转师太',
    '【两仪砂囊】两仪砂按一阵用量装囊，撒一圈即成阵基，省去称量。',
    '阵法材料铺 · 按黄品 30 灵石购买', 'zhen_cai', '两仪砂', 30
  UNION ALL SELECT '引雷针一匣', '十二枚一匣，雷阵足用。', 'lei', 'fan', 10, 50, '驭雷童',
    '【引雷针一匣】十二枚凡铁针装匣，按十二方位插下便是小雷阵的骨架。',
    '阵法材料铺 · 按凡品 10 灵石购买', 'zhen_cai', '引雷针', 10
  UNION ALL SELECT '定风旗一整套', '五杆成套，插地即稳。', 'feng', 'xuan', 100, 8, '听风散人',
    '【定风旗一整套】五杆旗骨配旗面，按梅花位插下，迷踪阵立刻可立。',
    '阵法材料铺 · 按玄品 100 灵石购买', 'zhen_cai', '定风旗杆', 100
  UNION ALL SELECT '息壤砂一钱', '一钱可固十丈阵基。', 'tu', 'tian', 900, 2, '埋玉人',
    '【息壤砂一钱】息壤砂极难得，铺子按钱出售，一钱可固十丈阵基。山门大阵才用得起。',
    '阵法材料铺 · 按天品 900 灵石购买', 'zhen_cai', '息壤砂', 900
) x
WHERE NOT EXISTS (SELECT 1 FROM wushan_market_item m WHERE m.title = x.t);

-- ------------------------------------------------------------
-- 6) 补齐武器 / 法宝 / 材料分类（首轮种子里与旧条目同名者被去重跳过）
--    以及两件「市集专供」物品，供市集购买后入背包引用
-- ------------------------------------------------------------
INSERT INTO wushan_item (title, summary, element, grade_key, category_id, sort, detail, acquire, effect, grade_price, status)
SELECT x.t, x.s, x.e, x.g, (SELECT id FROM wushan_category WHERE cat_key = x.c), x.so, x.d, x.a, x.f, x.p, 1
FROM (
  SELECT '断水匕' t, '短刃无声，近身一击。' s, 'shui' e, 'xuan' g, 'wuqi' c, 5 so,
    '【断水匕】弱水淬锋，刃薄如纸，入水无声。贴近时一击即走，不耐久战，是水行修士的暗手。' d,
    '炼器炉炼制 · 市集法器铺 · 宗门宝库' a, '近身短刃，水行加成' f, 100 p
  UNION ALL SELECT '撼地锤', '一锤落地，地脉皆震。', 'tu', 'di', 'wuqi', 6,
    '【撼地锤】玄铁母整铸的短柄大锤，落地方圆三丈地脉震动。力士专用，寻常人连举都举不起。',
    '炼器炉炼制 · 宗门宝库贡献兑换', '重兵器，震地破阵', 300
  UNION ALL SELECT '青岚刺', '细长如针，御风而走。', 'feng', 'huang', 'wuqi', 7,
    '【青岚刺】定风旗杆余料磨成的细刺，轻得几乎握不住，掷出随岚气转向。',
    '炼器炉炼制 · 市集法器铺', '投掷兵刃，风行加成', 30
  UNION ALL SELECT '九霄雷矛', '矛尖常带紫电。', 'lei', 'tian', 'wuqi', 8,
    '【九霄雷矛】雷击木心为杆、引雷针为尖，矛出时有紫电缠绕。雷行至高兵刃，一击可裂石。',
    '炼器炉炼制 · 拍卖行 · 宗门宝库', '长兵，雷行加成', 900
  UNION ALL SELECT '引雷幡', '幡展则雷云自聚。', 'lei', 'xuan', 'fabao', 5,
    '【引雷幡】玄铁为杆、符纸为面，展幡片刻雷云自聚。落雷原采药者随身之物，用时须择开阔地。',
    '炼器炉炼制 · 市集法器铺 · 山友寄售', '御使法宝，引雷', 100
  UNION ALL SELECT '照心镜', '照见本心，破幻第一。', 'taiji', 'di', 'fabao', 6,
    '【照心镜】太极玉璧磨成的铜镜，照人不见形，只见心中所执。太极幻境里唯一的保命之物。',
    '炼器炉炼制 · 宗门宝库贡献兑换', '破幻、静心', 300
  UNION ALL SELECT '敛息玉', '佩之气息全无。', 'shui', 'huang', 'fabao', 7,
    '【敛息玉】水云纱裹寒潭玉所制小佩，戴上后气息如水入海，秘境避兽常用。',
    '炼器炉炼制 · 市集法器铺', '隐匿气息', 30
  UNION ALL SELECT '聚灵盏', '一盏可聚一室灵气。', 'mu', 'xuan', 'fabao', 8,
    '【聚灵盏】建木边材雕成的小盏，置于案上，一室灵气缓缓向盏心聚拢，练功时最宜。',
    '炼器炉炼制 · 市集法器铺', '辅助练功，聚灵', 100
  UNION ALL SELECT '御风环', '掷出成环，可载一人。', 'feng', 'tian', 'fabao', 9,
    '【御风环】风灵羽管编成的圆环，掷出后涨大如车轮，可载一人越岭。一炷香后灵气散尽。',
    '炼器炉炼制 · 拍卖行 · 宗门宝库', '飞遁法宝', 900
  UNION ALL SELECT '寒潭淬水', '淬器令其带寒气。', 'shui', 'xuan', 'qi_cai', 9,
    '【寒潭淬水】寒潭底层的一桶活水，淬火时取代寻常井水，器身自带寒气。',
    '市集炼器材料铺（玄品 100 灵石）· 秘境寒潭', '炼器材料：附寒', 100
  UNION ALL SELECT '建木神屑', '建木刨下的碎屑，聚灵。', 'mu', 'sheng', 'qi_cai', 10,
    '【建木神屑】传说建木伐下的一缕碎屑，入器可令器物自行聚灵。千年不过一撮。',
    '秘境奇遇 · 拍卖行', '炼器材料：聚灵', 2700
  UNION ALL SELECT '阴阳阵纹石', '天然双色，可刻阵纹。', 'yinyang', 'di', 'zhen_cai', 6,
    '【阴阳阵纹石】晦明谷所出双色石，一半受墨一半拒墨，专刻阴阳互济之类阵纹。',
    '市集阵法材料铺（地品 300 灵石）· 秘境晦明谷', '阵法材料：刻纹', 300
  UNION ALL SELECT '雷池引脉砂', '导雷入脉，雷阵之引。', 'lei', 'tian', 'zhen_cai', 7,
    '【雷池引脉砂】九霄雷池边凝的黑砂，撒成一线便是雷气通路，大雷阵的引脉之物。',
    '秘境九霄雷池奇遇 · 宗门宝库', '阵法材料：导雷', 900
  UNION ALL SELECT '太极道种', '大道之种，得者可开一宗。', 'taiji', 'sheng', 'shiji_only', 1,
    '【太极道种】雾山市集压箱底的奇物，据说开宗立派者得之可镇山门。宝库不收，只在市集惊鸿一现。',
    '市集 · 圣品 2700 灵石（限量一件）', '镇宗奇物 / 收藏', 2700
  UNION ALL SELECT '九转还魂丹', '传说能起死回生的丹药。', 'mu', 'sheng', 'shiji_only', 2,
    '【九转还魂丹】九炉九转，千年一出。宝库只有丹方残页，成丹只在市集药庐偶现。',
    '市集药庐 · 圣品 2700 灵石', '服食：起死回生', 2700
) x
WHERE NOT EXISTS (SELECT 1 FROM wushan_item i WHERE i.title = x.t);

-- 回填市集物品的背包引用（新补的两件圣品也在其中）
UPDATE wushan_market_item SET
  item_ref = (SELECT id FROM wushan_item WHERE title = wushan_market_item.title ORDER BY id LIMIT 1)
WHERE item_ref IS NULL
  AND EXISTS (SELECT 1 FROM wushan_item WHERE title = wushan_market_item.title);
