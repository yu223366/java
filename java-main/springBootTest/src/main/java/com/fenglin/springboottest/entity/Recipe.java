package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 炼丹炼器配方，对应表 wushan_recipe。
 */
@Data
@TableName("wushan_recipe")
public class Recipe implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 丹名 / 器名 */
    private String name;

    /** danyao 丹药 / faqi 炼器 */
    private String category;

    /** 产物物品 id */
    private Long outputItemId;

    /** 产物数量 */
    private Integer outputCount;

    /** 炼制耗灵石 */
    private Integer costSpirit;

    /** 材料清单 JSON：[{"itemId":6,"count":1}] */
    private String materialsJson;

    /** 配方说明 */
    private String description;

    /** 状态：1 可炼 / 0 停用 */
    private Integer status;

    /** 成功率（百分比）：越高越容易成，失败只耗材料与灵石，不出产物 */
    private Integer successRate;

    /** 炼制类别：danyao 炼丹 / lianqi 炼器 / zhenfa 阵法 */
    private String craftType;


    /** 创建时间 */
    private LocalDateTime createTime;
}
