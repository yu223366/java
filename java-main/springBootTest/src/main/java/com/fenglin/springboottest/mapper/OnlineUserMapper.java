package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.OnlineUser;
import org.apache.ibatis.annotations.Mapper;

/** 在线用户 Mapper。 */
@Mapper
public interface OnlineUserMapper extends BaseMapper<OnlineUser> {
}
