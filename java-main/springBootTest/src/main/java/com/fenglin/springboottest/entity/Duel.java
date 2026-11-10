package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 五行问对决，对应表 wushan_duel。
 */
@Data
@TableName("wushan_duel")
public class Duel implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 挑战者 id */
    private Long challengerId;

    /** 挑战者名号 */
    private String challengerName;

    /** 对手 id */
    private Long opponentId;

    /** 对手名号 */
    private String opponentName;

    /** 0=进行中 1=已结束 */
    private Integer status;

    /** 已应答题数（答错也要推进，否则会卡在同一题） */
    private Integer answered;

    /** 题目快照 JSON */
    private String questionJson;

    /** 挑战者得分 */
    private Integer myScore;

    /** 对手得分 */
    private Integer opScore;

    /** win / lose / draw */
    private String result;

    /** 胜负灵石 */
    private Integer rewardSpirit;


    /** 创建时间 */
    private LocalDateTime createTime;
}
