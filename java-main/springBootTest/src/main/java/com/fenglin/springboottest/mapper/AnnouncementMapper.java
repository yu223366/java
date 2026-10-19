package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/** 山门公告 Mapper。 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
