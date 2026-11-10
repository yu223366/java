package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 宗门，对应表 wushan_sect。
 */
@Data
@TableName("wushan_sect")
public class Sect implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 宗门名 */
    private String name;

    /** 宗门五行 */
    private String element;

    /** 宗主 id */
    private Long leaderId;

    /** 宗主名号 */
    private String leaderName;

    /** 宗门公告 */
    private String notice;

    /** 创建时间 */
    private LocalDateTime createTime;
}
