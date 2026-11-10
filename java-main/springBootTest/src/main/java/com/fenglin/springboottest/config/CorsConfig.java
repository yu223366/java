package com.fenglin.springboottest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 跨域配置
 * 开发期前端走 vite 代理（同源，无需 CORS）；
 * 若将来前后端分离部署，前端直接调 8080 时需要此配置。
 * 目前放行本地常用前端端口，生产环境请改为具体域名。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                // 本地开发放行常见内网段，不写死某一台机器的 IP（换网络无需再改）；
                // 生产环境请替换为实际域名。
                .allowedOriginPatterns(
                        "http://localhost:*",
                        "http://127.0.0.1:*",
                        "http://10.*:*",
                        "http://172.*:*",
                        "http://192.168.*:*"
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
