package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 开拍通知：每场提前 1 分钟写入一条，所有在线用户轮询 status 时都能收到。
 */
@Data
@TableName("wushan_auction_notice")
public class AuctionNotice implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Integer roundNo;
    private String content;
    private LocalDateTime notifyTime;
}
