package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Message;
import org.apache.ibatis.annotations.Mapper;

/** 传书私信 Mapper。 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
