package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/** 论道请求体：点赞 { id } / 发表 { quote } */
@Data
public class DiscourseRequest {

    /** 点赞目标 id */
    private Long id;

    /** 发表的语录 */
    private String quote;
}
