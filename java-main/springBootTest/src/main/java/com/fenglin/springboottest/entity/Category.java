package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 藏经分类实体，对应表 wushan_category。
 * parent_id 自关联实现分级：一级（功法 / 心得 / 修炼资源）→ 二级（心法 / 身法 / …）。
 */
@Data
@TableName("wushan_category")
public class Category implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类 key（英文，供前端传参） */
    private String catKey;

    /** 分类名（中文） */
    private String name;

    /** 英文名 */
    private String en;

    /** 父分类 id：0 = 顶级 */
    private Long parentId;

    /** 层级：1 = 一级 2 = 二级 */
    private Integer catLevel;

    /** 排序 */
    private Integer sort;

    /** 状态：1 启用 / 0 下架 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
