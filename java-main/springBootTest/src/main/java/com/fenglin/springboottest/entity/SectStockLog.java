package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 宗门宝库流水实体，对应表 wushan_sect_stock_log。
 */
@Data
@TableName("wushan_sect_stock_log")
public class SectStockLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 宗门 id */
    private Long sectId;

    /** 山友 id */
    private Long userId;

    /** 山友名号 */
    private String username;

    /** 物品名 */
    private String itemTitle;

    /** 贡献点变动 */
    private Integer amount;

    /** redeem / deposit */
    private String action;

    /** 创建时间 */
    private LocalDateTime createTime;

}
