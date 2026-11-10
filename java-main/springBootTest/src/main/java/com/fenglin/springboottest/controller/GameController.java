package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.DailyAnswerReq;
import com.fenglin.springboottest.controller.dto.SpiritAnswerReq;
import com.fenglin.springboottest.service.GameService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 山门玩法控制器，对接前端 src/api/game.js。
 * 前缀 /api/game（前端 BASE_URL=/api，由 Vite 代理到 8080）。
 *
 * 接口清单：
 *   GET  /api/game/spirit/quiz      测灵根题目
 *   POST /api/game/spirit/answer    提交测评 { answers: [0,2,...] }
 *   GET  /api/game/spirit/me        我的灵根
 *   GET  /api/game/signin/status    签到状态
 *   POST /api/game/signin/checkin   每日签到
 *   GET  /api/game/daily/question   今日一题
 *   POST /api/game/daily/answer     提交答案 { answerIdx }
 *   GET  /api/game/wallet           灵石余额与流水
 */
@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/spirit/quiz")
    public Result<Map<String, Object>> spiritQuiz() {
        return Result.ok(gameService.spiritQuiz());
    }

    @PostMapping("/spirit/answer")
    public Result<Map<String, Object>> submitSpirit(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody SpiritAnswerReq req) {
        return Result.ok(gameService.submitSpirit(authorization, req.getAnswers()));
    }

    @GetMapping("/spirit/me")
    public Result<Map<String, Object>> mySpirit(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(gameService.mySpirit(authorization));
    }

    @GetMapping("/signin/status")
    public Result<Map<String, Object>> signInStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(gameService.signInStatus(authorization));
    }

    @PostMapping("/signin/checkin")
    public Result<Map<String, Object>> checkIn(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(gameService.checkIn(authorization));
    }

    @GetMapping("/daily/question")
    public Result<Map<String, Object>> dailyQuestion(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(gameService.dailyQuestion(authorization));
    }

    @PostMapping("/daily/answer")
    public Result<Map<String, Object>> answerDaily(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody DailyAnswerReq req) {
        return Result.ok(gameService.answerDaily(authorization, req.getAnswerIdx()));
    }

    @GetMapping("/wallet")
    public Result<Map<String, Object>> wallet(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(gameService.wallet(authorization));
    }
}
