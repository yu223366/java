package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DailyAnswer;
import org.apache.ibatis.annotations.Mapper;

/** 每日一题作答记录 Mapper。 */
@Mapper
public interface DailyAnswerMapper extends BaseMapper<DailyAnswer> {
}
