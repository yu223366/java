package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.AuctionResult;
import org.apache.ibatis.annotations.Mapper;

/** 拍卖成交记录 Mapper。 */
@Mapper
public interface AuctionResultMapper extends BaseMapper<AuctionResult> {
}
