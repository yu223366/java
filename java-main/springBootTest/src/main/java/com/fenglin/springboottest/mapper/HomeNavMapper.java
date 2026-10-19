package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.HomeNav;
import org.apache.ibatis.annotations.Mapper;

/** 首页导航 Mapper。 */
@Mapper
public interface HomeNavMapper extends BaseMapper<HomeNav> {
}
