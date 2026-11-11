package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 练功记录实体，对应表 wushan_practice。
 */
@Data
@TableName("wushan_practice")
public class Practice implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 山友 id */
    private Long userId;

    /** 练功重数 */
    private Integer level;

    /** 本重修为 */
    private Integer exp;

    /** 累计次数 */
    private Integer totalCount;

    /** 累计所得灵石 */
    private Integer totalSpirit;

    /** 上次练功时间 */
    private LocalDateTime lastTime;

    /** 创建时间 */
    private LocalDateTime createTime;

}
