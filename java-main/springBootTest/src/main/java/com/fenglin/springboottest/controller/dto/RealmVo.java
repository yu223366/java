package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 宝库秘境 VO（秘境页签）。
 * 字段与前端契约一致：{ id, name, element, grade, gradeLabel, layers, desc }。
 */
@Data
public class RealmVo {

    private Long id;

    /** 秘境名 */
    private String name;

    /** 五行 key */
    private String element;

    /** 品阶 key */
    private String grade;

    /** 品阶中文名 */
    private String gradeLabel;

    /** 层数（如：九层） */
    private String layers;

    /** 秘境简介 */
    private String desc;
}
