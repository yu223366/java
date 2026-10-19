package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 搜索日志实体，对应表 wushan_search_log（热词统计 / 检索分析用）。
 */
@Data
@TableName("wushan_search_log")
public class SearchLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 搜索者 id（未登录为空） */
    private Long userId;

    /** 搜索者名号 */
    private String username;

    /** 搜索词 */
    private String keyword;

    /** 筛选五行 */
    private String element;

    /** 筛选品阶 */
    private String gradeKey;

    /** 筛选分类 */
    private Long categoryId;

    /** 命中条数 */
    private Integer resultCnt;

    /** 搜索时间 */
    private LocalDateTime createTime;
}
