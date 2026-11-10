package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 每日一题作答请求。
 */
@Data
public class DailyAnswerReq {

    /** 所选选项下标 */
    private Integer answerIdx;
}
