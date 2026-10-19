package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 市集物品 VO，字段与前端契约一致：
 * { id, title, desc, element, grade, gradeLabel, categoryLabel, price, stock, seller, sold }
 */
@Data
public class MarketItemVo {

    private Long id;

    /** 物品名 */
    private String title;

    /** 简介 */
    private String desc;

    /** 五行 key */
    private String element;

    /** 品阶 key */
    private String grade;

    /** 品阶名（如「天品」） */
    private String gradeLabel;

    /** 分类名（如「灵药」） */
    private String categoryLabel;

    /** 售价（灵石） */
    private Integer price;

    /** 库存 */
    private Integer stock;

    /** 摊主 */
    private String seller;

    /** 已售 */
    private Integer sold;
}
