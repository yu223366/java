package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/** 登录结果 */
@Data
public class LoginVo {
    private String token;
    private String username;
}
