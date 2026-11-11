package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Practice;
import org.apache.ibatis.annotations.Mapper;

/** 练功记录 Mapper。 */
@Mapper
public interface PracticeMapper extends BaseMapper<Practice> {
}
