package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 注册（立契）请求。字段与前端 RegisterPanel 提交的 payload 一一对应。
 */
@Data
public class RegisterRequest {
    /** 名号 */
    private String username;
    /** 暗语（明文，落库前加密） */
    private String password;
    /** 本命元素 key（正五行 + 衍生）：jin / mu / shui / huo / tu / feng / lei / dian / taiji / yinyang */
    private String element;
    /** 元素 label：金 / 木 / 水 / 火 / 土 / 风 / 雷 / 电 / 太极 / 阴阳 */
    private String elementLabel;
    /** 图形符印标识 */
    private String captchaId;
    /** 图形符印输入 */
    private String captchaCode;
    /** 是否允诺山规 */
    private Boolean agreed;
    /** 允诺时间（ISO 字符串，可选） */
    private String agreedAt;
    /** 场景（可选） */
    private String scene;
}
