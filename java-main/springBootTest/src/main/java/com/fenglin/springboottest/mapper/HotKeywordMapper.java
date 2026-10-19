package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.HotKeyword;
import org.apache.ibatis.annotations.Mapper;

/** 热门搜索词 Mapper。 */
@Mapper
public interface HotKeywordMapper extends BaseMapper<HotKeyword> {
}
