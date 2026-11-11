package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.TreasureService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 宗门宝库 + 山友寄售控制器，对接前端 src/api/treasure.js。
 * 前缀 /api/treasure。
 *
 * 接口清单：
 *   GET  /api/treasure/vault            本宗宝库（宗门信息 + 贡献点 + 库存）
 *   POST /api/treasure/deposit          存入宗库 { itemId, count, contributionPrice }
 *   POST /api/treasure/redeem           贡献点兑换 { stockId, count }
 *   GET  /api/treasure/consigns         寄售区在架列表
 *   GET  /api/treasure/consigns/mine    我的挂单
 *   POST /api/treasure/consigns/publish 挂出寄售
 *   POST /api/treasure/consigns/buy     灵石购买 { consignId }
 *   POST /api/treasure/consigns/barter  以物换物 { consignId, offerItemId }
 *   POST /api/treasure/consigns/cancel  撤销挂单 { consignId }
 */
@RestController
@RequestMapping("/api/treasure")
public class TreasureController {

    private final TreasureService treasureService;

    public TreasureController(TreasureService treasureService) {
        this.treasureService = treasureService;
    }

    @GetMapping("/vault")
    public Result<Map<String, Object>> vault(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(treasureService.vault(authorization));
    }

    @PostMapping("/deposit")
    public Result<Map<String, Object>> deposit(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        return Result.ok(treasureService.deposit(authorization,
                asLong(body.get("itemId")), asInt(body.get("count")),
                asInt(body.get("contributionPrice"))));
    }

    @PostMapping("/redeem")
    public Result<Map<String, Object>> redeem(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        return Result.ok(treasureService.redeem(authorization,
                asLong(body.get("stockId")), asInt(body.get("count"))));
    }

    @GetMapping("/consigns")
    public Result<List<Map<String, Object>>> consigns(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(treasureService.consigns(authorization));
    }

    @GetMapping("/consigns/mine")
    public Result<List<Map<String, Object>>> myConsigns(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(treasureService.myConsigns(authorization));
    }

    @PostMapping("/consigns/publish")
    public Result<Map<String, Object>> publish(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        return Result.ok(treasureService.publish(authorization,
                asLong(body.get("itemId")), asInt(body.get("count")),
                asInt(body.get("priceSpirit")), asLong(body.get("barterItemId")),
                asString(body.get("barterTitle")), asInt(body.get("mode"))));
    }

    @PostMapping("/consigns/buy")
    public Result<Map<String, Object>> buy(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        return Result.ok(treasureService.buy(authorization, asLong(body.get("consignId"))));
    }

    @PostMapping("/consigns/barter")
    public Result<Map<String, Object>> barter(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        return Result.ok(treasureService.barter(authorization,
                asLong(body.get("consignId")), asLong(body.get("offerItemId"))));
    }

    @PostMapping("/consigns/cancel")
    public Result<Map<String, Object>> cancel(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        return Result.ok(treasureService.cancel(authorization, asLong(body.get("consignId"))));
    }

    /* ------------------------------ 参数转换 ------------------------------ */
    /** Jackson 把 JSON 数字解成 Integer/Long/Double，统一转字符串再解析，避免 ClassCastException */
    private static Long asLong(Object o) {
        if (o == null || "null".equals(String.valueOf(o))) return null;
        try {
            return Long.valueOf(String.valueOf(o).trim().replaceAll("\\.0+$", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static Integer asInt(Object o) {
        if (o == null || "null".equals(String.valueOf(o))) return null;
        try {
            return Integer.valueOf(String.valueOf(o).trim().replaceAll("\\.0+$", ""));
        } catch (Exception e) {
            return null;
        }
    }

    private static String asString(Object o) {
        if (o == null || "null".equals(String.valueOf(o))) return null;
        return String.valueOf(o);
    }
}
