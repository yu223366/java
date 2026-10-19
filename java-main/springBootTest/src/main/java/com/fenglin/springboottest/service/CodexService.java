package com.fenglin.springboottest.service;

import com.fenglin.springboottest.controller.dto.ItemVo;
import com.fenglin.springboottest.controller.dto.PageVo;
import com.fenglin.springboottest.controller.dto.RealmVo;
import com.fenglin.springboottest.controller.dto.SpiritRootVo;

/**
 * 藏经阁业务：功法 / 心得 / 修炼资源条目 + 灵根 + 秘境。
 * 对接前端 src/api/codex.js 的接口契约（BASE_URL=/api）。
 */
public interface CodexService {

    /**
     * 条目检索（功法 / 心得 / 修炼资源，数据源 wushan_item）。
     * @param element  五行筛选（all / 空 = 不限）
     * @param category 分类筛选，传一级分类（如 gongfa）自动下钻其子类
     * @param grade    品阶精确筛选（all / 空 = 不限）
     * @param q        关键词（模糊匹配条目名 / 简介）
     */
    PageVo<ItemVo> items(String element, String category, String grade, String q);

    /** 灵根列表（element / grade 筛选） */
    PageVo<SpiritRootVo> spiritRoots(String element, String grade);

    /** 秘境列表（element / grade 筛选） */
    PageVo<RealmVo> realms(String element, String grade);
}
