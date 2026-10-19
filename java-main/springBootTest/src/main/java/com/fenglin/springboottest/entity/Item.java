package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 藏经条目实体，对应表 wushan_item —— 首页搜索的对象。
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

    /** 创建时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
