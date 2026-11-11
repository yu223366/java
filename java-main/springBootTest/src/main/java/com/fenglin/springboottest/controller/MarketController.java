package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.MarketService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 雾山市集控制器，对接前端 src/api/market.js。
 * 前缀 /api/market。
 *
 * 接口清单：
 *   GET  /api/market/shops                 摊位字典
 *   GET  /api/market/items                物品列表 ?keyword=&element=&grade=&shop=&category=&page=&size=
 *   GET  /api/market/item/{id}            单件详情
 *   POST /api/market/buy                  购买 { id, count }（扣灵石 + 入背包）
 */
@RestController
@RequestMapping("/api/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @GetMapping("/shops")
    public Result<List<Map<String, Object>>> shops() {
        return Result.ok(marketService.shops());
    }

    @GetMapping("/items")
    public Result<Map<String, Object>> items(@RequestParam(required = false) String keyword,
                                             @RequestParam(required = false) String element,
                                             @RequestParam(required = false) String grade,
                                             @RequestParam(required = false) String shop,
                                             @RequestParam(required = false) String category,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) Integer size) {
        return Result.ok(marketService.items(keyword, element, grade, shop, category, page, size));
    }

    @GetMapping("/item/{id}")
    public Result<Map<String, Object>> detail(@PathVariable("id") Long id) {
        return Result.ok(marketService.detail(id));
    }

    @PostMapping("/buy")
    public Result<Map<String, Object>> buy(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Long id = body == null || body.get("id") == null ? null
                : Long.valueOf(String.valueOf(body.get("id")).replaceAll("\\.0+$", ""));
        Integer count = body == null || body.get("count") == null ? null
                : Integer.valueOf(String.valueOf(body.get("count")).replaceAll("\\.0+$", ""));
        return Result.ok(marketService.buy(authorization, id, count));
    }
}
