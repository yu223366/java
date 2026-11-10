package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.WorldService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 山门世界玩法控制器，对接前端 src/api/world.js。
 * 前缀 /api/world（前端 BASE_URL=/api，由 Vite 代理到 8080）。
 *
 * 接口清单：
 *   GET  /api/world/bag              我的背包
 *   GET  /api/world/realms           秘境列表
 *   POST /api/world/explore/start    进入秘境 { realmId }
 *   POST /api/world/explore/choose   选择事件选项 { choiceIdx }
 *   GET  /api/world/explore/logs     我的探索记录
 *   GET  /api/world/recipes          配方列表
 *   POST /api/world/craft            炼制 { recipeId }
 *   POST /api/world/duel/start       发起对决
 *   POST /api/world/duel/answer      应答 { duelId, choiceIdx }
 *   GET  /api/world/duel/records     对决记录
 *   GET  /api/world/inbox            收件箱
 *   POST /api/world/send             传书 { toName, content }
 *   POST /api/world/read             已读 { messageId }
 */
@RestController
@RequestMapping("/api/world")
public class WorldController {

    private final WorldService worldService;

    public WorldController(WorldService worldService) {
        this.worldService = worldService;
    }

    @GetMapping("/bag")
    public Result<Map<String, Object>> bag(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(worldService.bag(authorization));
    }

    @GetMapping("/realms")
    public Result<List<Map<String, Object>>> realms() {
        return Result.ok(worldService.realms());
    }

    @PostMapping("/explore/start")
    public Result<Map<String, Object>> exploreStart(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("realmId") Long realmId) {
        return Result.ok(worldService.exploreStart(authorization, realmId));
    }

    @PostMapping("/explore/choose")
    public Result<Map<String, Object>> exploreChoose(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("choiceIdx") Integer choiceIdx) {
        return Result.ok(worldService.exploreChoose(authorization, choiceIdx));
    }

    @GetMapping("/explore/logs")
    public Result<List<Map<String, Object>>> exploreLogs(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(worldService.exploreLogs(authorization));
    }

    @GetMapping("/recipes")
    public Result<List<Map<String, Object>>> recipes(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(worldService.recipes(authorization));
    }

    @PostMapping("/craft")
    public Result<Map<String, Object>> craft(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("recipeId") Long recipeId) {
        return Result.ok(worldService.craft(authorization, recipeId));
    }

    @PostMapping("/duel/start")
    public Result<Map<String, Object>> duelStart(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(worldService.duelStart(authorization));
    }

    @PostMapping("/duel/answer")
    public Result<Map<String, Object>> duelAnswer(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("duelId") Long duelId,
            @RequestParam("choiceIdx") Integer choiceIdx) {
        return Result.ok(worldService.duelAnswer(authorization, duelId, choiceIdx));
    }

    @GetMapping("/duel/records")
    public Result<List<Map<String, Object>>> duelRecords(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(worldService.duelRecords(authorization));
    }

    @GetMapping("/inbox")
    public Result<Map<String, Object>> inbox(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(worldService.inbox(authorization));
    }

    @PostMapping("/send")
    public Result<Map<String, Object>> send(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("toName") String toName,
            @RequestParam("content") String content) {
        return Result.ok(worldService.send(authorization, toName, content));
    }

    @PostMapping("/read")
    public Result<Map<String, Object>> read(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("messageId") Long messageId) {
        return Result.ok(worldService.read(authorization, messageId));
    }

    /** 一键全部已读 */
    @PostMapping("/readAll")
    public Result<Map<String, Object>> readAll(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(worldService.readAll(authorization));
    }
}
