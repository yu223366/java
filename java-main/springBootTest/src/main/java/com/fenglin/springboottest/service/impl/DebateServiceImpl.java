package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.Discourse;
import com.fenglin.springboottest.entity.DiscourseLike;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.DiscourseLikeMapper;
import com.fenglin.springboottest.mapper.DiscourseMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.DebateService;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 辩场实现。
 * 数据落在 wushan_discourse：topic 为辩题，side 0 正 / 1 反；
 * 普通论道语录的 topic 为 null，不受辩场影响。
 * 胜负规则：正反两方各自陈词的点赞数相加，票多者胜，相同为平。
 */
@Service
public class DebateServiceImpl implements DebateService {

    private final DiscourseMapper discourseMapper;
    private final DiscourseLikeMapper discourseLikeMapper;
    private final UserMapper userMapper;

    public DebateServiceImpl(DiscourseMapper discourseMapper,
                             DiscourseLikeMapper discourseLikeMapper,
                             UserMapper userMapper) {
        this.discourseMapper = discourseMapper;
        this.discourseLikeMapper = discourseLikeMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<Map<String, Object>> list(String authorization) {
        Long myId = optionalUserId(authorization);
        Set<Long> liked = new HashSet<>();
        if (myId != null) {
            liked = discourseLikeMapper.selectList(Wrappers.<DiscourseLike>lambdaQuery()
                            .eq(DiscourseLike::getUserId, myId)).stream()
                    .map(DiscourseLike::getDiscourseId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }
        List<Discourse> rows = discourseMapper.selectList(Wrappers.<Discourse>lambdaQuery()
                .isNotNull(Discourse::getTopic)
                .orderByDesc(Discourse::getId).last("limit 200"));
        Map<String, List<Discourse>> grouped = new LinkedHashMap<>();
        for (Discourse d : rows) {
            if (d.getTopic() == null || d.getTopic().isBlank()) continue;
            grouped.computeIfAbsent(d.getTopic().trim(), k -> new ArrayList<>()).add(d);
        }
        Set<Long> finalLiked = liked;
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
                m.put("zan", d.getZan() == null ? 0 : d.getZan());
                m.put("liked", finalLiked.contains(d.getId()));
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
            dm.put("winner", winnerOf(leftZan, rightZan));
            dm.put("winnerLabel", winnerLabel(leftZan, rightZan));
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
        if (cnt == null || cnt == 0) throw new BizException("查无此辩题");
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

    @Override
    public Map<String, Object> like(String token, Long discourseId) {
        Object[] p = requireUser(token);
        Long userId = (Long) p[0];
        if (discourseId == null) throw new BizException("未指定陈词");
        Discourse d = discourseMapper.selectById(discourseId);
        if (d == null || d.getTopic() == null || d.getTopic().isBlank()) {
            throw new BizException("查无此辩场陈词");
        }
        Long dup = discourseLikeMapper.selectCount(Wrappers.<DiscourseLike>lambdaQuery()
                .eq(DiscourseLike::getDiscourseId, discourseId)
                .eq(DiscourseLike::getUserId, userId));
        if (dup != null && dup > 0) throw new BizException("此言你已赞过，一赞为一票");
        DiscourseLike lk = new DiscourseLike();
        lk.setDiscourseId(discourseId);
        lk.setUserId(userId);
        lk.setCreateTime(LocalDateTime.now());
        discourseLikeMapper.insert(lk);
        discourseMapper.update(null, Wrappers.<Discourse>lambdaUpdate()
                .eq(Discourse::getId, discourseId).setSql("zan = zan + 1"));

        // 重新统计该辩题正反票数，判定谁胜
        List<Discourse> all = discourseMapper.selectList(Wrappers.<Discourse>lambdaQuery()
                .eq(Discourse::getTopic, d.getTopic()));
        int leftZan = 0;
        int rightZan = 0;
        // all 是在 zan+1 之后重新查出来的，不能再补加一票，否则刚赞的一方会被算两票
        for (Discourse x : all) {
            int z = x.getZan() == null ? 0 : x.getZan();
            if (x.getSide() != null && x.getSide() == 1) rightZan += z;
            else leftZan += z;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("id", discourseId);
        out.put("zan", (d.getZan() == null ? 0 : d.getZan()) + 1);
        out.put("topic", d.getTopic());
        out.put("side", d.getSide());
        out.put("leftZan", leftZan);
        out.put("rightZan", rightZan);
        out.put("winner", winnerOf(leftZan, rightZan));
        out.put("winnerLabel", winnerLabel(leftZan, rightZan));
        return out;
    }

    /* ------------------------------ 工具 ------------------------------ */
    /** 票多者胜；相同为平 */
    private static String winnerOf(int left, int right) {
        return left > right ? "left" : right > left ? "right" : "draw";
    }

    private static String winnerLabel(int left, int right) {
        if (left > right) return "正 方 胜";
        if (right > left) return "反 方 胜";
        return left == 0 ? "尚 未 开 票" : "势 均 力 敌";
    }

    private String elementOf(Long userId) {
        User u = userMapper.selectById(userId);
        return u != null && u.getElement() != null ? u.getElement() : "huo";
    }

    /** 未登录也能看辩场，只是不能点赞 */
    private Long optionalUserId(String authorization) {
        String t = authorization;
        if (t != null && t.startsWith("Bearer ")) t = t.substring(7);
        Object[] parsed = TokenUtil.parse(t);
        return parsed == null ? null : (Long) parsed[0];
    }

    private Object[] requireUser(String authorization) {
        String t = authorization;
        if (t != null && t.startsWith("Bearer ")) t = t.substring(7);
        Object[] parsed = TokenUtil.parse(t);
        if (parsed == null) throw new BizException("未登录不可登台论辩，请先入境");
        User user = userMapper.selectById((Long) parsed[0]);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BizException("查无此山籍，请重新入境");
        }
        return parsed;
    }
}
