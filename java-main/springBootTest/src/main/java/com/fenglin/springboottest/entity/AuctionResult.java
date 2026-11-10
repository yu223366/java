package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 拍卖成交记录：落槌后归出价最高者，流拍时 winner 为空。
 */
@Data
@TableName("wushan_auction_result")
public class AuctionResult implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer roundNo;
    private Long itemId;
    private String itemTitle;
    private Long winnerId;
    private String winnerName;
    private Integer finalPrice;
    private LocalDateTime hammerTime;
}
