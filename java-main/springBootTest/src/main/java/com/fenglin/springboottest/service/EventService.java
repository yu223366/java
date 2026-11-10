package com.fenglin.springboottest.service;

/**
 * 山门大事记：把系统里发生的大事写成山门公告，
 * 首页公告栏（/api/home/announcements）直接复用，无需额外接口。
 */
public interface EventService {

    /**
     * 记录一条大事（自动排在公告栏最前）。
     *
     * @param text 公告正文
     */
    void record(String text);
}
