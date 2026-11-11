package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 山籍（用户）实体，对应表 wushan_user。
 */
@Data
@TableName("wushan_user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 名号 */
    private String username;

    /** 暗语（明文存储，按需求不加密） */
    private String password;

    /** 本命元素 key（正五行 + 衍生）：jin / mu / shui / huo / tu / feng / lei / dian / taiji / yinyang */
    private String element;

    /** 元素 label：金 / 木 / 水 / 火 / 土 / 风 / 雷 / 电 / 太极 / 阴阳 */
    private String elementLabel;

    /** 是否允诺山规 */
    private Boolean agreed;

    /** 允诺时间 */
    private LocalDateTime agreedAt;

    /** 状态：1 正常 / 0 禁用 */
    private Integer status;

    /** 权限：0=管理员(执事) 1=普通山友（注册默认 1） */
    private Integer role;

    /** 灵石余额（签到 / 答题 / 拍卖 / 市集 的收支账户） */
    private Integer spirit;

    /**
     * 山籍注销标记（软注销）：0=正常 1=已注销。
     * 走软注销而非硬删，避免级联破坏寄售 / 宗门 / 论道 等外键依赖，且可恢复。
     */
    private Integer deleted;

    /** 创建时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
