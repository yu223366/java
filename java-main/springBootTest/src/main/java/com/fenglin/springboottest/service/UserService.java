package com.fenglin.springboottest.service;

import com.fenglin.springboottest.controller.dto.LoginRequest;
import com.fenglin.springboottest.controller.dto.LoginVo;
import com.fenglin.springboottest.controller.dto.RegisterRequest;
import com.fenglin.springboottest.controller.dto.RegisterVo;

/**
 * 山籍（用户）业务接口。
 */
public interface UserService {

    /** 名号是否可用（true=未被占用） */
    boolean isUsernameAvailable(String username);

    /** 立契（注册） */
    RegisterVo register(RegisterRequest req);

    /** 入境（登录） */
    LoginVo login(String principal, String password);
}
