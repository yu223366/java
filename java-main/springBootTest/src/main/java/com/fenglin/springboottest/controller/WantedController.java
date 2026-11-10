package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.WantedService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 求购挂单控制器，对接前端 src/api/wanted.js。
 * 前缀 /api/wanted。
 *
 * 接口清单：
 *   GET  /api/wanted/list    求购榜
 *   POST /api/wanted/publish  挂出求购 { title, maxPrice, note }
 *   POST /api/wanted/close    撤销求购 { id }
 *   GET  /api/wanted/match?title=  查与某物相关的求购
 */
@RestController
@RequestMapping("/api/wanted")
public class WantedController {

    private final WantedService wantedService;

    public WantedController(WantedService wantedService) {
        this.wantedService = wantedService;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(wantedService.list());
    }

    @PostMapping("/publish")
    public Result<Map<String, Object>> publish(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody Map<String, Object> body) {
        String title = body == null ? null : String.valueOf(body.get("title"));
        String note = body == null ? null : String.valueOf(body.get("note"));
        Integer maxPrice = body == null || body.get("maxPrice") == null ? null
                : Integer.valueOf(String.valueOf(body.get("maxPrice")));
        return Result.ok(wantedService.publish(authorization, title, maxPrice, note));
    }

    @PostMapping("/close")
    public Result<Map<String, Object>> close(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("id") Long id) {
        return Result.ok(wantedService.close(authorization, id));
    }

    @GetMapping("/match")
    public Result<List<Map<String, Object>>> match(@RequestParam("title") String title) {
        return Result.ok(wantedService.match(title));
    }
}
