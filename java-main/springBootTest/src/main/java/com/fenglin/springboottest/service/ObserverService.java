package com.fenglin.springboottest.service;

import java.util.Map;

/**
 * 观山台：全站实时数据总览。
 */
public interface ObserverService {

    /** 总览数据（在线人数、成交、热搜、论道、大事记） */
    Map<String, Object> overview();

    /** 导航栏徽标：各入口的实时状态 */
    Map<String, Object> badges(String authorization);
}
