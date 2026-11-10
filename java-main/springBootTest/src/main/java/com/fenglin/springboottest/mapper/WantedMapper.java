package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.Wanted;
import org.apache.ibatis.annotations.Mapper;

/** 求购挂单 Mapper。 */
@Mapper
public interface WantedMapper extends BaseMapper<Wanted> {
}
