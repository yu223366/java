package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秘境探索记录，对应表 wushan_explore_log。
 */
@Data
@TableName("wushan_explore_log")
public class ExploreLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 山友 id */
    private Long userId;

    /** 秘境 id */
    private Long realmId;

    /** 秘境名（冗余便于展示） */
    private String realmName;

    /** 所历事件路径 */
    private String pathJson;

    /** 所得物品与灵石 */
    private String rewardsJson;

    /** 本次消耗灵石 */
    private Integer costSpirit;


    /** 创建时间 */
    private LocalDateTime createTime;
}
