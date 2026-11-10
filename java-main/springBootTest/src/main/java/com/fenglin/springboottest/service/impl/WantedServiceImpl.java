package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.entity.Wanted;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.mapper.WantedMapper;
import com.fenglin.springboottest.service.WantedService;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 求购挂单实现，对应表 wushan_wanted。
 */
@Service
public class WantedServiceImpl implements WantedService {

    private final WantedMapper wantedMapper;
    private final UserMapper userMapper;

    public WantedServiceImpl(WantedMapper wantedMapper, UserMapper userMapper) {
        this.wantedMapper = wantedMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<Map<String, Object>> list() {
        List<Wanted> rows = wantedMapper.selectList(Wrappers.<Wanted>lambdaQuery()
                .eq(Wanted::getStatus, 0)
                .orderByDesc(Wanted::getMaxPrice).orderByDesc(Wanted::getId).last("limit 50"));
        return toVo(rows);
    }

    @Override
    public Map<String, Object> publish(String token, String title, Integer maxPrice, String note) {
        Object[] p = requireUser(token);
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) throw new BizException("请写明求购之物");
        if (t.length() > 60) throw new BizException("品名过长");
        int price = maxPrice == null ? 0 : Math.max(0, Math.min(maxPrice, 1000000));
        Long dup = wantedMapper.selectCount(Wrappers.<Wanted>lambdaQuery()
                .eq(Wanted::getUserId, (Long) p[0]).eq(Wanted::getTitle, t).eq(Wanted::getStatus, 0));
        if (dup != null && dup > 0) {
            throw new BizException("你已挂过此物的求购");
        }
        Wanted w = new Wanted();
        w.setUserId((Long) p[0]);
        w.setUsername((String) p[1]);
        w.setTitle(t);
        w.setMaxPrice(price);
        w.setNote(note == null ? "" : (note.length() > 120 ? note.substring(0, 120) : note));
        w.setStatus(0);
        w.setCreateTime(LocalDateTime.now());
        wantedMapper.insert(w);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", w.getId());
        out.put("title", w.getTitle());
        out.put("maxPrice", w.getMaxPrice());
        return out;
    }

    @Override
    public Map<String, Object> close(String token, Long id) {
        Object[] p = requireUser(token);
        Wanted w = wantedMapper.selectById(id);
        if (w == null) throw new BizException("查无此挂单");
        if (!w.getUserId().equals(p[0])) {
            throw new BizException("只能撤销自己的求购");
        }
        w.setStatus(2);
        wantedMapper.updateById(w);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    @Override
    public List<Map<String, Object>> match(String title) {
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) return new ArrayList<>();
        List<Wanted> rows = wantedMapper.selectList(Wrappers.<Wanted>lambdaQuery()
                .eq(Wanted::getStatus, 0).like(Wanted::getTitle, t).last("limit 10"));
        return toVo(rows);
    }

    /* ------------------------------ 工具 ------------------------------ */
    private List<Map<String, Object>> toVo(List<Wanted> rows) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Wanted w : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", w.getId());
            m.put("username", w.getUsername());
            m.put("title", w.getTitle());
            m.put("maxPrice", w.getMaxPrice());
            m.put("note", w.getNote() == null ? "" : w.getNote());
            m.put("time", w.getCreateTime() == null ? "" : w.getCreateTime().toString());
            out.add(m);
        }
        return out;
    }

    private Object[] requireUser(String authorization) {
        String t = authorization;
        if (t != null && t.startsWith("Bearer ")) {
            t = t.substring(7);
        }
        Object[] parsed = TokenUtil.parse(t);
        if (parsed == null) {
            throw new BizException("请先入境后再挂求购");
        }
        User user = userMapper.selectById((Long) parsed[0]);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BizException("查无此山籍，请重新入境");
        }
        return parsed;
    }
}
