package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.DailyQuestion;
import org.apache.ibatis.annotations.Mapper;

/** 每日一题题目 Mapper。 */
@Mapper
public interface DailyQuestionMapper extends BaseMapper<DailyQuestion> {
}
