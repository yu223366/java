package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/** 登录结果 */
@Data
public class LoginVo {
    private String token;
    private String username;

    /** 权限：0=管理员(执事) 1=普通山友，前端据此决定是否展示后台入口 */
    private Integer role;
}
