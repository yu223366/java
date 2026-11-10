package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 拍卖关注，对应表 wushan_auction_watch。
 * 关注某件拍品后，该物再次上拍时会收到捡漏提醒。
 */
@Data
@TableName("wushan_auction_watch")
public class AuctionWatch implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关注人 id */
    private Long userId;

    /** 关注的拍品名 */
    private String itemTitle;

    private LocalDateTime createTime;
}
