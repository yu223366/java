package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Discourse;
import org.apache.ibatis.annotations.Mapper;

/** 山友论道榜 Mapper。 */
@Mapper
public interface DiscourseMapper extends BaseMapper<Discourse> {
}
