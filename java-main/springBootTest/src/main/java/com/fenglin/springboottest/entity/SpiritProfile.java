package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 灵根测评记录，对应表 wushan_spirit_profile。
 * 一个山友可多次测评，以最后一次为准。
 */
@Data
@TableName("wushan_spirit_profile")
public class SpiritProfile implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 灵根名 */
    private String rootName;

    /** 五行 key */
    private String element;

    /** 品阶 key */
    private String gradeKey;

    /** 稀有度：1 普通 2 罕见 3 天品 4 异变 */
    private Integer rareLevel;

    /** 契合得分 */
    private Integer score;

    /** 灵根说明 */
    private String summary;

    private LocalDateTime createTime;
}
