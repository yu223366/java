/*
 Navicat Premium Dump SQL

 Source Server         : test
 Source Server Type    : MySQL
 Source Server Version : 80039 (8.0.39)
 Source Host           : localhost:3306
 Source Schema         : wushan

 Target Server Type    : MySQL
 Target Server Version : 80039 (8.0.39)
 File Encoding         : 65001

 Date: 16/10/2026 16:48:33
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for wushan_announcement
-- ----------------------------
DROP TABLE IF EXISTS `wushan_announcement`;
CREATE TABLE `wushan_announcement`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `text` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告内容',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1=展示 0=隐藏',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '山门公告' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_announcement
-- ----------------------------
INSERT INTO `wushan_announcement` VALUES (1, '今日卯时东岭雾散，观山道上可见云海，宜出行。', 1, 1, '2026-10-16 15:53:54');
INSERT INTO `wushan_announcement` VALUES (2, '藏经阁新录《五行总纲》批注三卷，山友可借阅。', 2, 1, '2026-10-16 15:53:54');
INSERT INTO `wushan_announcement` VALUES (3, '论道台今晚戌时开讲：火行「心明」之辨。', 3, 1, '2026-10-16 15:53:54');
INSERT INTO `wushan_announcement` VALUES (4, '市集三日内以物易物，免山税。', 4, 1, '2026-10-16 15:53:54');

-- ----------------------------
-- Table structure for wushan_category
-- ----------------------------
DROP TABLE IF EXISTS `wushan_category`;
CREATE TABLE `wushan_category`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `cat_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类 key（英文，供前端传参）',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名（中文）',
  `en` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '英文名',
  `parent_id` bigint NOT NULL DEFAULT 0 COMMENT '父分类 id：0=顶级',
  `cat_level` tinyint NOT NULL DEFAULT 1 COMMENT '层级：1=一级 2=二级',
  `sort` int NULL DEFAULT 0 COMMENT '排序（小在前）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1=启用 0=下架',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_cat_key`(`cat_key` ASC) USING BTREE,
  INDEX `idx_parent`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '藏经分类（分级）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_category
-- ----------------------------
INSERT INTO `wushan_category` VALUES (1, 'gongfa', '功法', 'technique', 0, 1, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_category` VALUES (2, 'xinde', '心得', 'insight', 0, 1, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_category` VALUES (3, 'ziyuan', '修炼资源', 'resource', 0, 1, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_category` VALUES (4, 'xinfa', '心法', 'mind', 1, 2, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_category` VALUES (5, 'shenfa', '身法', 'movement', 1, 2, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_category` VALUES (6, 'shufa', '术法', 'skill', 1, 2, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_category` VALUES (7, 'zhenfa', '阵法', 'formation', 1, 2, 4, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_category` VALUES (8, 'lingyao', '灵药', 'herb', 3, 2, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_category` VALUES (9, 'liancai', '炼材', 'material', 3, 2, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_category` VALUES (10, 'danyao', '丹药', 'pill', 3, 2, 3, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_category` VALUES (11, 'faqi', '法器', 'artifact', 3, 2, 4, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');

-- ----------------------------
-- Table structure for wushan_discourse
-- ----------------------------
DROP TABLE IF EXISTS `wushan_discourse`;
CREATE TABLE `wushan_discourse`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '山友名号',
  `element` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '五行 key',
  `quote` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '论道语录',
  `zan` int NULL DEFAULT 0 COMMENT '点赞数',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1=上榜 0=下榜',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 20 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '山友论道榜' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_discourse
-- ----------------------------
INSERT INTO `wushan_discourse` VALUES (1, '焚舟子', 'huo', '火不焚舟，舟自渡；心不明镜，镜自昏。', 130, 1, 1);
INSERT INTO `wushan_discourse` VALUES (2, '淬金生', 'jin', '金生于土而锋于砺，人成于事而明于过。', 97, 2, 1);
INSERT INTO `wushan_discourse` VALUES (3, '溪山客', 'shui', '水行不争先，争的是滔滔不绝。', 87, 3, 1);
INSERT INTO `wushan_discourse` VALUES (4, '扶桑叟', 'mu', '草木一秋，尚且向光而立。', 64, 4, 1);
INSERT INTO `wushan_discourse` VALUES (5, '厚土翁', 'tu', '土不言语，载万物而行。', 52, 5, 1);
INSERT INTO `wushan_discourse` VALUES (6, '听风散人', 'feng', '风过千山，不留一物，故能过千山。', 47, 6, 1);
INSERT INTO `wushan_discourse` VALUES (7, '掌雷真人', 'lei', '雷起于沉默，决于一线。', 39, 7, 1);
INSERT INTO `wushan_discourse` VALUES (8, '掣电行者', 'dian', '电光一瞬，照见本心。', 33, 8, 1);
INSERT INTO `wushan_discourse` VALUES (9, '圆转师太', 'taiji', '至刚易折，至柔无形，圆转者久。', 28, 9, 1);
INSERT INTO `wushan_discourse` VALUES (10, '负阴君', 'yinyang', '孤阳不生，独阴不长，相济者昌。', 21, 10, 1);
INSERT INTO `wushan_discourse` VALUES (11, '观星客', 'taiji', '仰观星，俯观心，星不语，心自明。', 19, 11, 1);
INSERT INTO `wushan_discourse` VALUES (12, '藏锋居士', 'jin', '利刃在鞘方为利刃，人前藏锋是修行。', 17, 12, 1);
INSERT INTO `wushan_discourse` VALUES (13, '拾露童子', 'mu', '晨露拾不满一碗，却洗亮了一双眼。', 15, 13, 1);
INSERT INTO `wushan_discourse` VALUES (14, '半盏道人', 'shui', '茶斟半盏，话留三分，路才走得远。', 12, 14, 1);
INSERT INTO `wushan_discourse` VALUES (15, '燃灯叟', 'huo', '灯芯烧的是油，亮的是别人夜里的路。', 9, 15, 1);
INSERT INTO `wushan_discourse` VALUES (16, '埋玉人', 'tu', '玉埋土中千年，出土仍是玉。', 8, 16, 1);
INSERT INTO `wushan_discourse` VALUES (17, '驭雷童', 'lei', '怕雷的人听不见雷里的天机。', 7, 17, 1);
INSERT INTO `wushan_discourse` VALUES (18, '流光君', 'dian', '一瞬即是一生，莫负电光石火。', 5, 18, 1);
INSERT INTO `wushan_discourse` VALUES (19, '验收行者', 'huo', '灯芯烧的是油，亮的是别人夜里的路。', 0, 999, 1);

-- ----------------------------
-- Table structure for wushan_grade
-- ----------------------------
DROP TABLE IF EXISTS `wushan_grade`;
CREATE TABLE `wushan_grade`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `grade_key` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '品阶 key：fan/huang/xuan/di/tian/sheng',
  `name` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '品阶名：凡品/黄品/玄品/地品/天品/圣品',
  `grade_rank` tinyint NOT NULL COMMENT '等第：1=凡 … 6=圣',
  `color` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '前端展示色',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1=启用 0=下架',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_grade_key`(`grade_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 7 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '品阶（分级）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_grade
-- ----------------------------
INSERT INTO `wushan_grade` VALUES (1, 'fan', '凡品', 1, '#9aa08c', 1, 1);
INSERT INTO `wushan_grade` VALUES (2, 'huang', '黄品', 2, '#e8c372', 2, 1);
INSERT INTO `wushan_grade` VALUES (3, 'xuan', '玄品', 3, '#3a8fd0', 3, 1);
INSERT INTO `wushan_grade` VALUES (4, 'di', '地品', 4, '#9d7bf0', 4, 1);
INSERT INTO `wushan_grade` VALUES (5, 'tian', '天品', 5, '#e8412b', 5, 1);
INSERT INTO `wushan_grade` VALUES (6, 'sheng', '圣品', 6, '#f2e34d', 6, 1);

-- ----------------------------
-- Table structure for wushan_hot_keyword
-- ----------------------------
DROP TABLE IF EXISTS `wushan_hot_keyword`;
CREATE TABLE `wushan_hot_keyword`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `keyword` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '热词',
  `element` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '五行 key',
  `search_count` int NULL DEFAULT 0 COMMENT '搜索次数（可由搜索日志统计刷新）',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1=展示 0=隐藏',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_keyword`(`keyword` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '热门搜索词' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_hot_keyword
-- ----------------------------
INSERT INTO `wushan_hot_keyword` VALUES (1, '五行阵法', 'shui', 520, 1, 1);
INSERT INTO `wushan_hot_keyword` VALUES (2, '雾山秘录', 'huo', 486, 2, 1);
INSERT INTO `wushan_hot_keyword` VALUES (3, '淬火之术', 'huo', 402, 3, 1);
INSERT INTO `wushan_hot_keyword` VALUES (4, '山涧灵草', 'mu', 377, 4, 1);
INSERT INTO `wushan_hot_keyword` VALUES (5, '金石锻造', 'jin', 301, 5, 1);
INSERT INTO `wushan_hot_keyword` VALUES (6, '厚土寻脉', 'tu', 288, 6, 1);
INSERT INTO `wushan_hot_keyword` VALUES (7, '御风身法', 'feng', 250, 7, 1);
INSERT INTO `wushan_hot_keyword` VALUES (8, '紫雷诀', 'lei', 233, 8, 1);
INSERT INTO `wushan_hot_keyword` VALUES (9, '掣电心法', 'dian', 210, 9, 1);
INSERT INTO `wushan_hot_keyword` VALUES (10, '太极图谱', 'taiji', 198, 10, 1);
INSERT INTO `wushan_hot_keyword` VALUES (11, '阴阳双鱼', 'yinyang', 175, 11, 1);

-- ----------------------------
-- Table structure for wushan_item
-- ----------------------------
DROP TABLE IF EXISTS `wushan_item`;
CREATE TABLE `wushan_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '条目名',
  `summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '条目简介',
  `element` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '五行 key：jin/mu/shui/huo/tu/feng/lei/dian/taiji/yinyang',
  `grade_key` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '品阶 key',
  `category_id` bigint NOT NULL COMMENT '分类 id（关联 wushan_category）',
  `sort` int NULL DEFAULT 0 COMMENT '排序（小在前）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1=上架 0=下架',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_element`(`element` ASC) USING BTREE,
  INDEX `idx_category`(`category_id` ASC) USING BTREE,
  INDEX `idx_grade`(`grade_key` ASC) USING BTREE,
  INDEX `idx_title`(`title` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 122 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '藏经条目（搜索对象）' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_item
-- ----------------------------
INSERT INTO `wushan_item` VALUES (1, '焚天七绝焰', '七式连环，焰随心走，至刚至烈，火行至高功法。', 'huo', 'tian', 6, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (2, '心火燎原功', '以内火淬炼经脉，痛而后明，进境极快。', 'huo', 'di', 4, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (3, '引火诀', '引外火为己用，火行入门之基，稳而无毒。', 'huo', 'xuan', 4, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (4, '《火候记》', '文火武火，全凭一心，论火行修炼的节奏。', 'huo', 'huang', 2, 4, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (5, '《明心一炬》', '火不照外物，照的是本心，心明则焰清。', 'huo', 'di', 2, 5, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (6, '地心火髓', '地脉深处的火之精华，炼体炼器两相宜。', 'huo', 'tian', 9, 6, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (7, '朱果', '赤红果实，服之火行亲和大增。', 'huo', 'xuan', 8, 7, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (8, '火纹石', '石纹如焰，铺设火阵的常用材料。', 'huo', 'huang', 9, 8, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (9, '庚金百斩诀', '百斩一意，斩物斩念，金行至高攻伐之法。', 'jin', 'tian', 6, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (10, '金刚不坏体', '淬体如铁，横练金身，守御第一。', 'jin', 'di', 4, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (11, '锐金剑指', '指出如剑，锋锐凝于一线，破甲穿石。', 'jin', 'xuan', 6, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (12, '《断金录》', '当断不断，反受其乱，论金行之决断。', 'jin', 'huang', 2, 4, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (13, '《锋藏于鞘》', '利刃在鞘方为利刃，藏锋是金行的第一课。', 'jin', 'di', 2, 5, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (14, '庚金精', '万年金气凝成的精粒，锻造上品兵刃之基。', 'jin', 'tian', 9, 6, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (15, '白玉髓', '温润如玉而坚逾铁，炼器辅材。', 'jin', 'xuan', 9, 7, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (16, '剑心草', '叶如剑形，服之坚定剑心，不易动摇。', 'jin', 'huang', 8, 8, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (17, '青木长生功', '生生不息，伤损自愈，木行至高养生法。', 'mu', 'tian', 4, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (18, '万藤缠身手', '柔藤千变，缠困绞锁，以柔克刚。', 'mu', 'di', 6, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (19, '木灵吐纳法', '晨昏吐纳，与草木共鸣，固本培元。', 'mu', 'xuan', 4, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (20, '《本草问答》', '以问答体论山中药性，采药必读。', 'mu', 'huang', 2, 4, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (21, '《生生不息论》', '损而不折，折而复生，木行的大道。', 'mu', 'di', 2, 5, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (22, '千年灵芝', '固本培元第一灵药，重伤亦可续命。', 'mu', 'tian', 8, 6, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (23, '木灵乳', '古树流出的乳白津液，温养经脉。', 'mu', 'xuan', 8, 7, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (24, '固元参', '常见的培元参类，价平效稳。', 'mu', 'huang', 8, 8, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (25, '沧浪十七叠', '后浪推前浪，十七叠力，水行至高攻法。', 'shui', 'tian', 6, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (26, '弱水诀', '至柔至韧，滴水穿石，绵长不休。', 'shui', 'di', 4, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (27, '凝冰指', '点水成冰，寒气侵体，迟滞敌形。', 'shui', 'xuan', 6, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (28, '《观水笔记》', '水无常形，随物赋形，论顺势而为。', 'shui', 'huang', 2, 4, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (29, '《静水流深》', '不动的是表面，奔涌的是深处。', 'shui', 'di', 2, 5, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (30, '寒潭玉液', '千年寒潭底的一捧玉液，洗经伐髓。', 'shui', 'tian', 8, 6, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (31, '水云纱', '入水不濡，织衣可避水火。', 'shui', 'xuan', 9, 7, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (32, '净水珠', '化浊为清，行路必备的珠子。', 'shui', 'huang', 8, 8, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (33, '厚土不灭体', '身融大地，不动如山，土行至高守御。', 'tu', 'tian', 4, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (34, '山岳印', '一印压下，重逾千钧，封地断路。', 'tu', 'di', 6, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (35, '撼地桩', '扎桩于地，借大地之力发劲。', 'tu', 'xuan', 4, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (36, '《守拙篇》', '大巧若拙，守得住才有以后。', 'tu', 'huang', 2, 4, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (37, '《大地无言》', '土不言语，载万物而行。', 'tu', 'di', 2, 5, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (38, '息壤', '生生不息的神土，一撮可成一丘。', 'tu', 'tian', 9, 6, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (39, '黄精玉髓', '土中精气所结，健脾固本。', 'tu', 'xuan', 8, 7, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (40, '千年窑土', '筑炉炼器的上好底料。', 'tu', 'huang', 9, 8, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (41, '大衍游风身法', '身与风合，来去无踪，风行至高身法。', 'feng', 'tian', 5, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (42, '罡风九变', '九个方位随心切换，攻守转换极快。', 'feng', 'di', 5, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (43, '听风步', '踏风而行，步随音动。', 'feng', 'xuan', 5, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (44, '《风语集》', '听风辨位，未动先知。', 'feng', 'huang', 2, 4, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (45, '《无痕之道》', '风过千山，不留一物，故能过千山。', 'feng', 'di', 2, 5, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (46, '风灵羽', '风灵蜕下的羽衣，御风而行的凭依。', 'feng', 'tian', 9, 6, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (47, '青岚叶', '叶落而风起，练身法常用。', 'feng', 'xuan', 8, 7, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (48, '追风草', '煎汤沐足，轻身健步。', 'feng', 'huang', 8, 8, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (49, '九霄引雷诀', '引天雷入体淬炼，雷行至高法门，凶险亦大。', 'lei', 'tian', 4, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (50, '紫霄神雷', '掌心凝紫雷，一击震魂。', 'lei', 'di', 6, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (51, '惊雷指', '蓄而忽发，指落雷鸣。', 'lei', 'xuan', 6, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (52, '《雷起于渊》', '雷霆之怒，蓄于至静。', 'lei', 'huang', 2, 4, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (53, '《一线之决》', '雷行修的是决断，一线之间定生死。', 'lei', 'di', 2, 5, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (54, '雷击木心', '历雷不死的老木之心，镇宅辟邪。', 'lei', 'tian', 9, 6, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (55, '紫霄髓', '雷云深处的凝结物，淬炼神魂。', 'lei', 'xuan', 8, 7, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (56, '引雷针', '布阵引雷的凡铁长针。', 'lei', 'huang', 9, 8, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (57, '掣电无影剑', '剑出如电，影未现而剑已收，电行至高剑法。', 'dian', 'tian', 6, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (58, '电光淬体术', '以电淬髓，速度与反应俱进。', 'dian', 'di', 4, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (59, '掠光诀', '借光而遁，瞬息十丈。', 'dian', 'xuan', 5, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (60, '《一瞬观照》', '电光石火，照见本来。', 'dian', 'huang', 2, 4, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (61, '《迅烈如霹雳》', '迟疑是电行唯一的天敌。', 'dian', 'di', 2, 5, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (62, '电光石髓', '闪电击石所留的髓纹，服之神速。', 'dian', 'tian', 8, 6, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (63, '掣电梭', '掷出即至的暗器，肉眼难追。', 'dian', 'xuan', 9, 7, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (64, '明澈露', '洗目之露，视物极明。', 'dian', 'huang', 8, 8, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (65, '太极玄功 · 周天图', '周天运转，万法归一，太极至高心法。', 'taiji', 'tian', 4, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (66, '阴阳鱼推手', '听劲化劲，后发先至。', 'taiji', 'di', 6, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (67, '揽雀尾十八式', '起承转合，式式圆转。', 'taiji', 'xuan', 6, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (68, '《圆转论》', '至刚易折，圆转者久。', 'taiji', 'huang', 2, 4, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (69, '《万化归一》', '千招万式，尽头是一个圆。', 'taiji', 'di', 2, 5, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (70, '太极玉璧', '璧上阴阳流转，参悟圆融之理。', 'taiji', 'tian', 9, 6, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (71, '圆转石', '握之掌心自转，静心之石。', 'taiji', 'xuan', 9, 7, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (72, '两仪砂', '阴阳两色的细砂，布两仪阵之基。', 'taiji', 'huang', 9, 8, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (73, '阴阳互济功', '阴阳二气互为其根，阴阳至高法门。', 'yinyang', 'tian', 4, 1, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (74, '消长指', '指分晦明，一指之内判阴阳。', 'yinyang', 'di', 6, 2, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (75, '双鱼映月诀', '月下修行，映月观心。', 'yinyang', 'xuan', 4, 3, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (76, '《负阴抱阳》', '孤阳不生，独阴不长。', 'yinyang', 'huang', 2, 4, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (77, '《消长之衡》', '盛极必衰，衡者恒存。', 'yinyang', 'di', 2, 5, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (78, '阴阳双鱼佩', '佩分两半，合则气机圆满。', 'yinyang', 'tian', 9, 6, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (79, '晦朔草', '晦日朔日各开一花的奇草。', 'yinyang', 'xuan', 8, 7, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (80, '子午露', '子午二时收取的露水，调和气机。', 'yinyang', 'huang', 8, 8, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (81, '雾山秘录 · 卷一', '五山武星之火卷，载炽烈心明之法。', 'huo', 'di', 4, 11, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (82, '金石锻造录', '锋锐决断，金行兵刃锻造全解。', 'jin', 'huang', 2, 12, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (83, '山涧灵草图鉴', '生发仁厚，草木图谱与采撷时令。', 'mu', 'xuan', 8, 13, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (84, '五行阵法纲要', '灵动智略，水行布阵之纲要。', 'shui', 'di', 7, 14, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (85, '厚土寻脉记', '厚重承载，寻地脉走向之实录。', 'tu', 'huang', 2, 15, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (86, '淬火之术十二式', '以火淬锋，十二式口诀与图解。', 'huo', 'xuan', 6, 16, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (87, '雾山五行总纲', '五山武星总纲，入境者必读。', 'huo', 'tian', 2, 17, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (88, '御风身法 · 行无踪', '疾速无形，踏叶不惊的轻功总诀。', 'feng', 'di', 5, 18, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (89, '紫雷诀', '震赫果决，引九霄雷气入体的法门。', 'lei', 'xuan', 4, 19, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (90, '掣电心法', '迅烈明澈，以电悟道的观想法门。', 'dian', 'xuan', 4, 20, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (91, '太极图谱', '圆融万化，阴阳鱼推手与套路图解。', 'taiji', 'xuan', 7, 21, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (92, '阴阳双鱼石拓本', '相济消长，山中古碑的双鱼拓片。', 'yinyang', 'xuan', 9, 22, 1, '2026-10-16 15:53:54', '2026-10-16 15:53:54');
INSERT INTO `wushan_item` VALUES (93, '培元丹', '固本培元的常用丹药，性温无碍，人人可服。', 'mu', 'huang', 10, 1, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_item` VALUES (94, '凝神丹', '宁心凝神，行功走火时一粒定惊。', 'shui', 'xuan', 10, 2, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_item` VALUES (95, '破障丹', '冲击关口前的引路丹，破障一臂之力。', 'huo', 'di', 10, 3, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_item` VALUES (96, '洗髓丹', '洗经伐髓，脱胎换骨，可遇不可求。', 'shui', 'tian', 10, 4, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_item` VALUES (97, '金创药', '外伤速愈的寻常药散，行囊必备。', 'jin', 'fan', 10, 5, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_item` VALUES (98, '青锋剑', '三尺青锋，削铁如泥，剑修常备。', 'jin', 'xuan', 11, 1, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_item` VALUES (99, '赤焰葫芦', '内蕴一缕地心火，可放可收。', 'huo', 'di', 11, 2, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_item` VALUES (100, '藏水青囊', '囊中自有一汪寒潭，旱地亦可用。', 'shui', 'xuan', 11, 3, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_item` VALUES (101, '千钧印', '一印压下重逾千钧，封敌断路。', 'tu', 'di', 11, 4, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_item` VALUES (102, '避风铜铃', '风起铃动，先人一步知险。', 'feng', 'huang', 11, 5, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_item` VALUES (103, '《生火十二诀》', '最寻常的引火口诀，灶膛火房皆可用。', 'huo', 'fan', 6, 31, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (104, '山间青笋', '寻常时蔬，充饥解乏，采药人的口粮。', 'mu', 'fan', 8, 32, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (105, '净水砂囊', '粗砂滤水的皮囊，行路常备。', 'shui', 'fan', 9, 33, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (106, '《夯歌集》', '筑墙打夯的号子，练气自打夯起。', 'tu', 'fan', 2, 34, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (107, '追风蒲扇', '凡人纳凉的蒲扇，练听风的第一步。', 'feng', 'fan', 11, 35, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (108, '《数雷篇》', '雷夜数雷记数，练的是心静。', 'lei', 'fan', 2, 36, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (109, '《辨光小识》', '分辨天光与电光的入门小册。', 'dian', 'fan', 2, 37, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (110, '晨操十式', '山民晨练的十式体操，太极由此入门。', 'taiji', 'fan', 5, 38, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (111, '《时辰歌诀》', '歌诀记昼夜时辰，阴阳启蒙读物。', 'yinyang', 'fan', 2, 39, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (112, '《雾山火经 · 无字卷》', '火行至典，无字而心传，历代掌峰亲授。', 'huo', 'sheng', 4, 41, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (113, '万兵朝宗诀', '天下兵刃气机归宗一诀，金行圣典。', 'jin', 'sheng', 6, 42, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (114, '建木神种', '传说建木的一粒种子，落地生根千里。', 'mu', 'sheng', 9, 43, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (115, '沧溟真解', '万水归海的真解，观之可通水性本源。', 'shui', 'sheng', 4, 44, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (116, '《厚德载物 · 山经》', '土行圣典，载物配德，非大毅力者不可读。', 'tu', 'sheng', 2, 45, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (117, '罡风轮转图', '天罡轮转之图，参透者风行万里。', 'feng', 'sheng', 5, 46, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (118, '九霄雷帝玺', '传说雷帝遗玺，掌雷罚之权柄。', 'lei', 'sheng', 11, 47, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (119, '掣电神行符', '一符贴身，身法快过电光。', 'dian', 'sheng', 11, 48, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (120, '《太极本源论》', '万化归一之论，太极一脉的源头经卷。', 'taiji', 'sheng', 2, 49, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_item` VALUES (121, '太初阴阳葫', '葫中自成阴阳二气，天地未分之象。', 'yinyang', 'sheng', 11, 50, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');

-- ----------------------------
-- Table structure for wushan_market_item
-- ----------------------------
DROP TABLE IF EXISTS `wushan_market_item`;
CREATE TABLE `wushan_market_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '物品名',
  `summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '物品简介',
  `element` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '五行 key',
  `grade_key` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '品阶 key',
  `category_id` bigint NULL DEFAULT NULL COMMENT '分类 id（关联 wushan_category）',
  `price` int NOT NULL DEFAULT 0 COMMENT '售价（灵石）',
  `stock` int NOT NULL DEFAULT 0 COMMENT '库存',
  `seller` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '雾山山货铺' COMMENT '摊主',
  `sold` int NOT NULL DEFAULT 0 COMMENT '已售',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态：1=在售 0=下架',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上架时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_element`(`element` ASC) USING BTREE,
  INDEX `idx_grade`(`grade_key` ASC) USING BTREE,
  INDEX `idx_category`(`category_id` ASC) USING BTREE,
  INDEX `idx_title`(`title` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '市集物品' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_market_item
-- ----------------------------
INSERT INTO `wushan_market_item` VALUES (1, '千年灵芝', '固本培元第一灵药，重伤亦可续命。', 'mu', 'tian', 8, 800, 1, '拾露童子', 4, 1, '2026-10-16 15:54:17', '2026-10-16 16:24:50');
INSERT INTO `wushan_market_item` VALUES (2, '固元参', '常见的培元参类，价平效稳。', 'mu', 'huang', 8, 30, 50, '雾山山货铺', 21, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (3, '朱果', '赤红果实，服之火行亲和大增。', 'huo', 'xuan', 8, 120, 12, '燃灯叟', 7, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (4, '寒潭玉液', '千年寒潭底的一捧玉液，洗经伐髓。', 'shui', 'tian', 8, 660, 3, '半盏道人', 2, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (5, '净水珠', '化浊为清，行路必备的珠子。', 'shui', 'huang', 8, 25, 40, '雾山山货铺', 16, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (6, '剑心草', '叶如剑形，服之坚定剑心。', 'jin', 'huang', 8, 20, 60, '藏锋居士', 9, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (7, '庚金精', '万年金气凝成的精粒，锻造上品兵刃之基。', 'jin', 'tian', 9, 900, 1, '藏锋居士', 4, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (8, '白玉髓', '温润如玉而坚逾铁，炼器辅材。', 'jin', 'xuan', 9, 150, 8, '雾山山货铺', 5, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (9, '火纹石', '石纹如焰，铺设火阵的常用材料。', 'huo', 'huang', 9, 15, 80, '燃灯叟', 12, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (10, '雷击木心', '历雷不死的老木之心，镇宅辟邪。', 'lei', 'tian', 9, 750, 2, '驭雷童', 1, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (11, '紫霄髓', '雷云深处的凝结物，淬炼神魂。', 'lei', 'xuan', 9, 180, 6, '驭雷童', 3, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (12, '息壤', '生生不息的神土，一撮可成一丘。', 'tu', 'tian', 9, 1200, 1, '埋玉人', 0, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (13, '风灵羽', '风灵蜕下的羽衣，御风而行的凭依。', 'feng', 'tian', 9, 700, 2, '听风散人', 2, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (14, '培元丹', '固本培元的常用丹药，性温无碍。', 'mu', 'huang', 10, 40, 30, '雾山药庐', 18, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (15, '凝神丹', '宁心凝神，走火入魔时一粒定惊。', 'shui', 'xuan', 10, 90, 15, '雾山药庐', 8, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (16, '破障丹', '冲击关口前的引路丹。', 'huo', 'di', 10, 260, 6, '雾山药庐', 2, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (17, '洗髓丹', '洗经伐髓，脱胎换骨，可遇不可求。', 'shui', 'tian', 10, 1500, 1, '雾山药庐', 0, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (18, '青锋剑', '三尺青锋，削铁如泥，剑修常备。', 'jin', 'xuan', 11, 200, 5, '藏锋居士', 3, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (19, '赤焰葫芦', '内蕴一缕地心火，可放可收。', 'huo', 'di', 11, 480, 3, '焚舟子', 1, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (20, '藏水青囊', '囊中自有一汪寒潭，旱地亦可用。', 'shui', 'xuan', 11, 160, 7, '半盏道人', 4, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (21, '千钧印', '一印压下重逾千钧，封敌断路。', 'tu', 'di', 11, 520, 2, '厚土翁', 1, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (22, '避风铜铃', '风起铃动，先人一步知险。', 'feng', 'huang', 11, 35, 25, '听风散人', 10, 1, '2026-10-16 15:54:17', '2026-10-16 15:54:17');
INSERT INTO `wushan_market_item` VALUES (23, '电光石髓', '闪电击石所留的髓纹，服之神速。', 'dian', 'tian', 8, 680, 4, '驭雷童', 1, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (24, '掣电梭', '掷出即至的暗器，肉眼难追。', 'dian', 'xuan', 11, 160, 6, '流光君', 2, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (25, '明澈露', '洗目之露，视物极明。', 'dian', 'huang', 8, 22, 45, '雾山药庐', 13, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (26, '太极玉璧', '璧上阴阳流转，参悟圆融之理。', 'taiji', 'tian', 9, 720, 2, '圆转师太', 1, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (27, '圆转石', '握之掌心自转，静心之石。', 'taiji', 'xuan', 9, 145, 7, '圆转师太', 3, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (28, '两仪砂', '阴阳两色的细砂，布两仪阵之基。', 'taiji', 'huang', 9, 18, 60, '雾山山货铺', 22, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (29, '太极道种', '大道之种，得者可开一宗。', 'taiji', 'sheng', 9, 3000, 1, '圆转师太', 0, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (30, '阴阳双鱼佩', '佩分两半，合则气机圆满。', 'yinyang', 'tian', 11, 860, 1, '负阴君', 2, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (31, '晦朔草', '晦日朔日各开一花的奇草。', 'yinyang', 'xuan', 8, 155, 5, '负阴君', 2, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (32, '子午露', '子午二时收取的露水，调和气机。', 'yinyang', 'huang', 8, 28, 38, '半盏道人', 9, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (33, '寻常艾草', '山中最常见的艾草，灸疗入门。', 'mu', 'fan', 8, 6, 120, '雾山山货铺', 35, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (34, '粗麻符纸', '最寻常的符纸，画符练手用。', 'tu', 'fan', 9, 4, 200, '雾山山货铺', 48, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');
INSERT INTO `wushan_market_item` VALUES (35, '九转还魂丹', '传说中能起死回生的丹药，千年一出。', 'mu', 'sheng', 10, 2400, 1, '雾山药庐', 0, 1, '2026-10-16 16:30:43', '2026-10-16 16:30:43');

-- ----------------------------
-- Table structure for wushan_nav
-- ----------------------------
DROP TABLE IF EXISTS `wushan_nav`;
CREATE TABLE `wushan_nav`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `nav_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '导航 key',
  `label` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '中文名',
  `en` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '英文名',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1=启用 0=隐藏',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_nav_key`(`nav_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '首页导航' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_nav
-- ----------------------------
INSERT INTO `wushan_nav` VALUES (1, 'guanshan', '观山', 'scenery', 1, 1);
INSERT INTO `wushan_nav` VALUES (2, 'cangjing', '藏经', 'codex', 2, 1);
INSERT INTO `wushan_nav` VALUES (3, 'lundao', '论道', 'discourse', 3, 1);
INSERT INTO `wushan_nav` VALUES (4, 'shiji', '市集', 'market', 4, 1);

-- ----------------------------
-- Table structure for wushan_realm
-- ----------------------------
DROP TABLE IF EXISTS `wushan_realm`;
CREATE TABLE `wushan_realm`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '秘境名',
  `element` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '五行 key',
  `grade_key` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '品阶 key',
  `layers` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '层数（如：九层）',
  `summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '秘境简介',
  `sort` int NULL DEFAULT 0 COMMENT '排序（小在前）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1=开启 0=封闭',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开启时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_realm_name`(`name` ASC) USING BTREE,
  INDEX `idx_element`(`element` ASC) USING BTREE,
  INDEX `idx_grade`(`grade_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 25 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '藏经阁秘境' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_realm
-- ----------------------------
INSERT INTO `wushan_realm` VALUES (1, '焚火秘境', 'huo', 'di', '九层', '地火奔涌，愈深愈烈，最深处传闻有火髓。', 1, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (2, '离火洞天', 'huo', 'tian', '三层', '天品秘境，非离火灵根者入内即灼。', 2, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (3, '断剑冢', 'jin', 'di', '七层', '万剑朝冢，剑气森然，剑修圣地。', 3, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (4, '白刃崖', 'jin', 'xuan', '五层', '崖壁如刃，采药炼体两相宜。', 4, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (5, '青木林深处', 'mu', 'xuan', '五层', '草木有灵，迷径重重，需持本心而行。', 5, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (6, '长生谷', 'mu', 'di', '九层', '药气氤氲，愈深药龄愈老。', 6, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (7, '寒潭秘境', 'shui', 'xuan', '三层', '潭底别有洞天，寒气刺骨。', 7, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (8, '沧浪海眼', 'shui', 'tian', '一层', '海眼之下暗流万千，一步一世界。', 8, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (9, '地肺石窟', 'tu', 'di', '十二层', '层峦入地，矿脉纵横，土行宝藏。', 9, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (10, '息壤秘窟', 'tu', 'tian', '一层', '天品秘境，见者有份，去者多留。', 10, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (11, '风眼长廊', 'feng', 'xuan', '九层', '罡风穿廊，身法试炼之地。', 11, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (12, '青岚绝壁', 'feng', 'di', '五层', '岚气缠壁，攀者如御风。', 12, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (13, '落雷原', 'lei', 'di', '七层', '终年雷暴，引雷淬体者的乐园。', 13, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (14, '九霄雷池', 'lei', 'tian', '一层', '天品秘境，雷池之水可淬神魂。', 14, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (15, '电光裂缝', 'dian', 'xuan', '三层', '裂缝间电光游走，进退须快。', 15, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (16, '掣电之隙', 'dian', 'di', '一层', '一瞬即达，一瞬即失，考验眼力。', 16, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (17, '太极幻境', 'taiji', 'tian', '一层', '阴阳双鱼流转，入境者各见本心。', 17, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (18, '晦明谷', 'yinyang', 'di', '三层', '昼夜各半，阴阳消长，修炼互济功之地。', 18, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_realm` VALUES (19, '乱石迷径', 'tu', 'fan', '一层', '乱石堆成的天然迷径，试炼新人的第一关。', 19, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_realm` VALUES (20, '萤火洞', 'huo', 'fan', '一层', '洞中萤火常年不熄，夜行训练之地。', 20, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_realm` VALUES (21, '碧潭幽谷', 'shui', 'huang', '三层', '谷中碧潭连串，采药避暑两相宜。', 21, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_realm` VALUES (22, '松涛林海', 'feng', 'huang', '五层', '风起松涛，练听风辨位的去处。', 22, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_realm` VALUES (23, '太初混元境', 'taiji', 'sheng', '一层', '开山祖师坐化之地，一步一纪元。', 23, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_realm` VALUES (24, '两仪归墟', 'yinyang', 'sheng', '一层', '阴阳二气在此归墟，互济功大成之地。', 24, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');

-- ----------------------------
-- Table structure for wushan_scene
-- ----------------------------
DROP TABLE IF EXISTS `wushan_scene`;
CREATE TABLE `wushan_scene`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_key` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图卷 key',
  `title` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '图卷名',
  `en` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '英文名',
  `season` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '季节：春/夏/秋/冬',
  `summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图卷描述',
  `tint` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '主色调（前端展示）',
  `sort` int NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1=展示 0=隐藏',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_scene_key`(`scene_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '山中四时图卷' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_scene
-- ----------------------------
INSERT INTO `wushan_scene` VALUES (1, 'spring', '春山新霁', 'spring', '春', '草木生发，山色初醒，涧水初涨。', '#4caf6e', 1, 1);
INSERT INTO `wushan_scene` VALUES (2, 'summer', '夏山听雨', 'summer', '夏', '骤雨过岭，雾气蒸腾，雷走云低。', '#3a8fd0', 2, 1);
INSERT INTO `wushan_scene` VALUES (3, 'autumn', '秋山落叶', 'autumn', '秋', '金风肃杀，层林尽染，雁字南归。', '#e8c372', 3, 1);
INSERT INTO `wushan_scene` VALUES (4, 'winter', '冬山覆雪', 'winter', '冬', '千山素裹，炉火围坐，星火可亲。', '#ece4d2', 4, 1);

-- ----------------------------
-- Table structure for wushan_search_log
-- ----------------------------
DROP TABLE IF EXISTS `wushan_search_log`;
CREATE TABLE `wushan_search_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` bigint NULL DEFAULT NULL COMMENT '搜索者 id（未登录为空）',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '搜索者名号',
  `keyword` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '搜索词',
  `element` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '筛选五行',
  `grade_key` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '筛选品阶',
  `category_id` bigint NULL DEFAULT NULL COMMENT '筛选分类',
  `result_cnt` int NULL DEFAULT 0 COMMENT '命中条数',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '搜索时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_keyword`(`keyword` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '搜索日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_search_log
-- ----------------------------
INSERT INTO `wushan_search_log` VALUES (1, NULL, NULL, '淬火之术', NULL, NULL, NULL, 1, '2026-10-16 16:11:20');
INSERT INTO `wushan_search_log` VALUES (2, NULL, NULL, '山涧灵草', NULL, NULL, NULL, 1, '2026-10-16 16:11:23');
INSERT INTO `wushan_search_log` VALUES (3, NULL, NULL, '山涧灵草', NULL, NULL, NULL, 1, '2026-10-16 16:11:23');
INSERT INTO `wushan_search_log` VALUES (4, NULL, NULL, '山涧灵草', NULL, NULL, NULL, 1, '2026-10-16 16:11:24');
INSERT INTO `wushan_search_log` VALUES (5, NULL, NULL, '五行阵法', NULL, NULL, NULL, 1, '2026-10-16 16:29:33');
INSERT INTO `wushan_search_log` VALUES (6, NULL, NULL, '', NULL, NULL, NULL, 23, '2026-10-16 16:29:33');
INSERT INTO `wushan_search_log` VALUES (7, NULL, NULL, '', 'huo', 'tian', NULL, 3, '2026-10-16 16:29:33');

-- ----------------------------
-- Table structure for wushan_spirit_root
-- ----------------------------
DROP TABLE IF EXISTS `wushan_spirit_root`;
CREATE TABLE `wushan_spirit_root`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '灵根名',
  `element` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '五行 key',
  `grade_key` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '品阶 key',
  `summary` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '灵根简介',
  `sort` int NULL DEFAULT 0 COMMENT '排序（小在前）',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1=收录 0=隐匿',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收录时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_root_name`(`name` ASC) USING BTREE,
  INDEX `idx_element`(`element` ASC) USING BTREE,
  INDEX `idx_grade`(`grade_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 17 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '藏经阁灵根' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_spirit_root
-- ----------------------------
INSERT INTO `wushan_spirit_root` VALUES (1, '庚金灵根', 'jin', 'tian', '锋锐天成，习金行功法一日千里，杀伐之气重。', 1, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_spirit_root` VALUES (2, '青木灵根', 'mu', 'tian', '生生不息，疗伤自愈远胜常人，木行天授。', 2, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_spirit_root` VALUES (3, '弱水灵根', 'shui', 'tian', '性灵近水，吐纳如鱼得渊，变化之资。', 3, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_spirit_root` VALUES (4, '离火灵根', 'huo', 'tian', '火毒不侵，心火自燃，攻伐之资冠绝十行。', 4, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_spirit_root` VALUES (5, '息土灵根', 'tu', 'tian', '根骨如山，稳如磐石，守御与耐力之最。', 5, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_spirit_root` VALUES (6, '罡风灵根', 'feng', 'di', '身轻如燕，感风先知，身法一途天赋异禀。', 6, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_spirit_root` VALUES (7, '惊雷灵根', 'lei', 'di', '引雷淬体，怒而愈强，天生气势慑人。', 7, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_spirit_root` VALUES (8, '掣电灵根', 'dian', 'di', '反应如电，一点即通，最忌迟疑。', 8, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_spirit_root` VALUES (9, '混元灵根', 'taiji', 'sheng', '万法圆融，诸系皆通，百年难出一人。', 9, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_spirit_root` VALUES (10, '两仪灵根', 'yinyang', 'sheng', '阴阳同修，昼夜异能，圣品之资。', 10, 1, '2026-10-16 15:59:00', '2026-10-16 15:59:00');
INSERT INTO `wushan_spirit_root` VALUES (11, '金铁杂根', 'jin', 'fan', '微具金性，练金行事倍功半，胜在心志。', 11, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_spirit_root` VALUES (12, '静电微根', 'dian', 'fan', '冬日指尖有麻意，聊胜于无，贵在自知。', 12, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_spirit_root` VALUES (13, '赤炎双根', 'huo', 'huang', '心头两点火种，攻伐稍逊离火，胜在稳定。', 13, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_spirit_root` VALUES (14, '疾风敏根', 'feng', 'huang', '感风即动，身法入门远胜常人。', 14, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_spirit_root` VALUES (15, '玄水真根', 'shui', 'xuan', '水性通透，冰火同源，次弱水灵根一等的真根。', 15, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');
INSERT INTO `wushan_spirit_root` VALUES (16, '紫霆真根', 'lei', 'xuan', '引雷不伤，怒雷淬体比惊雷灵根温和。', 16, 1, '2026-10-16 16:34:21', '2026-10-16 16:34:21');

-- ----------------------------
-- Table structure for wushan_trade_log
-- ----------------------------
DROP TABLE IF EXISTS `wushan_trade_log`;
CREATE TABLE `wushan_trade_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `item_id` bigint NOT NULL COMMENT '物品 id',
  `item_title` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '物品名（冗余，便于查账）',
  `buyer_id` bigint NULL DEFAULT NULL COMMENT '买家 id（未登录为空）',
  `buyer` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '买家名号',
  `buy_count` int NOT NULL DEFAULT 1 COMMENT '购买数量',
  `total_price` int NOT NULL DEFAULT 0 COMMENT '总价（灵石）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成交时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_item`(`item_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '市集交易日志' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_trade_log
-- ----------------------------
INSERT INTO `wushan_trade_log` VALUES (1, 1, '千年灵芝', 6, '验收行者', 1, 800, '2026-10-16 16:24:50');

-- ----------------------------
-- Table structure for wushan_user
-- ----------------------------
DROP TABLE IF EXISTS `wushan_user`;
CREATE TABLE `wushan_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '涓婚敭',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '鍚嶅彿锛堢櫥褰曞悕 / 鏄电О锛',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '鏆楄?锛圔Crypt 瀵嗘枃瀛樺偍锛',
  `element` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '鏈?懡浜旇? key锛歫in/mu/shui/huo/tu',
  `element_label` varchar(8) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '浜旇? label锛氶噾/鏈?姘?鐏?鍦',
  `agreed` tinyint(1) NULL DEFAULT 0 COMMENT '鏄?惁鍏佽?灞辫?锛?=鏄?0=鍚',
  `agreed_at` datetime NULL DEFAULT NULL COMMENT '鍏佽?鏃堕棿',
  `status` tinyint NULL DEFAULT 1 COMMENT '鐘舵?锛?=姝ｅ父 0=绂佺敤',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '闆惧北灞辩睄锛堢敤鎴疯〃锛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_user
-- ----------------------------
INSERT INTO `wushan_user` VALUES (1, '金行', '$2a$10$2ox56qY8z556d59eBMusKukDmxGLfCmeUBHWuhMTYf7KAdt64KpGy', 'jin', '金', 1, '2026-10-16 15:41:13', 1, '2026-10-16 15:41:13', '2026-10-16 15:41:13');
INSERT INTO `wushan_user` VALUES (2, '木行', '$2a$10$CtWXR3Xa/YzDBkcEDQQDgei.X0gN//0VTpNfpgEolicmUjcL1xota', 'huo', '木', 1, '2026-10-16 15:46:33', 1, '2026-10-16 15:46:33', '2026-10-16 15:52:35');
INSERT INTO `wushan_user` VALUES (3, '水行', '$2a$10$84mQugdp6teYV2QQJlBVyeoOkGEQuznBCoJnFpJBjsPld3g1ypfaG', 'huo', '水', 1, '2026-10-16 15:47:27', 1, '2026-10-16 15:47:27', '2026-10-16 15:52:30');
INSERT INTO `wushan_user` VALUES (4, '火行', '$2a$10$lZE/nfPpowJjjhF2xAkh6umOXm30JM71KKjE5KpjIsbn3Nxcnk5im', 'huo', '火', 1, '2026-10-16 15:49:47', 1, '2026-10-16 15:49:47', '2026-10-16 15:49:47');
INSERT INTO `wushan_user` VALUES (5, '土行', '$2a$10$F8ma5/0.rOzBt9bbmh.vles8EmUt0WTqTGmxn.08WFGOBUQ0GA54W', 'tu', '土', 1, '2026-10-16 15:50:41', 1, '2026-10-16 15:50:41', '2026-10-16 15:50:41');
INSERT INTO `wushan_user` VALUES (6, '验收行者', '$2a$10$NTH86cwkyN6erMUw5eh80eozwDqdgUAPyrXVQRP4RqpmDIvMcdmPO', 'huo', '火', 1, '2026-10-16 16:06:58', 1, '2026-10-16 16:06:58', '2026-10-16 16:06:58');
INSERT INTO `wushan_user` VALUES (7, 'yy', '$2a$10$7LXfYOBAzqu/ZUN4dZ5Fg.6h38SG/yOZqGq87byomFalbWG1pWoBe', 'huo', '火', 1, '2026-10-16 16:37:43', 1, '2026-10-16 16:37:43', '2026-10-16 16:37:43');

SET FOREIGN_KEY_CHECKS = 1;
