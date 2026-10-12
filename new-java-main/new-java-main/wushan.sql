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

 Date: 12/10/2026 11:56:26
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '闆惧北灞辩睄锛堢敤鎴疯〃锛' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of wushan_user
-- ----------------------------
INSERT INTO `wushan_user` VALUES (1, '闻人', '$2a$10$tCCd0mQg48AT8gHrw36SiOooxRtLIPQlfORJWbR1YExJIuAPAMQwO', 'huo', '火', 1, '2026-10-12 11:04:15', 1, '2026-10-12 11:04:15', '2026-10-12 11:04:15');

SET FOREIGN_KEY_CHECKS = 1;
