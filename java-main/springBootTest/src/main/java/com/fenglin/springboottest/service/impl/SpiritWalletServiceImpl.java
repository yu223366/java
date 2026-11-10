package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.SpiritLog;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.SpiritLogMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.SpiritWalletService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 灵石钱包实现。
 * 余额更新一律用带条件的 SQL 原子加减，避免并发下多扣 / 少扣。
 */
@Service
public class SpiritWalletServiceImpl implements SpiritWalletService {

    private final UserMapper userMapper;
    private final SpiritLogMapper spiritLogMapper;

    public SpiritWalletServiceImpl(UserMapper userMapper, SpiritLogMapper spiritLogMapper) {
        this.userMapper = userMapper;
        this.spiritLogMapper = spiritLogMapper;
    }

    @Override
    public int balance(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BizException("查无此山籍");
        }
        return u.getSpirit() == null ? 0 : u.getSpirit();
    }

    @Override
    public void add(Long userId, int amount, String reason) {
        if (amount <= 0) return;
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .setSql("spirit = IFNULL(spirit, 0) + " + amount));
        writeLog(userId, amount, reason);
    }

    @Override
    public void deduct(Long userId, int amount, String reason) {
        if (amount <= 0) return;
        // 带余额条件：余额不足时影响 0 行，直接判定失败
        int rows = userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, userId)
                .ge(User::getSpirit, amount)
                .setSql("spirit = spirit - " + amount));
        if (rows == 0) {
            int now = balance(userId);
            throw new BizException("灵石不足，尚缺 " + Math.max(0, amount - now) + " 灵石");
        }
        writeLog(userId, -amount, reason);
    }

    @Override
    public List<Map<String, Object>> logs(Long userId, int limit) {
        int n = limit <= 0 || limit > 50 ? 20 : limit;
        List<SpiritLog> list = spiritLogMapper.selectList(Wrappers.<SpiritLog>lambdaQuery()
                .eq(SpiritLog::getUserId, userId)
                .orderByDesc(SpiritLog::getId)
                .last("limit " + n));
        List<Map<String, Object>> out = new ArrayList<>();
        for (SpiritLog l : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("amount", l.getAmount());
            m.put("balance", l.getBalance());
            m.put("reason", l.getReason());
            m.put("time", l.getCreateTime() == null ? "" : l.getCreateTime().toString());
            out.add(m);
        }
        return out;
    }

    private void writeLog(Long userId, int amount, String reason) {
        SpiritLog l = new SpiritLog();
        l.setUserId(userId);
        l.setAmount(amount);
        l.setBalance(balance(userId));
        l.setReason(reason == null ? "" : reason);
        l.setCreateTime(LocalDateTime.now());
        spiritLogMapper.insert(l);
    }
}
