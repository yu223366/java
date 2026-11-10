package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.SignIn;
import org.apache.ibatis.annotations.Mapper;

/** 每日签到 Mapper。 */
@Mapper
public interface SignInMapper extends BaseMapper<SignIn> {
}
