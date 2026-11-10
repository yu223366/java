package com.fenglin.springboottest.service;

import java.util.List;
import java.util.Map;

/**
 * 山门世界玩法：背包 / 秘境探索 / 炼丹炼器 / 五行问对决 / 传书私信。
 */
public interface WorldService {

    /** 我的背包（含物品名、五行、品阶） */
    Map<String, Object> bag(String authorization);

    /** 可探索的秘境列表 */
    List<Map<String, Object>> realms();

    /** 进入秘境：消耗灵石，返回第一个事件节点 */
    Map<String, Object> exploreStart(String authorization, Long realmId);

    /** 选择当前事件的选项，推进到下一节点或结算 */
    Map<String, Object> exploreChoose(String authorization, Integer choiceIdx);

    /** 我的探索记录 */
    List<Map<String, Object>> exploreLogs(String authorization);

    /** 配方列表（含材料是否够用） */
    List<Map<String, Object>> recipes(String authorization);

    /** 炼制：扣材料与灵石，产物入背包 */
    Map<String, Object> craft(String authorization, Long recipeId);

    /** 发起对决：随机匹配一位在线山友 */
    Map<String, Object> duelStart(String authorization);

    /** 应答对决题目：答满 5 题自动结算 */
    Map<String, Object> duelAnswer(String authorization, Long duelId, Integer choiceIdx);

    /** 我的对决记录 */
    List<Map<String, Object>> duelRecords(String authorization);

    /** 收件箱 + 未读数 + 可传书的山友 */
    Map<String, Object> inbox(String authorization);

    /** 传书 */
    Map<String, Object> send(String authorization, String toName, String content);

    /** 标记已读 */
    Map<String, Object> read(String authorization, Long messageId);

    /** 一键全部已读 */
    Map<String, Object> readAll(String authorization);
}
