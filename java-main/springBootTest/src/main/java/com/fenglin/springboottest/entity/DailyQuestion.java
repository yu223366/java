package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日一题题目，对应表 wushan_daily_question。
 * 当天题目落库，保证所有山友面对的是同一道题。
 */
@Data
@TableName("wushan_daily_question")
public class DailyQuestion implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate questionDate;

    /** 题干 */
    private String question;

    /** 选项 JSON 数组，如 ["甲","乙","丙","丁"] */
    private String optionsJson;

    /** 正确选项下标 */
    private Integer answerIdx;

    /** 答对奖励灵石 */
    private Integer rewardSpirit;

    private LocalDateTime createTime;
}
