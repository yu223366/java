package com.fenglin.springboottest.service;

import java.util.List;
import java.util.Map;

/**
 * 雾山市集：分类摊位（丹药材料铺 / 炼器材料铺 / 阵法材料铺 / 药铺 / 法器铺 / 杂货）、
 * 每件物品的详细介绍、按品质以灵石定价购买（购得之物直接入背包）。
 */
public interface MarketService {

    /** 摊位字典：key / label / desc / count */
    List<Map<String, Object>> shops();

    /** 物品分页列表 */
    Map<String, Object> items(String keyword, String element, String grade, String shop,
                              String category, Integer page, Integer size);

    /** 单件详情（含全文介绍、获取途径、品质基准价、对应背包物品） */
    Map<String, Object> detail(Long id);

    /** 购买：扣灵石 → 扣库存 → 物品入背包 → 记交易日志 */
    Map<String, Object> buy(String authorization, Long id, Integer count);
}
