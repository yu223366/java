package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 当前登录用户信息 VO，对齐前端 home.js 契约：{ username, element, nickname }。
 */
@Data
public class UserInfoVo {

    private String username;

    /** 本命五行 key */
    private String element;

    /** 昵称（当前与名号一致） */
    private String nickname;
}
