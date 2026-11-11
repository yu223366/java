package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 辩场点赞（一人一赞）实体，对应表 wushan_discourse_like。
 */
@Data
@TableName("wushan_discourse_like")
public class DiscourseLike implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 陈词 id */
    private Long discourseId;

    /** 点赞人 id */
    private Long userId;

    /** 创建时间 */
    private LocalDateTime createTime;

}
