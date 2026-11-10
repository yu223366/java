package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 山友背包，对应表 wushan_user_bag —— 秘境掉落与炼丹消耗都记在这里。
 */
@Data
@TableName("wushan_user_bag")
public class UserBag implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 山友 id */
    private Long userId;

    /** 物品 id（对应 wushan_item） */
    private Long itemId;

    /** 持有数量 */
    private Integer count;


    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
