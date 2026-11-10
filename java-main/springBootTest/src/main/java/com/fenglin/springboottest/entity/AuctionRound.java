package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 拍卖场次，对应表 wushan_auction_round。
 */
@Data
@TableName("wushan_auction_round")
public class AuctionRound implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 场次号（递增） */
    private Integer roundNo;

    /** 0=未开始 1=进行中 2=已结束 */
    private Integer status;

    /** 本场拍品件数 */
    private Integer itemCount;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;
}
