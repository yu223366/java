package com.fenglin.springboottest.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fenglin.springboottest.entity.AuctionNotice;
import org.apache.ibatis.annotations.Mapper;

/** 开拍通知 Mapper。 */
@Mapper
public interface AuctionNoticeMapper extends BaseMapper<AuctionNotice> {
}
