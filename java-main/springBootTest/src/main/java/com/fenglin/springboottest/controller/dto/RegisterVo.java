package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/** 注册结果 */
@Data
public class RegisterVo {
    private String userId;
    private String token;
    private String username;
    private String element;

    /** 权限：注册即普通山友，恒为 1 */
    private Integer role;
}
