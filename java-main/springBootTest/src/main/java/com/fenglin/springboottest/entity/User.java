package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 山籍（用户）实体，对应表 wushan_user。
 */
@Data
@TableName("wushan_user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 名号 */
    private String username;

    /** 暗语（BCrypt 密文） */
    private String password;

    /** 本命元素 key（正五行 + 衍生）：jin / mu / shui / huo / tu / feng / lei / dian / taiji / yinyang */
    private String element;

    /** 元素 label：金 / 木 / 水 / 火 / 土 / 风 / 雷 / 电 / 太极 / 阴阳 */
    private String elementLabel;

    /** 是否允诺山规 */
    private Boolean agreed;

    /** 允诺时间 */
    private LocalDateTime agreedAt;

    /** 状态：1 正常 / 0 禁用 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
