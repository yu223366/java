package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 每旬一题作答实体，对应表 wushan_question_answer。
 */
@Data
@TableName("wushan_question_answer")
public class QuestionAnswer implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 山友 id */
    private Long userId;

    /** 时间槽 key */
    private String slotKey;

    /** 所选择下标 */
    private Integer answerIdx;

    /** 是否正确 */
    private Integer isCorrect;

    /** 所得灵石 */
    private Integer rewardSpirit;

    /** 创建时间 */
    private LocalDateTime createTime;

}
