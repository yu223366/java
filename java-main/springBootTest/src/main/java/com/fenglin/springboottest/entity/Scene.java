package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 山中四时图卷实体，对应表 wushan_scene。
 */
@Data
@TableName("wushan_scene")
public class Scene implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 图卷 key */
    private String sceneKey;

    /** 图卷名 */
    private String title;

    /** 英文名 */
    private String en;

    /** 季节：春 / 夏 / 秋 / 冬 */
    private String season;

    /** 图卷描述 */
    private String summary;

    /** 主色调 */
    private String tint;

    /** 排序 */
    private Integer sort;

    /** 状态：1 展示 / 0 隐藏 */
    private Integer status;
}
