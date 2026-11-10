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

    /** 辩论主题（为空即普通论道语录） */
    private String topic;

    /** 辩论立场：0=正方 1=反方（普通语录为 0） */
    private Integer side;

    /** 排序 */
    private Integer sort;

    /** 状态：1 上榜 / 0 下榜 */
    private Integer status;
}
