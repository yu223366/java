package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.SpiritLog;
import org.apache.ibatis.annotations.Mapper;

/** 灵石流水 Mapper。 */
@Mapper
public interface SpiritLogMapper extends BaseMapper<SpiritLog> {
}
