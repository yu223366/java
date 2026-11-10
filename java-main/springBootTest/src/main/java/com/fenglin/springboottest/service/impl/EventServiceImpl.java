package com.fenglin.springboottest.service.impl;

import com.fenglin.springboottest.entity.Announcement;
import com.fenglin.springboottest.mapper.AnnouncementMapper;
import com.fenglin.springboottest.service.EventService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 大事记实现：写入 wushan_announcement。
 * sort 固定 0 —— 首页按 sort 升序取公告，新事件自然排在最前。
 */
@Service
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private final AnnouncementMapper announcementMapper;

    public EventServiceImpl(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    @Override
    public void record(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        try {
            Announcement a = new Announcement();
            a.setText(text.length() > 500 ? text.substring(0, 500) : text);
            a.setSort(0);
            a.setStatus(1);
            a.setCreateTime(LocalDateTime.now());
            announcementMapper.insert(a);
        } catch (Exception e) {
            // 记大事失败不应影响主流程
            log.warn("写入山门大事记失败：{}", e.getMessage());
        }
    }
}
