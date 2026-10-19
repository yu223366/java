package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/** 图形符印（验证码）生成结果 */
@Data
public class CaptchaVo {
    /** 验证码标识，提交时回传用于二次校验 */
    private String captchaId;
    /** base64 PNG 图片，形如 data:image/png;base64,xxxx */
    private String image;

    /**
     * 仅本地联调用：当 wushan.captcha.expose-code=true 时返回明文符印。
     * 生产务必置 false。前端会忽略此字段。
     */
    private String devCode;

    public CaptchaVo() {
    }

    public CaptchaVo(String captchaId, String image) {
        this.captchaId = captchaId;
        this.image = image;
    }
}
