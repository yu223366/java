package com.fenglin.springboottest.service;

import java.util.List;
import java.util.Map;

/**
 * 拍卖行服务：
 * 每 10 分钟一场、提前 1 分钟通知在线用户、每场 5 件按品级由低到高、
 * 出价后 5 秒倒计时落槌、仅注册且在线用户可参与。
 */
public interface AuctionService {

    /** 当前场次全量状态（同时刷新调用者的在线状态） */
    Map<String, Object> status(String authorization);

    /** 出价，返回最新价 / 出价人 / 倒计时 */
    Map<String, Object> bid(String authorization, Integer increment);

    /** 拍品属性详情 */
    Map<String, Object> itemDetail(Long itemId);

    /** 出价流水 */
    List<Map<String, Object>> logs(Integer roundNo);

    /** 往场成交记录（回放） */
    List<Map<String, Object>> history(String authorization, Integer limit);

    /** 关注拍品：此物再上拍时提醒捡漏 */
    Map<String, Object> watch(String authorization, String itemTitle);

    /** 我关注的拍品 */
    List<Map<String, Object>> watches(String authorization);

    /** 我的竞拍记录 */
    List<Map<String, Object>> myBids(String authorization);
}
