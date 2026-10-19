package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/** 注册结果 */
@Data
public class RegisterVo {
    private String userId;
    private String token;
    private String username;
    private String element;
}
