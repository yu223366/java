package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.QuestionAnswer;
import org.apache.ibatis.annotations.Mapper;

/** 每旬一题作答 Mapper。 */
@Mapper
public interface QuestionAnswerMapper extends BaseMapper<QuestionAnswer> {
}
