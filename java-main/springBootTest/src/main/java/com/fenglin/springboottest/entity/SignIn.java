package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日签到，对应表 wushan_sign_in（user_id + sign_date 唯一）。
 */
@Data
@TableName("wushan_sign_in")
public class SignIn implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 签到日期 */
    private LocalDate signDate;

    /** 连续签到天数 */
    private Integer continuousDays;

    /** 本次所得灵石 */
    private Integer rewardSpirit;

    private LocalDateTime createTime;
}
