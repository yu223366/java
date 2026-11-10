package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.Sect;
import com.fenglin.springboottest.entity.SectMember;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.mapper.SectMapper;
import com.fenglin.springboottest.mapper.SectMemberMapper;
import com.fenglin.springboottest.mapper.UserMapper;
import com.fenglin.springboottest.service.EventService;
import com.fenglin.springboottest.service.SectService;
import com.fenglin.springboottest.service.SpiritWalletService;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 宗门实现。规则：一人只属一宗；宗主不可自行离山；捐献扣灵石换贡献。
 */
@Service
public class SectServiceImpl implements SectService {

    private final SectMapper sectMapper;
    private final SectMemberMapper sectMemberMapper;
    private final UserMapper userMapper;
    private final SpiritWalletService wallet;
    private final EventService eventService;

    public SectServiceImpl(SectMapper sectMapper,
                           SectMemberMapper sectMemberMapper,
                           UserMapper userMapper,
                           SpiritWalletService wallet,
                           EventService eventService) {
        this.sectMapper = sectMapper;
        this.sectMemberMapper = sectMemberMapper;
        this.userMapper = userMapper;
        this.wallet = wallet;
        this.eventService = eventService;
    }

    @Override
    public List<Map<String, Object>> list() {
        List<Sect> sects = sectMapper.selectList(Wrappers.<Sect>lambdaQuery().orderByAsc(Sect::getId));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Sect s : sects) {
            Long members = sectMemberMapper.selectCount(
                    Wrappers.<SectMember>lambdaQuery().eq(SectMember::getSectId, s.getId()));
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("name", s.getName());
            m.put("element", s.getElement());
            m.put("leaderName", s.getLeaderName());
            m.put("notice", s.getNotice() == null ? "" : s.getNotice());
            m.put("memberCount", members);
            out.add(m);
        }
        return out;
    }

    @Override
    public Map<String, Object> mine(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        SectMember me = myMember(userId);
        Map<String, Object> out = new LinkedHashMap<>();
        if (me == null) {
            out.put("hasSect", false);
            out.put("balance", wallet.balance(userId));
            return out;
        }
        Sect sect = sectMapper.selectById(me.getSectId());
        List<SectMember> members = sectMemberMapper.selectList(Wrappers.<SectMember>lambdaQuery()
                .eq(SectMember::getSectId, me.getSectId()).orderByDesc(SectMember::getContribution));
        List<Map<String, Object>> ms = new ArrayList<>();
        for (SectMember m : members) {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("userId", m.getUserId());
            mm.put("username", m.getUsername());
            mm.put("role", m.getRole());
            mm.put("roleName", roleName(m.getRole()));
            mm.put("contribution", m.getContribution());
            ms.add(mm);
        }
        int total = 0;
        for (SectMember m : members) {
            total += m.getContribution() == null ? 0 : m.getContribution();
        }
        out.put("hasSect", true);
        out.put("sect", sect == null ? null : sectView(sect));
        out.put("members", ms);
        out.put("myContribution", me.getContribution());
        out.put("totalContribution", total);
        out.put("myRole", me.getRole());
        out.put("myRoleName", roleName(me.getRole()));
        out.put("balance", wallet.balance(userId));
        return out;
    }

    @Override
    public Map<String, Object> create(String authorization, String name, String element) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        String username = (String) u[1];
        if (name == null || name.isBlank() || name.trim().length() > 12) {
            throw new BizException("宗门名须在 12 字之内");
        }
        if (myMember(userId) != null) {
            throw new BizException("你已有山门，不可另立");
        }
        String finalName = name.trim();
        Long dup = sectMapper.selectCount(Wrappers.<Sect>lambdaQuery().eq(Sect::getName, finalName));
        if (dup != null && dup > 0) {
            throw new BizException("「" + finalName + "」已有人立宗");
        }
        Sect s = new Sect();
        s.setName(finalName);
        s.setElement(element == null || element.isBlank() ? "huo" : element);
        s.setLeaderId(userId);
        s.setLeaderName(username);
        s.setNotice("山门初立，共勉之。");
        s.setCreateTime(LocalDateTime.now());
        sectMapper.insert(s);

        SectMember m = new SectMember();
        m.setSectId(s.getId());
        m.setUserId(userId);
        m.setUsername(username);
        m.setRole(2);
        m.setContribution(0);
        m.setJoinTime(LocalDateTime.now());
        sectMemberMapper.insert(m);

        eventService.record("山友 " + username + " 立宗「" + finalName + "」，山门再添一处。");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("sectId", s.getId());
        out.put("name", s.getName());
        return out;
    }

    @Override
    public Map<String, Object> join(String authorization, Long sectId) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        String username = (String) u[1];
        if (myMember(userId) != null) {
            throw new BizException("你已有山门，不可另投");
        }
        Sect s = sectMapper.selectById(sectId);
        if (s == null) {
            throw new BizException("查无此宗");
        }
        SectMember m = new SectMember();
        m.setSectId(sectId);
        m.setUserId(userId);
        m.setUsername(username);
        m.setRole(0);
        m.setContribution(0);
        m.setJoinTime(LocalDateTime.now());
        sectMemberMapper.insert(m);

        eventService.record("山友 " + username + " 投「" + s.getName() + "」门下。");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("sectName", s.getName());
        return out;
    }

    @Override
    public Map<String, Object> quit(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        SectMember me = myMember(userId);
        if (me == null) {
            throw new BizException("你本无宗门");
        }
        if (me.getRole() != null && me.getRole() == 2) {
            throw new BizException("你是宗主，需先传位他人方可离山");
        }
        sectMemberMapper.deleteById(me.getId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    @Override
    public Map<String, Object> donate(String authorization, Integer amount) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        SectMember me = myMember(userId);
        if (me == null) {
            throw new BizException("你尚无宗门");
        }
        int amt = amount == null ? 50 : Math.max(1, Math.min(amount, 2000));
        wallet.deduct(userId, amt, "宗门捐献");
        me.setContribution((me.getContribution() == null ? 0 : me.getContribution()) + amt);
        sectMemberMapper.updateById(me);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("contribution", me.getContribution());
        out.put("balance", wallet.balance(userId));
        return out;
    }

    /* ------------------------------ 传位 / 解散 ------------------------------ */
    @Override
    public Map<String, Object> transfer(String authorization, Long targetUserId) {
        Object[] u = requireUser(authorization);
        Long meId = (Long) u[0];
        SectMember me = myMember(meId);
        if (me == null) {
            throw new BizException("你尚无宗门");
        }
        if (me.getRole() == null || me.getRole() != 2) {
            throw new BizException("只有宗主可传位");
        }
        if (Objects.equals(targetUserId, meId)) {
            throw new BizException("不可传位于己");
        }
        SectMember target = sectMemberMapper.selectOne(Wrappers.<SectMember>lambdaQuery()
                .eq(SectMember::getSectId, me.getSectId())
                .eq(SectMember::getUserId, targetUserId));
        if (target == null) {
            throw new BizException("此人不在你宗门");
        }
        me.setRole(0);
        target.setRole(2);
        sectMemberMapper.updateById(me);
        sectMemberMapper.updateById(target);

        Sect s = sectMapper.selectById(me.getSectId());
        String sectName = s == null ? "" : s.getName();
        if (s != null) {
            s.setLeaderId(target.getUserId());
            s.setLeaderName(target.getUsername());
            sectMapper.updateById(s);
        }
        eventService.record("山友 " + me.getUsername() + " 将「" + sectName + "」宗主之位传于 "
                + target.getUsername() + "。");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("newLeader", target.getUsername());
        return out;
    }

    @Override
    public Map<String, Object> disband(String authorization) {
        Object[] u = requireUser(authorization);
        Long meId = (Long) u[0];
        SectMember me = myMember(meId);
        if (me == null) {
            throw new BizException("你尚无宗门");
        }
        if (me.getRole() == null || me.getRole() != 2) {
            throw new BizException("只有宗主可解散宗门");
        }
        Sect s = sectMapper.selectById(me.getSectId());
        String sectName = s == null ? "" : s.getName();
        sectMemberMapper.delete(Wrappers.<SectMember>lambdaQuery()
                .eq(SectMember::getSectId, me.getSectId()));
        if (s != null) {
            sectMapper.deleteById(s.getId());
        }
        eventService.record("山门「" + sectName + "」就此散伙，各奔前程。");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("name", sectName);
        return out;
    }

    /* ------------------------------ 工具 ------------------------------ */
    private SectMember myMember(Long userId) {
        return sectMemberMapper.selectOne(Wrappers.<SectMember>lambdaQuery()
                .eq(SectMember::getUserId, userId).last("limit 1"));
    }

    private Map<String, Object> sectView(Sect s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("name", s.getName());
        m.put("element", s.getElement());
        m.put("leaderName", s.getLeaderName());
        m.put("notice", s.getNotice() == null ? "" : s.getNotice());
        return m;
    }

    private static String roleName(Integer role) {
        if (role == null) return "弟子";
        switch (role) {
            case 2:
                return "宗主";
            case 1:
                return "长老";
            default:
                return "弟子";
        }
    }

    private Object[] requireUser(String authorization) {
        String t = authorization;
        if (t != null && t.startsWith("Bearer ")) {
            t = t.substring(7);
        }
        Object[] parsed = TokenUtil.parse(t);
        if (parsed == null) {
            throw new BizException("令牌无效，请重新入境");
        }
        User user = userMapper.selectById((Long) parsed[0]);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BizException("查无此山籍，请重新入境");
        }
        return parsed;
    }
}
