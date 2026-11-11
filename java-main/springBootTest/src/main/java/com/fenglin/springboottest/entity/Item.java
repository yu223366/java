package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 宝库条目实体，对应表 wushan_item —— 首页搜索的对象。
 */
@Data
@TableName("wushan_item")
public class Item implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 条目名 */
    private String title;

    /** 条目简介 */
    private String summary;

    /** 五行 key：jin / mu / shui / huo / tu / feng / lei / dian / taiji / yinyang */
    private String element;

    /** 品阶 key */
    private String gradeKey;

    /** 分类 id（关联 wushan_category） */
    private Long categoryId;

    /** 排序 */
    private Integer sort;

    /** 状态：1 上架 / 0 下架 */
    private Integer status;

    /** 拍卖状态：0=未拍卖（默认） 1=已拍卖（拍卖行成交后置 1，不再入池） */
    private Integer auction;

    /** 详细介绍（宝库点开后展示的全文） */
    private String detail;

    /** 获取途径（市集 / 秘境 / 炼制 / 宗门宝库 / 寄售） */
    private String acquire;

    /** 用途与功效 */
    private String effect;

    /** 品质基准价（灵石）：凡10 黄30 玄100 地300 天900 圣2700 */
    private Integer gradePrice;

    /** 创建时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
