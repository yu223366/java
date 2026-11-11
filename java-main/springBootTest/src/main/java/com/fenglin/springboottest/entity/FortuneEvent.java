package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 秘境奇遇事件实体，对应表 wushan_fortune_event。
 */
@Data
@TableName("wushan_fortune_event")
public class FortuneEvent implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 奇遇名 */
    private String name;

    /** 奇遇描述 */
    private String eventDesc;

    /** 额外灵石 */
    private Integer spirit;

    /** 额外掉落物品名 */
    private String itemTitle;

    /** 掉落数量 */
    private Integer itemCount;

    /** 抽取权重 */
    private Integer weight;

    /** 品阶 */
    private String gradeKey;

    /** 1=启用 */
    private Integer status;

}
