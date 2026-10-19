package com.fenglin.springboottest.controller.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用分页结果（论道 / 市集列表共用）。
 * 字段与前端契约一致：{ total, page, size, items: [...] }。
 */
@Data
public class PageVo<T> {

    /** 命中总数 */
    private long total;

    /** 当前页码 */
    private long page;

    /** 每页条数 */
    private long size;

    /** 结果条目 */
    private List<T> items;

    public static <T> PageVo<T> of(long total, long page, long size, List<T> items) {
        PageVo<T> vo = new PageVo<>();
        vo.total = total;
        vo.page = page;
        vo.size = size;
        vo.items = items;
        return vo;
    }
}
