package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.FortuneEvent;
import org.apache.ibatis.annotations.Mapper;

/** 秘境奇遇事件 Mapper。 */
@Mapper
public interface FortuneEventMapper extends BaseMapper<FortuneEvent> {
}
