package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/** 图形符印校验请求 */
@Data
public class CaptchaVerifyReq {
    private String captchaId;
    private String captchaCode;
}
