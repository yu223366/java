package com.fenglin.springboottest.service;

import java.util.List;
import java.util.Map;

/**
 * 辩场：同一辩题下分正反两方，各自陈词，点赞即票，票数多者胜。
 * 复用 wushan_discourse（topic 区分辩题、side 区分立场），另用
 * wushan_discourse_like 保证一人对一条陈词只能赞一次。
 */
public interface DebateService {

    /** 辩场列表：按辩题分组，含正反双方语录、票数与胜负判定 */
    List<Map<String, Object>> list(String authorization);

    /** 发起辩论（正方立论） */
    Map<String, Object> publish(String token, String topic, String quote);

    /** 应战 / 附论：side 0=正方 1=反方 */
    Map<String, Object> reply(String token, String topic, Integer side, String quote);

    /** 给某条陈词点赞（一人一次），返回票数与该辩题当前胜负 */
    Map<String, Object> like(String token, Long discourseId);
}
