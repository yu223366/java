package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.ObserverService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 观山台控制器：全站数据总览（只读）。
 * 前缀 /api/observer。
 */
@RestController
@RequestMapping("/api/observer")
public class ObserverController {

    private final ObserverService observerService;

    public ObserverController(ObserverService observerService) {
        this.observerService = observerService;
    }

    /** 总览数据 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.ok(observerService.overview());
    }

    /** 导航栏徽标：各入口的实时状态（在线 / 未读 / 已测灵根 / 出价次数） */
    @GetMapping("/badges")
    public Result<Map<String, Object>> badges(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(observerService.badges(authorization));
    }
}
