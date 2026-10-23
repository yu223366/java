package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 宝库秘境实体，对应表 wushan_realm。
 */
@Data
@TableName("wushan_realm")
public class Realm implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 秘境名 */
    private String name;

    /** 五行 key */
    private String element;

    /** 品阶 key */
    private String gradeKey;

    /** 层数（如：九层） */
    private String layers;

    /** 秘境简介 */
    private String summary;

    /** 排序 */
    private Integer sort;

    /** 状态：1 开启 / 0 封闭 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
