package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 宗门成员，对应表 wushan_sect_member。
 */
@Data
@TableName("wushan_sect_member")
public class SectMember implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 宗门 id */
    private Long sectId;

    /** 成员 id */
    private Long userId;

    /** 成员名号 */
    private String username;

    /** 0=弟子 1=长老 2=宗主 */
    private Integer role;

    /** 贡献值 */
    private Integer contribution;

    /** 加入时间 */
    private LocalDateTime joinTime;
}
