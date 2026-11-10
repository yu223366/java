package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/**
 * 出价请求：只传加价档（10 / 50 / 100 / 500），
 * 出价人身份由 Authorization 令牌解析，前端不得伪造。
 */
@Data
public class AuctionBidReq {

    /** 本次加价额 */
    private Integer increment;
}
