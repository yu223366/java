package com.fenglin.springboottest.service;

import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.controller.dto.CaptchaVo;
import com.fenglin.springboottest.util.CaptchaImageUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 图形符印（验证码）服务：内存存储，自带图片绘制与时效性校验。
 */
@Service
public class CaptchaService {

    private static final int CODE_LENGTH = 4;
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    @Value("${wushan.captcha.expire-seconds:300}")
    private long expireSeconds;

    @Value("${wushan.captcha.expose-code:false}")
    private boolean exposeCode;

    private final Map<String, Holder> store = new ConcurrentHashMap<>();
    private final Random random = new Random();

    /** 生成一张图形符印，返回标识与 base64 图片 */
    public CaptchaVo generate() {
        String captchaId = UUID.randomUUID().toString().replace("-", "");
        String code = randomCode(CODE_LENGTH);
        store.put(captchaId, new Holder(code, System.currentTimeMillis() + expireSeconds * 1000L));
        CaptchaVo vo = new CaptchaVo(captchaId, CaptchaImageUtil.toBase64(code));
        if (exposeCode) {
            vo.setDevCode(code);
        }
        return vo;
    }

    /** 校验图形符印（不在此消费，供发码与注册多次校验；注册成功后再消费） */
    public void verify(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            throw new BizException("符印缺失");
        }
        Holder holder = store.get(captchaId);
        if (holder == null) {
            throw new BizException("符印已失效，请换一张");
        }
        if (holder.expireAt < System.currentTimeMillis()) {
            store.remove(captchaId);
            throw new BizException("符印已过期，请换一张");
        }
        if (!holder.code.equalsIgnoreCase(captchaCode.trim())) {
            throw new BizException("符印不符");
        }
    }

    /** 注册成功后消费（作废）该符印 */
    public void consume(String captchaId) {
        if (captchaId != null) {
            store.remove(captchaId);
        }
    }

    private String randomCode(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /** 内部存储结构 */
    private static class Holder {
        final String code;
        final long expireAt;

        Holder(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
