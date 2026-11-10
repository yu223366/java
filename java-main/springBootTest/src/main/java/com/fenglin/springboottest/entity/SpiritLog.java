package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 灵石流水，对应表 wushan_spirit_log。
 * 正数为收入（签到 / 答题 / 退款），负数为支出（拍卖 / 市集）。
 */
@Data
@TableName("wushan_spirit_log")
public class SpiritLog implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 变动数额 */
    private Integer amount;

    /** 变动后余额 */
    private Integer balance;

    /** 事由 */
    private String reason;

    private LocalDateTime createTime;
}
