package com.fenglin.springboottest.service;

import java.util.List;
import java.util.Map;

/**
 * 宗门宝库 + 山友寄售。
 *
 * <p>宝库归宗门管：本宗成员可用贡献点兑换库中藏品，也可把背包里的物品存入宗库换贡献；
 * 别人的物品可挂到宝库的寄售区，用灵石购买，或以物换物。</p>
 */
public interface TreasureService {

    /** 本宗宝库：宗门信息 + 我的贡献点 + 库存清单 */
    Map<String, Object> vault(String authorization);

    /** 把背包中的物品存入宗门宝库，换取贡献点（宗主 / 长老可自定义兑换价） */
    Map<String, Object> deposit(String authorization, Long itemId, Integer count, Integer contributionPrice);

    /** 用贡献点兑换宗库藏品，物品入背包 */
    Map<String, Object> redeem(String authorization, Long stockId, Integer count);

    /** 寄售区列表（只看在架） */
    List<Map<String, Object>> consigns(String authorization);

    /** 我的寄售挂单 */
    List<Map<String, Object>> myConsigns(String authorization);

    /** 挂出寄售：itemId 从背包扣，mode 0 只卖灵石 / 1 只换物 / 2 皆可 */
    Map<String, Object> publish(String authorization, Long itemId, Integer count,
                                Integer priceSpirit, Long barterItemId, String barterTitle, Integer mode);

    /** 用灵石买下寄售物：灵石转给卖家，物品入买家背包 */
    Map<String, Object> buy(String authorization, Long consignId);

    /** 以物换物：买家拿 offerItemId 换挂单物品，双方背包同时变动 */
    Map<String, Object> barter(String authorization, Long consignId, Long offerItemId);

    /** 撤销自己的挂单，物品退回背包 */
    Map<String, Object> cancel(String authorization, Long consignId);
}
