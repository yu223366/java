package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 山友论道榜实体，对应表 wushan_discourse。
 */
@Data
@TableName("wushan_discourse")
public class Discourse implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 山友名号 */
    private String name;

    /** 五行 key */
    private String element;

    /** 论道语录 */
    private String quote;

    /** 点赞数 */
    private Integer zan;

    /** 排序 */
    private Integer sort;

    /** 状态：1 上榜 / 0 下榜 */
    private Integer status;
}
