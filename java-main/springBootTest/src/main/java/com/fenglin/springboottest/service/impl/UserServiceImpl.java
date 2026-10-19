package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.controller.dto.LoginRequest;
import com.fenglin.springboottest.controller.dto.LoginVo;
import com.fenglin.springboottest.controller.dto.RegisterRequest;
import com.fenglin.springboottest.controller.dto.RegisterVo;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.CaptchaService;
import com.fenglin.springboottest.service.UserService;
import com.fenglin.springboottest.util.SecureUtil;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 山籍（用户）业务实现：注册（立契）/ 登录（入境）/ 查重。
 */
@Service
public class UserServiceImpl implements UserService {

    // 名号：2~16 位，汉字 / 字母 / 数字 / 下划线（与前端校验口径一致）
    private static final Pattern USERNAME_RE = Pattern.compile("[\\u4e00-\\u9fa5A-Za-z0-9_]+");
    // 暗语规则：8~20 位，不含空格与汉字，且兼含字母与数字
    private static final Pattern HAS_SPACE_CN = Pattern.compile("[\\s\\u4e00-\\u9fa5]");

    private final UserMapper userMapper;
    private final CaptchaService captchaService;

    public UserServiceImpl(UserMapper userMapper, CaptchaService captchaService) {
        this.userMapper = userMapper;
        this.captchaService = captchaService;
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        Long count = userMapper.selectCount(
                Wrappers.<User>lambdaQuery().eq(User::getUsername, username.trim()));
        return count == null || count == 0;
    }

    @Override
    public RegisterVo register(RegisterRequest req) {
        // 1) 基础入参校验
        String username = trim(req.getUsername());
        String password = req.getPassword();

        if (username == null || username.length() < 2 || username.length() > 16) {
            throw new BizException("名号需为 2~16 位");
        }
        if (!USERNAME_RE.matcher(username).matches()) {
            throw new BizException("名号仅可用汉字、字母、数字与下划线");
        }
        if (password == null || password.length() < 8 || password.length() > 20) {
            throw new BizException("暗语需为 8~20 位");
        }
        if (HAS_SPACE_CN.matcher(password).find()) {
            throw new BizException("暗语不可含空格或汉字");
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw new BizException("暗语需兼含字母与数字");
        }
        if (req.getAgreed() == null || !req.getAgreed()) {
            throw new BizException("需先允诺山规，方可立契");
        }

        // 2) 图形符印校验
        captchaService.verify(req.getCaptchaId(), req.getCaptchaCode());

        // 3) 名号查重
        if (!isUsernameAvailable(username)) {
            throw new BizException("此名号已有人立契，请另择一名");
        }

        // 4) 入库
        User user = new User();
        user.setUsername(username);
        user.setPassword(SecureUtil.hash(password));
        user.setElement(trim(req.getElement()));
        user.setElementLabel(trim(req.getElementLabel()));
        user.setAgreed(true);
        user.setAgreedAt(LocalDateTime.now());
        user.setStatus(1);
        userMapper.insert(user);

        // 5) 消费一次性图形符印
        captchaService.consume(req.getCaptchaId());

        // 6) 返回（含登录态令牌）
        RegisterVo vo = new RegisterVo();
        vo.setUserId(String.valueOf(user.getId()));
        vo.setToken(TokenUtil.generate(user.getId(), username));
        vo.setUsername(username);
        vo.setElement(user.getElement());
        return vo;
    }

    @Override
    public LoginVo login(String principal, String password) {
        if (principal == null || principal.isBlank() || password == null || password.isBlank()) {
            throw new BizException("名号 / 暗语不可空");
        }
        String p = principal.trim();
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUsername, p));
        if (user == null) {
            throw new BizException("查无此山籍");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BizException("山籍已被封禁");
        }
        if (!SecureUtil.matches(password, user.getPassword())) {
            throw new BizException("暗语有误");
        }
        LoginVo vo = new LoginVo();
        vo.setToken(TokenUtil.generate(user.getId(), user.getUsername()));
        vo.setUsername(user.getUsername());
        return vo;
    }

    private static String trim(String s) {
        return s == null ? null : s.trim();
    }
}
