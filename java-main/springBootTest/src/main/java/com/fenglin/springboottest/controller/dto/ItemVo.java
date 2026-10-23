package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 条目展示 VO —— 字段名对齐前端契约：
 * HomePanel 用 { id, title, desc, element }，宝库另可读 category / grade。
 */
@Data
public class ItemVo {

    private Long id;

    /** 条目名 */
    private String title;

    /** 条目简介 */
    private String desc;

    /** 五行 key */
    private String element;

    /** 分类 key（如 gongfa / xinfa / lingyao） */
    private String category;

    /** 分类中文名 */
    private String categoryLabel;

    /** 品阶 key（fan/huang/xuan/di/tian/sheng） */
    private String grade;

    /** 品阶中文名 */
    private String gradeLabel;
}
