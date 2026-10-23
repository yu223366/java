package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 宝库灵根实体，对应表 wushan_spirit_root。
 */
@Data
@TableName("wushan_spirit_root")
public class SpiritRoot implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 灵根名 */
    private String name;

    /** 五行 key */
    private String element;

    /** 品阶 key */
    private String gradeKey;

    /** 灵根简介 */
    private String summary;

    /** 排序 */
    private Integer sort;

    /** 状态：1 收录 / 0 隐匿 */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
