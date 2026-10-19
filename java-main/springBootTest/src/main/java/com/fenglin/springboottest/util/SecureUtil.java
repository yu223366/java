package com.fenglin.springboottest.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * 密码安全工具：BCrypt 单向哈希。明文永不落库。
 */
public final class SecureUtil {

    private SecureUtil() {
    }

    /** 生成 BCrypt 密文 */
    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(10));
    }

    /** 校验明文与密文是否匹配 */
    public static boolean matches(String plain, String hashed) {
        if (plain == null || hashed == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(plain, hashed);
        } catch (Exception e) {
            return false;
        }
    }
}
