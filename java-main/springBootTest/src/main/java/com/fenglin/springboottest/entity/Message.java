package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 传书私信，对应表 wushan_message。
 */
@Data
@TableName("wushan_message")
public class Message implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 寄信人 id */
    private Long fromId;

    /** 寄信人名号 */
    private String fromName;

    /** 收信人 id */
    private Long toId;

    /** 收信人名号 */
    private String toName;

    /** 信文 */
    private String content;

    /** 1 已读 / 0 未读 */
    private Integer isRead;


    /** 创建时间 */
    private LocalDateTime createTime;
}
