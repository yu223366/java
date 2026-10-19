package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 搜索结果。
 * items 字段名与前端 home.js 契约一致：{ total, items: [...] }。
 */
@Data
public class SearchResultVo {

    /** 命中总数 */
    private long total;

    /** 当前页码 */
    private long page;

    /** 每页条数 */
    private long size;

    /** 结果条目 */
    private List<ItemVo> items;

    public static SearchResultVo of(long total, long page, long size, List<ItemVo> items) {
        SearchResultVo vo = new SearchResultVo();
        vo.total = total;
        vo.page = page;
        vo.size = size;
        vo.items = items;
        return vo;
    }
}
