package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.AuctionBidReq;
import com.fenglin.springboottest.service.AuctionService;
import com.fenglin.springboottest.service.SseService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 拍卖行控制器，对接前端 src/api/auction.js。
 * 所有接口前缀 /api/auction（前端 BASE_URL=/api，由 Vite 代理到 8080）。
 *
 * 接口清单：
 *   GET  /api/auction/status              当前场次状态（附带开拍通知 / 在线人数 / 出价流水）
 *   POST /api/auction/bid                 出价 { increment: 10|50|100|500 }，需 Bearer token
 *   GET  /api/auction/item?id=            拍品属性
 *   GET  /api/auction/logs?roundNo=       出价流水
 */
@RestController
@RequestMapping("/api/auction")
public class AuctionController {

    private final AuctionService auctionService;
    private final SseService sseService;

    public AuctionController(AuctionService auctionService, SseService sseService) {
        this.auctionService = auctionService;
        this.sseService = sseService;
    }

    /** 前端每 2~3 秒轮询一次，服务端借此刷新在线状态 */
    @GetMapping("/status")
    public Result<Map<String, Object>> status(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(auctionService.status(authorization));
    }

    /** 出价：身份取自令牌，前端只传加价档 */
    @PostMapping("/bid")
    public Result<Map<String, Object>> bid(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AuctionBidReq req) {
        return Result.ok(auctionService.bid(authorization, req.getIncrement()));
    }

    /** 点击拍品查看属性 */
    @GetMapping("/item")
    public Result<Map<String, Object>> item(@RequestParam("id") Long id) {
        return Result.ok(auctionService.itemDetail(id));
    }

    /** 出价流水 */
    @GetMapping("/logs")
    public Result<List<Map<String, Object>>> logs(
            @RequestParam(value = "roundNo", required = false) Integer roundNo) {
        return Result.ok(auctionService.logs(roundNo));
    }

    /** 往场成交记录（回放） */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return Result.ok(auctionService.history(authorization, limit));
    }

    /** 关注拍品，捡漏提醒 */
    @PostMapping("/watch")
    public Result<Map<String, Object>> watch(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam("itemTitle") String itemTitle) {
        return Result.ok(auctionService.watch(authorization, itemTitle));
    }

    /** 我关注的拍品 */
    @GetMapping("/watches")
    public Result<List<Map<String, Object>>> watches(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(auctionService.watches(authorization));
    }

    /** 我的竞拍记录 */
    @GetMapping("/my")
    public Result<List<Map<String, Object>>> myBids(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return Result.ok(auctionService.myBids(authorization));
    }

    /**
     * SSE 事件流：开拍、落槌、出价等实时推送。
     * 前端优先用它接收通知，轮询只作为兜底。
     */
    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter stream() {
        return sseService.subscribe();
    }
}
