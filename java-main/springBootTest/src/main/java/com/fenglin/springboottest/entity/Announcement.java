package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 山门公告实体，对应表 wushan_announcement。
 */
@Data
@TableName("wushan_announcement")
public class Announcement implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公告内容 */
    private String text;

    /** 排序 */
    private Integer sort;

    /** 状态：1 展示 / 0 隐藏 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
