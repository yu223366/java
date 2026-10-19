package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 市集物品实体，对应表 wushan_market_item。
 */
@Data
@TableName("wushan_market_item")
public class MarketItem implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 物品名 */
    private String title;

    /** 物品简介 */
    private String summary;

    /** 五行 key */
    private String element;

    /** 品阶 key */
    private String gradeKey;

    /** 分类 id（关联 wushan_category） */
    private Long categoryId;

    /** 售价（灵石） */
    private Integer price;

    /** 库存 */
    private Integer stock;

    /** 摊主 */
    private String seller;

    /** 已售 */
    private Integer sold;

    /** 状态：1 在售 / 0 下架 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
