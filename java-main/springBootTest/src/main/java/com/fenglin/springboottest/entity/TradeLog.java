package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 市集交易日志实体，对应表 wushan_trade_log。
 */
@Data
@TableName("wushan_trade_log")
public class TradeLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物品 id */
    private Long itemId;

    /** 物品名（冗余） */
    private String itemTitle;

    /** 买家 id（未登录为空） */
    private Long buyerId;

    /** 买家名号 */
    private String buyer;

    /** 购买数量 */
    private Integer buyCount;

    /** 总价（灵石） */
    private Integer totalPrice;

    private LocalDateTime createTime;
}
