package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 拍卖出价流水：记录谁、加了多少、加价后的总价。
 */
@Data
@TableName("wushan_auction_bid")
public class AuctionBid implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer roundNo;
    private Long itemId;
    private String itemTitle;
    private Long userId;
    private String username;

    /** 本次加价额：10 / 50 / 100 / 500 */
    private Integer incrementAmount;

    /** 加价后的当前价 */
    private Integer price;

    private LocalDateTime bidTime;
}
