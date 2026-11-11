package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 宝库山友寄售实体，对应表 wushan_consign。
 */
@Data
@TableName("wushan_consign")
public class Consign implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 寄售人 id */
    private Long sellerId;

    /** 寄售人名号 */
    private String sellerName;

    /** 寄售物品 id */
    private Long itemId;

    /** 寄售物品名 */
    private String itemTitle;

    /** 数量 */
    private Integer count;

    /** 灵石价 */
    private Integer priceSpirit;

    /** 想换到的物品 id */
    private Long barterItemId;

    /** 想换到的物品名 */
    private String barterTitle;

    /** 0只卖 1只换 2皆可 */
    private Integer mode;

    /** 0在架 1成交 2撤销 3取回 */
    private Integer status;

    /** 买家 id */
    private Long buyerId;

    /** 买家名号 */
    private String buyerName;

    /** 创建时间 */
    private LocalDateTime createTime;

}
