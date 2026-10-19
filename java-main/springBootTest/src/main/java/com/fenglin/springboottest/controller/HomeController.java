package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.*;
import com.fenglin.springboottest.service.HomeService;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 首页控制器，对接前端 src/api/home.js 的接口契约。
 * 所有接口前缀 /api/home（前端 BASE_URL=/api，由 Vite 代理到 8080）。
 *
 * 接口清单：
 *   GET  /api/home/user/info     当前登录用户信息（Bearer token）
 *   GET  /api/home/nav           导航栏菜单
 *   GET  /api/home/hot-keywords  热门搜索词
 *   GET  /api/home/announcements 山门公告
 *   GET  /api/home/scenes        山中四时图卷
 *   GET  /api/home/discourse     山友论道榜
 *   GET  /api/home/categories    分类树（分级）
 *   GET  /api/home/grades        品阶字典（分级）
 *   GET  /api/home/search/suggest?q=xxx   搜索联想
 *   POST /api/home/search        组合搜索（关键词 + 五行 + 品阶 + 分类，支持分页）
 */
@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final HomeService homeService;

    public HomeController(HomeService homeService) {
        this.homeService = homeService;
    }

    /** 当前登录用户信息 */
    @GetMapping("/user/info")
    public Result<UserInfoVo> userInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(homeService.getUserInfo(authorization));
    }

    /** 导航栏菜单 */
    @GetMapping("/nav")
    public Result<List<Map<String, Object>>> nav() {
        return Result.ok(homeService.getNav());
    }

    /** 热门搜索词 */
    @GetMapping("/hot-keywords")
    public Result<List<Map<String, Object>>> hotKeywords() {
        return Result.ok(homeService.getHotKeywords());
    }

    /** 山门公告 */
    @GetMapping("/announcements")
    public Result<List<Map<String, Object>>> announcements() {
        return Result.ok(homeService.getAnnouncements());
    }

    /** 山中四时图卷 */
    @GetMapping("/scenes")
    public Result<List<Map<String, Object>>> scenes() {
        return Result.ok(homeService.getScenes());
    }

    /** 山友论道榜 */
    @GetMapping("/discourse")
    public Result<List<Map<String, Object>>> discourse() {
        return Result.ok(homeService.getDiscourse());
    }

    /** 分类树（分级：一级含二级） */
    @GetMapping("/categories")
    public Result<List<CategoryVo>> categories() {
        return Result.ok(homeService.getCategories());
    }

    /** 品阶字典（分级） */
    @GetMapping("/grades")
    public Result<List<GradeVo>> grades() {
        return Result.ok(homeService.getGrades());
    }

    /** 搜索联想 */
    @GetMapping("/search/suggest")
    public Result<List<String>> suggest(@RequestParam(value = "q", required = false) String q) {
        return Result.ok(homeService.suggest(q));
    }

    /**
     * 组合搜索。请求体：
     * { keyword, element?, grade?, gradeMin?, category?, page?, size? }
     */
    @PostMapping("/search")
    public Result<SearchResultVo> search(@RequestBody(required = false) SearchRequest req,
                                         @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (req == null) {
            req = new SearchRequest();
        }
        // 从令牌解析搜索者（可空，不影响搜索本身）
        Long userId = null;
        String username = null;
        String token = authorization;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Object[] parsed = TokenUtil.parse(token);
        if (parsed != null) {
            userId = (Long) parsed[0];
            username = (String) parsed[1];
        }
        return Result.ok(homeService.search(req, userId, username));
    }

    /* ============================== 论道堂（导航「论道」） ============================== */

    /** 论道分页列表（按点赞数降序） */
    @GetMapping("/discourse/list")
    public Result<PageVo<DiscourseVo>> discourseList(@RequestParam(required = false) Integer page,
                                                    @RequestParam(required = false) Integer size) {
        return Result.ok(homeService.discourseList(page, size));
    }

    /** 点赞一条语录。请求体 { id } */
    @PostMapping("/discourse/like")
    public Result<DiscourseVo> likeDiscourse(@RequestBody DiscourseRequest req) {
        return Result.ok(homeService.likeDiscourse(req == null ? null : req.getId()));
    }

    /** 发表语录（需登录）。请求体 { quote } */
    @PostMapping("/discourse/publish")
    public Result<DiscourseVo> publishDiscourse(@RequestBody DiscourseRequest req,
                                                @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(homeService.publishDiscourse(authorization, req == null ? null : req.getQuote()));
    }

    /* ============================== 市集（导航「市集」） ============================== */

    /**
     * 市集物品列表。
     * 查询参数：keyword / element / grade / category（父类自动含子类）/ page / size
     */
    @GetMapping("/market/items")
    public Result<PageVo<MarketItemVo>> marketItems(@RequestParam(required = false) String keyword,
                                                   @RequestParam(required = false) String element,
                                                   @RequestParam(required = false) String grade,
                                                   @RequestParam(required = false) String category,
                                                   @RequestParam(required = false) Integer page,
                                                   @RequestParam(required = false) Integer size) {
        return Result.ok(homeService.marketList(keyword, element, grade, category, page, size));
    }

    /** 购买市集物品（扣库存 + 记交易日志）。请求体 { id, count } */
    @PostMapping("/market/buy")
    public Result<MarketItemVo> buyMarket(@RequestBody BuyRequest req,
                                          @RequestHeader(value = "Authorization", required = false) String authorization) {
        Long userId = null;
        String username = null;
        String token = authorization;
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        Object[] parsed = TokenUtil.parse(token);
        if (parsed != null) {
            userId = (Long) parsed[0];
            username = (String) parsed[1];
        }
        Long id = req == null ? null : req.getId();
        Integer count = req == null ? null : req.getCount();
        return Result.ok(homeService.buyMarket(id, count, userId, username));
    }
}
