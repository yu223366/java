package com.fenglin.springboottest.service;

import java.util.List;
import java.util.Map;

/**
 * 山门玩法：测灵根 / 每日签到 / 每日一题。
 */
public interface GameService {

    /** 取测灵根的题目（答案不在前端，由服务端评分） */
    Map<String, Object> spiritQuiz();

    /** 提交测灵根答案，返回灵根结果 */
    Map<String, Object> submitSpirit(String authorization, List<Integer> answers);

    /** 我的灵根（最近一次测评） */
    Map<String, Object> mySpirit(String authorization);

    /** 签到状态 */
    Map<String, Object> signInStatus(String authorization);

    /** 每日签到 */
    Map<String, Object> checkIn(String authorization);

    /** 今日一题 */
    Map<String, Object> dailyQuestion(String authorization);

    /** 提交今日一题答案 */
    Map<String, Object> answerDaily(String authorization, Integer answerIdx);

    /** 灵石钱包：余额 + 最近流水 */
    Map<String, Object> wallet(String authorization);
}
