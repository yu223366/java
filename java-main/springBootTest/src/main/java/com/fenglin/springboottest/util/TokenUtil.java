package com.fenglin.springboottest.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 简易令牌工具（本地联调用）。
 *
 * <p>格式：{@code base64url(userId:username:timestamp) + "." + HMAC-SHA256 签名}。</p>
 *
 * <p>为什么要签名：明文 base64 的令牌任何人改一个字符就能伪造成别人的身份，
 * 后台权限（role=0）等于形同虚设 —— 普通山友把 {@code 自己的id:自己名号} 换成
 * {@code admin 的 id:admin} 再 base64 一次就能通过 AdminController 的执事校验。
 * 加上 HMAC 之后，没有密钥就无法拼出合法令牌。</p>
 *
 * <p>密钥来源优先级：环境变量 {@code WUSHAN_TOKEN_SECRET} → 系统属性
 * {@code wushan.token.secret} → 内置默认值。生产环境务必通过环境变量覆盖。</p>
 */
public final class TokenUtil {

    /** 默认密钥（仅本地联调；上线请用 WUSHAN_TOKEN_SECRET 覆盖） */
    private static final String DEFAULT_SECRET = "wushan-wuxing-local-dev-secret";

    private static final String SECRET = resolveSecret();
    private static final String HMAC_ALG = "HmacSHA256";

    private TokenUtil() {
    }

    private static String resolveSecret() {
        String s = System.getenv("WUSHAN_TOKEN_SECRET");
        if (s == null || s.isBlank()) {
            s = System.getProperty("wushan.token.secret");
        }
        return (s == null || s.isBlank()) ? DEFAULT_SECRET : s;
    }

    /** 生成令牌：payload 用 base64url 编码，后接 HMAC 签名 */
    public static String generate(Long userId, String username) {
        String payload = userId + ":" + username + ":" + System.currentTimeMillis();
        String body = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return body + "." + sign(body);
    }

    /**
     * 解析令牌并校验签名。
     *
     * @return [0]=userId(Long), [1]=username(String)；格式非法或签名不符返回 null
     */
    public static Object[] parse(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String t = token.trim();
            int dot = t.lastIndexOf('.');
            if (dot <= 0 || dot == t.length() - 1) {
                return null; // 无签名（旧格式 / 伪造）一律拒绝
            }
            String body = t.substring(0, dot);
            String sig = t.substring(dot + 1);
            // 定长比较，避免时序侧信道
            if (!MessageDigest.isEqual(
                    sign(body).getBytes(StandardCharsets.UTF_8),
                    sig.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }
            String raw = new String(
                    Base64.getUrlDecoder().decode(body), StandardCharsets.UTF_8);
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

    /** HMAC-SHA256，输出 base64url（无填充） */
    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            byte[] digest = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("令牌签名失败", e);
        }
    }
}
