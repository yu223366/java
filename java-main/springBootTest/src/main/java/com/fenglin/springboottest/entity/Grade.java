package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 品阶实体，对应表 wushan_grade。
 * grade_rank 1~6 由低到高：凡品 → 黄品 → 玄品 → 地品 → 天品 → 圣品，
 * 支持按「该品及以上」分级筛选。
 */
@Data
@TableName("wushan_grade")
public class Grade implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 品阶 key：fan / huang / xuan / di / tian / sheng */
    private String gradeKey;

    /** 品阶名 */
    private String name;

    /** 等第：1 凡 … 6 圣 */
    private Integer gradeRank;

    /** 前端展示色 */
    private String color;

    /** 排序 */
    private Integer sort;

    /** 状态：1 启用 / 0 下架 */
    private Integer status;
}
