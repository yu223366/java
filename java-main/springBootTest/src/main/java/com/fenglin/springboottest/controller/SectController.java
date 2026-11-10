package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.service.SectService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 宗门控制器，对接前端 src/api/sect.js。
 * 前缀 /api/sect。
 *
 * 接口清单：
 *   GET  /api/sect/list     宗门列表
 *   GET  /api/sect/mine      我所在的宗门
 *   POST /api/sect/create    立宗 ?name=&element=
 *   POST /api/sect/join      投山门 ?sectId=
 *   POST /api/sect/quit      离山
 *   POST /api/sect/donate    捐献 ?amount=
 */
@RestController
@RequestMapping("/api/sect")
public class SectController {

    private final SectService sectService;

    public SectController(SectService sectService) {
        this.sectService = sectService;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(sectService.list());
    }

    @GetMapping("/mine")
    public Result<Map<String, Object>> mine(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(sectService.mine(authorization));
    }

    @PostMapping("/create")
    public Result<Map<String, Object>> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("name") String name,
            @RequestParam(value = "element", required = false) String element) {
        return Result.ok(sectService.create(authorization, name, element));
    }

    @PostMapping("/join")
    public Result<Map<String, Object>> join(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("sectId") Long sectId) {
        return Result.ok(sectService.join(authorization, sectId));
    }

    @PostMapping("/quit")
    public Result<Map<String, Object>> quit(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(sectService.quit(authorization));
    }

    @PostMapping("/donate")
    public Result<Map<String, Object>> donate(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "amount", required = false) Integer amount) {
        return Result.ok(sectService.donate(authorization, amount));
    }

    /** 宗主传位 ?targetUserId= */
    @PostMapping("/transfer")
    public Result<Map<String, Object>> transfer(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("targetUserId") Long targetUserId) {
        return Result.ok(sectService.transfer(authorization, targetUserId));
    }

    /** 解散宗门 */
    @PostMapping("/disband")
    public Result<Map<String, Object>> disband(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(sectService.disband(authorization));
    }
}
