package com.fenglin.springboottest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 在线用户（拍卖行）：只有注册且近期活跃的用户可出价、可收到开拍通知。
 * 主键为 user_id，前端每次轮询 status 时刷新 lastActiveTime。
 */
@Data
@TableName("wushan_online_user")
public class OnlineUser implements Serializable {

    @TableId(type = IdType.INPUT)
    private Long userId;

    private String username;
    private LocalDateTime lastActiveTime;
}
