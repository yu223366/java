package com.fenglin.springboottest.service;

import java.util.List;
import java.util.Map;

/**
 * 宗门：山友自发结阵，共守一方山头。
 */
public interface SectService {

    /** 宗门列表（含成员数） */
    List<Map<String, Object>> list();

    /** 我所在的宗门（成员、贡献、公告） */
    Map<String, Object> mine(String authorization);

    /** 开山立宗 */
    Map<String, Object> create(String authorization, String name, String element);

    /** 投山门 */
    Map<String, Object> join(String authorization, Long sectId);

    /** 离山 */
    Map<String, Object> quit(String authorization);

    /** 捐献灵石，累积贡献 */
    Map<String, Object> donate(String authorization, Integer amount);

    /** 宗主传位给某位成员 */
    Map<String, Object> transfer(String authorization, Long targetUserId);

    /** 宗主解散宗门 */
    Map<String, Object> disband(String authorization);
}
