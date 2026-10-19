package com.fenglin.springboottest.service;

import com.fenglin.springboottest.controller.dto.*;

import java.util.List;
import java.util.Map;

/**
 * 首页业务接口，对接前端 src/api/home.js 的契约。
 */
public interface HomeService {

    /** 当前登录用户信息（依据 Authorization: Bearer token 解析） */
    UserInfoVo getUserInfo(String token);

    /** 导航栏菜单 [{ key, label, en }] */
    List<Map<String, Object>> getNav();

    /** 热门搜索词 [{ keyword, element }] */
    List<Map<String, Object>> getHotKeywords();

    /** 山门公告 [{ id, text }] */
    List<Map<String, Object>> getAnnouncements();

    /** 山中四时图卷 [{ key, title, en, season, desc, tint }] */
    List<Map<String, Object>> getScenes();

    /** 山友论道榜 [{ id, name, element, quote, zan }] */
    List<Map<String, Object>> getDiscourse();

    /** 分类树 [{ key, label, en, children: [...] }]（分级） */
    List<CategoryVo> getCategories();

    /** 品阶字典 [{ key, label, rank, color }]（分级） */
    List<GradeVo> getGrades();

    /** 搜索联想：返回条目名列表（最多 6 条） */
    List<String> suggest(String q);

    /**
     * 组合搜索：关键词 + 五行 + 品阶（精确 / 该品及以上）+ 分类（支持父类含子类）。
     */
    SearchResultVo search(SearchRequest req, Long userId, String username);

    /* ------------------------------ 论道堂（导航「论道」） ------------------------------ */

    /** 论道分页列表（按点赞数降序） */
    PageVo<DiscourseVo> discourseList(Integer page, Integer size);

    /** 点赞一条语录，返回最新 zan */
    DiscourseVo likeDiscourse(Long id);

    /** 发表语录（依据 Bearer token 取山友名号与五行），返回新语料 */
    DiscourseVo publishDiscourse(String token, String quote);

    /* ------------------------------ 市集（导航「市集」） ------------------------------ */

    /** 市集物品分页列表：关键词 + 五行 + 品阶 + 分类（父类含子类） */
    PageVo<MarketItemVo> marketList(String keyword, String element, String grade,
                                   String category, Integer page, Integer size);

    /** 购买市集物品：校验库存 → 扣库存/累计已售 → 记交易日志，返回最新物品信息 */
    MarketItemVo buyMarket(Long id, Integer count, Long userId, String username);
}
