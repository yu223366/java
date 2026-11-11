package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.DebateService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 辩场控制器，对接前端 src/api/debate.js。
 * 前缀 /api/debate。
 *
 * 接口清单：
 *   GET  /api/debate/list     辩场列表
 *   POST /api/debate/publish   发起辩论 { topic, quote }
 *   POST /api/debate/reply     应战 { topic, side, quote }
 *   POST /api/debate/like      点赞 { id }（一人一票，票多者胜）
 */
@RestController
@RequestMapping("/api/debate")
public class DebateController {

    private final DebateService debateService;

    public DebateController(DebateService debateService) {
        this.debateService = debateService;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(debateService.list(authorization));
    }

    @PostMapping("/like")
    public Result<Map<String, Object>> like(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        Long id = body == null || body.get("id") == null ? null
                : Long.valueOf(String.valueOf(body.get("id")).trim().replaceAll("\\.0+$", ""));
        return Result.ok(debateService.like(authorization, id));
    }

    @PostMapping("/publish")
    public Result<Map<String, Object>> publish(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String topic = body == null ? null : String.valueOf(body.get("topic"));
        String quote = body == null ? null : String.valueOf(body.get("quote"));
        return Result.ok(debateService.publish(authorization, topic, quote));
    }

    @PostMapping("/reply")
    public Result<Map<String, Object>> reply(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String topic = body == null ? null : String.valueOf(body.get("topic"));
        String quote = body == null ? null : String.valueOf(body.get("quote"));
        Integer side = body == null ? null
                : (body.get("side") == null ? null
                    : Integer.valueOf(String.valueOf(body.get("side")).trim().replaceAll("\\.0+$", "")));
        return Result.ok(debateService.reply(authorization, topic, side, quote));
    }
}
