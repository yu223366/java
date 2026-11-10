package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.AuctionBid;
import org.apache.ibatis.annotations.Mapper;

/** 拍卖出价流水 Mapper。 */
@Mapper
public interface AuctionBidMapper extends BaseMapper<AuctionBid> {
}
