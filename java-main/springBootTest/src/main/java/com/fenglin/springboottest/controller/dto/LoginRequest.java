package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/** 登录请求 */
@Data
public class LoginRequest {
    /** 名号或传音符 */
    private String username;
    /** 暗语 */
    private String password;
}
