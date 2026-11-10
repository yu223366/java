package com.fenglin.springboottest.service;

import java.util.List;
import java.util.Map;

/**
 * 灵石钱包：此前项目只有「灵石」这个数字，并没有账户。
 * 签到、每日一题发灵石，拍卖、市集、炼丹扣灵石，都走这里，保证收支可查。
 */
public interface SpiritWalletService {

    /** 当前余额 */
    int balance(Long userId);

    /** 收入（amount 为正） */
    void add(Long userId, int amount, String reason);

    /** 支出（amount 为正），余额不足抛业务异常 */
    void deduct(Long userId, int amount, String reason);

    /** 最近流水 */
    List<Map<String, Object>> logs(Long userId, int limit);
}
