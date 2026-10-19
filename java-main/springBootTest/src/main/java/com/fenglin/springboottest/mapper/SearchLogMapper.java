package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.SearchLog;
import org.apache.ibatis.annotations.Mapper;

/** 搜索日志 Mapper。 */
@Mapper
public interface SearchLogMapper extends BaseMapper<SearchLog> {
}
