package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日一题作答记录，对应表 wushan_daily_answer（user_id + question_date 唯一）。
 */
@Data
@TableName("wushan_daily_answer")
public class DailyAnswer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate questionDate;

    /** 所选选项下标 */
    private Integer answerIdx;

    /** 是否答对 */
    private Integer isCorrect;

    /** 所得灵石 */
    private Integer rewardSpirit;

    private LocalDateTime createTime;
}
