package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 求购挂单，对应表 wushan_wanted。
 * 山友挂出想收之物并标出最高愿付灵石，供持有者上门。
 */
@Data
@TableName("wushan_wanted")
public class Wanted implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 求购人 id */
    private Long userId;

    /** 求购人名号 */
    private String username;

    /** 想求购之物 */
    private String title;

    /** 愿付最高灵石 */
    private Integer maxPrice;

    /** 备注 */
    private String note;

    /** 0=求购中 1=已成交 2=已撤销 */
    private Integer status;

    private LocalDateTime createTime;
}
