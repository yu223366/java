package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 首页搜索请求。
 * 支持关键词 + 五行 + 品阶 + 分类的组合筛选：
 *   keyword   关键词（模糊匹配条目名 / 简介）
 *   element   五行 key（jin/mu/shui/huo/tu/feng/lei/dian/taiji/yinyang，all = 不限）
 *   grade     品阶精确匹配（fan/huang/xuan/di/tian/sheng）
 *   gradeMin  「该品及以上」分级筛选：如传 di 则返回地品/天品/圣品
 *   category  分类 key（支持分级：传一级分类如 gongfa 时自动包含其全部子类）
 *   page/size 分页（默认 1 / 20，size 上限 50）
 */
@Data
public class SearchRequest {

    /** 关键词 */
    private String keyword;

    /** 五行 key */
    private String element;

    /** 品阶 key（精确） */
    private String grade;

    /** 品阶 key（该品及以上） */
    private String gradeMin;

    /** 分类 key */
    private String category;

    /** 页码，从 1 起 */
    private Integer page;

    /** 每页条数 */
    private Integer size;
}
