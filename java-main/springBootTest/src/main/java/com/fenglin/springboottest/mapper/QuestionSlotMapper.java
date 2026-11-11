package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.QuestionSlot;
import org.apache.ibatis.annotations.Mapper;

/** 每旬一题（十分钟一轮） Mapper。 */
@Mapper
public interface QuestionSlotMapper extends BaseMapper<QuestionSlot> {
}
