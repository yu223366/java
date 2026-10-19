package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 品阶 VO：{ key, label, rank, color }。
 */
@Data
public class GradeVo {

    private String key;

    private String label;

    /** 等第 1~6，可用于「该品及以上」分级筛选 */
    private Integer rank;

    private String color;
}
