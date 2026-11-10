package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.AuctionRound;
import org.apache.ibatis.annotations.Mapper;

/** 拍卖场次 Mapper。 */
@Mapper
public interface AuctionRoundMapper extends BaseMapper<AuctionRound> {
}
