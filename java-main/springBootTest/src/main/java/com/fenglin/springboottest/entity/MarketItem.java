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

    /** 详细介绍（市集点开后展示的全文） */
    private String detail;

    /** 获取途径 */
    private String acquire;

    /** 摊位 key：dan_cai 丹药材料铺 / qi_cai 炼器材料铺 / zhen_cai 阵法材料铺 / danyao / faqi / zahuo */
    private String shopKey;

    /** 对应背包物品 id（购得后入包用） */
    private Long itemRef;

    /** 品质基准价（灵石） */
    private Integer gradePrice;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
