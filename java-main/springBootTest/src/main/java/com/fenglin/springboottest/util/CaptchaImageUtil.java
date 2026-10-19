package com.fenglin.springboottest.util;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Random;

/**
 * 纯 JDK 实现的图形验证码绘制工具，输出 base64 PNG（data URL），
 * 无需引入任何第三方验证码依赖，headless 环境可正常生成。
 */
public final class CaptchaImageUtil {

    private CaptchaImageUtil() {
    }

    /** 生成验证码图片，返回形如 data:image/png;base64,xxxx 的字符串 */
    public static String toBase64(String code) {
        int width = 128;
        int height = 46;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        Random random = new Random();

        // 背景（米黄宣纸色）
        g.setColor(new Color(245, 240, 230));
        g.fillRect(0, 0, width, height);

        // 干扰线
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.drawLine(random.nextInt(width), random.nextInt(height),
                    random.nextInt(width), random.nextInt(height));
        }

        // 字符（墨色，轻微抖动）
        g.setFont(new Font("Serif", Font.BOLD, 30));
        for (int i = 0; i < code.length(); i++) {
            g.setColor(new Color(40 + random.nextInt(80), 40 + random.nextInt(80), 40 + random.nextInt(80)));
            int y = 34 + random.nextInt(6);
            g.drawString(String.valueOf(code.charAt(i)), 18 + i * 26, y);
        }

        g.dispose();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", baos);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("验证码图片生成失败", e);
        }
    }
}
