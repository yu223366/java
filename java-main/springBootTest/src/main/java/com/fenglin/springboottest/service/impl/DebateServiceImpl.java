package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.Discourse;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.DiscourseMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.DebateService;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 辩场实现。
 * 数据落在 wushan_discourse：topic 为辩题，side 0 正 / 1 反；
 * 普通论道语录的 topic 为 null，不受辩场影响。
 */
@Service
public class DebateServiceImpl implements DebateService {

    private final DiscourseMapper discourseMapper;
    private final UserMapper userMapper;

    public DebateServiceImpl(DiscourseMapper discourseMapper, UserMapper userMapper) {
        this.discourseMapper = discourseMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<Map<String, Object>> list() {
        List<Discourse> rows = discourseMapper.selectList(Wrappers.<Discourse>lambdaQuery()
                .isNotNull(Discourse::getTopic)
                .orderByDesc(Discourse::getId).last("limit 200"));
        Map<String, List<Discourse>> grouped = new LinkedHashMap<>();
        for (Discourse d : rows) {
            if (d.getTopic() == null || d.getTopic().isBlank()) continue;
            grouped.computeIfAbsent(d.getTopic().trim(), k -> new ArrayList<>()).add(d);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, List<Discourse>> e : grouped.entrySet()) {
            List<Map<String, Object>> left = new ArrayList<>();
            List<Map<String, Object>> right = new ArrayList<>();
            int leftZan = 0;
            int rightZan = 0;
            for (Discourse d : e.getValue()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", d.getId());
                m.put("name", d.getName());
                m.put("element", d.getElement());
                m.put("quote", d.getQuote());
                m.put("zan", d.getZan());
                if (d.getSide() != null && d.getSide() == 1) {
                    right.add(m);
                    rightZan += d.getZan() == null ? 0 : d.getZan();
                } else {
                    left.add(m);
                    leftZan += d.getZan() == null ? 0 : d.getZan();
                }
            }
            Map<String, Object> dm = new LinkedHashMap<>();
            dm.put("topic", e.getKey());
            dm.put("left", left);
            dm.put("right", right);
            dm.put("leftZan", leftZan);
            dm.put("rightZan", rightZan);
            dm.put("count", e.getValue().size());
            out.add(dm);
        }
        return out;
    }

    @Override
    public Map<String, Object> publish(String token, String topic, String quote) {
        Object[] p = requireUser(token);
        String t = topic == null ? "" : topic.trim();
        if (t.isEmpty()) throw new BizException("辩题不可为空");
        if (t.length() > 30) throw new BizException("辩题至多 30 字");
        String q = quote == null ? "" : quote.trim();
        if (q.isEmpty()) throw new BizException("立论不可为空");
        if (q.length() > 200) throw new BizException("立论至多 200 字");
        Long dup = discourseMapper.selectCount(Wrappers.<Discourse>lambdaQuery()
                .eq(Discourse::getTopic, t));
        if (dup != null && dup > 0) {
            throw new BizException("「" + t + "」已在辩场中，换个辩题罢");
        }
        Discourse d = new Discourse();
        d.setName((String) p[1]);
        d.setElement(elementOf((Long) p[0]));
        d.setQuote(q);
        d.setZan(0);
        d.setSort(999);
        d.setStatus(1);
        d.setTopic(t);
        d.setSide(0);
        discourseMapper.insert(d);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", d.getId());
        out.put("topic", t);
        out.put("side", 0);
        return out;
    }

    @Override
    public Map<String, Object> reply(String token, String topic, Integer side, String quote) {
        Object[] p = requireUser(token);
        String t = topic == null ? "" : topic.trim();
        Long cnt = discourseMapper.selectCount(Wrappers.<Discourse>lambdaQuery()
                .eq(Discourse::getTopic, t));
        if (cnt == null || cnt == 0) {
            throw new BizException("查无此辩题");
        }
        String q = quote == null ? "" : quote.trim();
        if (q.isEmpty()) throw new BizException("陈词不可为空");
        if (q.length() > 200) throw new BizException("陈词至多 200 字");
        Discourse d = new Discourse();
        d.setName((String) p[1]);
        d.setElement(elementOf((Long) p[0]));
        d.setQuote(q);
        d.setZan(0);
        d.setSort(999);
        d.setStatus(1);
        d.setTopic(t);
        d.setSide(side != null && side == 1 ? 1 : 0);
        discourseMapper.insert(d);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("id", d.getId());
        out.put("topic", t);
        out.put("side", d.getSide());
        return out;
    }

    /* ------------------------------ 工具 ------------------------------ */
    private String elementOf(Long userId) {
        User u = userMapper.selectById(userId);
        return u != null && u.getElement() != null ? u.getElement() : "huo";
    }

    private Object[] requireUser(String authorization) {
        String t = authorization;
        if (t != null && t.startsWith("Bearer ")) {
            t = t.substring(7);
        }
        Object[] parsed = TokenUtil.parse(t);
        if (parsed == null) {
            throw new BizException("未登录不可登台论辩，请先入境");
        }
        User user = userMapper.selectById((Long) parsed[0]);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BizException("查无此山籍，请重新入境");
        }
        return parsed;
    }
}
