package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 论道语料 VO，字段与前端契约一致：{ id, name, element, quote, zan }。
 */
@Data
public class DiscourseVo {

    private Long id;

    /** 山友名号 */
    private String name;

    /** 五行 key */
    private String element;

    /** 语录 */
    private String quote;

    /** 点赞数 */
    private Integer zan;
}
