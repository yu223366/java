package com.fenglin.springboottest.service;

import java.util.List;
import java.util.Map;

/**
 * 辩场：同一辩题下分正反两方，各自陈词，点赞即票。
 * 复用 wushan_discourse（topic 区分辩题、side 区分立场），不另起新表。
 */
public interface DebateService {

    /** 辩场列表：按辩题分组，含正反双方语录与票数 */
    List<Map<String, Object>> list();

    /** 发起辩论（正方立论） */
    Map<String, Object> publish(String token, String topic, String quote);

    /** 应战 / 附论：side 0=正方 1=反方 */
    Map<String, Object> reply(String token, String topic, Integer side, String quote);
}
