package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.UserBag;
import org.apache.ibatis.annotations.Mapper;

/** 山友背包 Mapper。 */
@Mapper
public interface UserBagMapper extends BaseMapper<UserBag> {
}
