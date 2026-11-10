package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.AuctionWatch;
import org.apache.ibatis.annotations.Mapper;

/** 拍卖关注 Mapper。 */
@Mapper
public interface AuctionWatchMapper extends BaseMapper<AuctionWatch> {
}
