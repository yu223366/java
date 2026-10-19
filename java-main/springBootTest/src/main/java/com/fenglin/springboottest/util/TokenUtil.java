package com.fenglin.springboottest.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 简易令牌工具：本地联调用，格式为 base64(userId:username:timestamp)。
 * 生产环境应替换为 JWT / OAuth 等可校验、可过期的令牌方案。
 */
public final class TokenUtil {

    private TokenUtil() {
    }

    public static String generate(Long userId, String username) {
        String raw = userId + ":" + username + ":" + System.currentTimeMillis();
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 解析令牌（本地联调用，与 generate 对应）。
     *
     * @return [0]=userId, [1]=username；格式非法返回 null
     */
    public static Object[] parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String raw = new String(
                    Base64.getUrlDecoder().decode(token.trim()), StandardCharsets.UTF_8);
            String[] parts = raw.split(":", 3);
            if (parts.length < 2) {
                return null;
            }
            Long userId = Long.valueOf(parts[0]);
            String username = parts[1];
            return new Object[]{userId, username};
        } catch (Exception e) {
            return null;
        }
    }
}
