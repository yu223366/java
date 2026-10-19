package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 首页导航实体，对应表 wushan_nav。
 */
@Data
@TableName("wushan_nav")
public class HomeNav implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 导航 key */
    private String navKey;

    /** 中文名 */
    private String label;

    /** 英文名 */
    private String en;

    /** 排序 */
    private Integer sort;

    /** 状态：1 启用 / 0 隐藏 */
    private Integer status;
}
