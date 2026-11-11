package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 宗门宝库库存实体，对应表 wushan_sect_stock。
 */
@Data
@TableName("wushan_sect_stock")
public class SectStock implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 宗门 id */
    private Long sectId;

    /** 物品 id */
    private Long itemId;

    /** 物品名 */
    private String itemTitle;

    /** 库存 */
    private Integer count;

    /** 兑换所需贡献点 */
    private Integer contributionPrice;

    /** 1=在架 0=下架 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

}
