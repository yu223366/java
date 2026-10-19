package com.fenglin.springboottest.controller.dto;

import lombok.Data;

/** 市集购买请求体：{ id, count } */
@Data
public class BuyRequest {

    /** 物品 id */
    private Long id;

    /** 购买数量（默认 1） */
    private Integer count;
}
