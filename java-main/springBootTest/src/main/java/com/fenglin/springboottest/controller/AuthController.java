package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.common.Result;
import com.fenglin.springboottest.controller.dto.*;
import com.fenglin.springboottest.service.CaptchaService;
import com.fenglin.springboottest.service.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器，对接前端 src/api/auth.js 的接口契约。
 * 所有接口前缀 /api/auth（前端 BASE_URL=/api，由 Vite 代理到 8080）。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final CaptchaService captchaService;
    private final UserService userService;

    public AuthController(CaptchaService captchaService, UserService userService) {
        this.captchaService = captchaService;
        this.userService = userService;
    }

    /** 生成图形符印 */
    @PostMapping("/captcha")
    public Result<CaptchaVo> captcha() {
        return Result.ok(captchaService.generate());
    }

    /** 校验图形符印 */
    @PostMapping("/captcha/verify")
    public Result<Map<String, Object>> verifyCaptcha(@RequestBody CaptchaVerifyReq req) {
        captchaService.verify(req.getCaptchaId(), req.getCaptchaCode());
        Map<String, Object> data = new HashMap<>(2);
        data.put("ok", true);
        return Result.ok(data);
    }

    /** 名号可用性查询 */
    @GetMapping("/username/available")
    public Result<Map<String, Object>> checkUsername(@RequestParam("username") String username) {
        Map<String, Object> data = new HashMap<>(2);
        data.put("available", userService.isUsernameAvailable(username));
        return Result.ok(data);
    }

    /** 注册（立契） */
    @PostMapping("/register")
    public Result<RegisterVo> register(@RequestBody RegisterRequest req) {
        return Result.ok(userService.register(req));
    }

    /** 登录（入境） */
    @PostMapping("/login")
    public Result<LoginVo> login(@RequestBody LoginRequest req) {
        return Result.ok(userService.login(req.getUsername(), req.getPassword()));
    }

    /**
     * 退出登录（出山）。
     * 当前令牌为无状态本地令牌，服务端无需注销会话；
     * 前端清掉本地 token 即可。预留此接口保持契约完整。
     */
    @PostMapping("/logout")
    public Result<Map<String, Object>> logout() {
        Map<String, Object> data = new HashMap<>(2);
        data.put("ok", true);
        return Result.ok(data);
    }
}
