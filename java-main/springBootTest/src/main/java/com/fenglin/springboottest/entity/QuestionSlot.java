package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 每旬一题（十分钟一轮）实体，对应表 wushan_question_slot。
 */
@Data
@TableName("wushan_question_slot")
public class QuestionSlot implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 时间槽 key */
    private String slotKey;

    /** 题干 */
    private String question;

    /** 选项 JSON */
    private String optionsJson;

    /** 正确选项下标 */
    private Integer answerIdx;

    /** 答对奖励 */
    private Integer rewardSpirit;

    /** 创建时间 */
    private LocalDateTime createTime;

}
