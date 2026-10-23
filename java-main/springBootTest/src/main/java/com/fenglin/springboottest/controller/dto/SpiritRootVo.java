package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 宝库灵根 VO（灵根页签）。
 * 字段与前端契约一致：{ id, name, element, grade, gradeLabel, desc }。
 */
@Data
public class SpiritRootVo {

    private Long id;

    /** 灵根名 */
    private String name;

    /** 五行 key */
    private String element;

    /** 品阶 key */
    private String grade;

    /** 品阶中文名 */
    private String gradeLabel;

    /** 灵根简介 */
    private String desc;
}
