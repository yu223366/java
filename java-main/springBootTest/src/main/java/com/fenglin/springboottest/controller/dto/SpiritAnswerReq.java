package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 测灵根作答请求：按题序给出每题选中的选项下标。
 */
@Data
public class SpiritAnswerReq {

    /** 8 题的选择下标，例如 [0,2,1,0,3,1,2,0] */
    private List<Integer> answers;
}
