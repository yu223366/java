package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 热门搜索词实体，对应表 wushan_hot_keyword。
 */
@Data
@TableName("wushan_hot_keyword")
public class HotKeyword implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 热词 */
    private String keyword;

    /** 五行 key */
    private String element;

    /** 搜索次数 */
    private Integer searchCount;

    /** 排序 */
    private Integer sort;

    /** 状态：1 展示 / 0 隐藏 */
    private Integer status;
}
