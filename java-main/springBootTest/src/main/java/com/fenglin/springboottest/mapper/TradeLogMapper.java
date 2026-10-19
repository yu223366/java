package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.TradeLog;
import org.apache.ibatis.annotations.Mapper;

/** 市集交易日志 Mapper */
@Mapper
public interface TradeLogMapper extends BaseMapper<TradeLog> {
}
