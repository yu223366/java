package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.entity.Announcement;
import com.fenglin.springboottest.entity.AuctionBid;
import com.fenglin.springboottest.entity.AuctionResult;
import com.fenglin.springboottest.entity.Discourse;
import com.fenglin.springboottest.entity.Item;
import com.fenglin.springboottest.entity.Message;
import com.fenglin.springboottest.entity.OnlineUser;
import com.fenglin.springboottest.entity.SearchLog;
import com.fenglin.springboottest.entity.SignIn;
import com.fenglin.springboottest.entity.SpiritProfile;
import com.fenglin.springboottest.mapper.*;
import com.fenglin.springboottest.service.ObserverService;
import com.fenglin.springboottest.service.SseService;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 观山台数据聚合：全部只读统计，不做任何写操作。
 */
@Service
public class ObserverServiceImpl implements ObserverService {

    private final UserMapper userMapper;
    private final OnlineUserMapper onlineUserMapper;
    private final SignInMapper signInMapper;
    private final AuctionResultMapper auctionResultMapper;
    private final ExploreLogMapper exploreLogMapper;
    private final SearchLogMapper searchLogMapper;
    private final DiscourseMapper discourseMapper;
    private final AnnouncementMapper announcementMapper;
    private final ItemMapper itemMapper;
    private final MessageMapper messageMapper;
    private final SpiritProfileMapper spiritProfileMapper;
    private final AuctionBidMapper auctionBidMapper;
    private final SseService sseService;

    public ObserverServiceImpl(UserMapper userMapper,
                               OnlineUserMapper onlineUserMapper,
                               SignInMapper signInMapper,
                               AuctionResultMapper auctionResultMapper,
                               ExploreLogMapper exploreLogMapper,
                               SearchLogMapper searchLogMapper,
                               DiscourseMapper discourseMapper,
                               AnnouncementMapper announcementMapper,
                               ItemMapper itemMapper,
                               MessageMapper messageMapper,
                               SpiritProfileMapper spiritProfileMapper,
                               AuctionBidMapper auctionBidMapper,
                               SseService sseService) {
        this.userMapper = userMapper;
        this.onlineUserMapper = onlineUserMapper;
        this.signInMapper = signInMapper;
        this.auctionResultMapper = auctionResultMapper;
        this.exploreLogMapper = exploreLogMapper;
        this.searchLogMapper = searchLogMapper;
        this.discourseMapper = discourseMapper;
        this.announcementMapper = announcementMapper;
        this.itemMapper = itemMapper;
        this.messageMapper = messageMapper;
        this.spiritProfileMapper = spiritProfileMapper;
        this.auctionBidMapper = auctionBidMapper;
        this.sseService = sseService;
    }

    @Override
    public Map<String, Object> overview() {
        Map<String, Object> m = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = LocalDateTime.of(today, LocalTime.MIN);

        Long online = onlineUserMapper.selectCount(Wrappers.<OnlineUser>lambdaQuery()
                .ge(OnlineUser::getLastActiveTime, LocalDateTime.now().minusMinutes(10)));
        m.put("online", (online == null ? 0 : online) + sseService.subscriberCount());
        m.put("userTotal", userMapper.selectCount(null));

        Long signCount = signInMapper.selectCount(Wrappers.<SignIn>lambdaQuery()
                .eq(SignIn::getSignDate, today));
        m.put("signInToday", signCount);

        List<AuctionResult> todayResults = auctionResultMapper.selectList(
                Wrappers.<AuctionResult>lambdaQuery().ge(AuctionResult::getHammerTime, dayStart));
        int amount = 0;
        int sold = 0;
        for (AuctionResult r : todayResults) {
            amount += r.getFinalPrice() == null ? 0 : r.getFinalPrice();
            if (r.getWinnerId() != null) sold++;
        }
        m.put("auctionCount", todayResults.size());
        m.put("auctionSold", sold);
        m.put("auctionAmount", amount);
        m.put("exploreTotal", exploreLogMapper.selectCount(null));

        // 热门搜索：取近期日志按关键词聚合
        List<SearchLog> logs = searchLogMapper.selectList(Wrappers.<SearchLog>lambdaQuery()
                .orderByDesc(SearchLog::getId).last("limit 300"));
        Map<String, Integer> kw = new LinkedHashMap<>();
        for (SearchLog l : logs) {
            if (l.getKeyword() == null || l.getKeyword().isBlank()) continue;
            kw.merge(l.getKeyword().trim(), 1, Integer::sum);
        }
        List<Map<String, Object>> hot = new ArrayList<>();
        kw.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .forEach(e -> {
                    Map<String, Object> h = new LinkedHashMap<>();
                    h.put("keyword", e.getKey());
                    h.put("count", e.getValue());
                    hot.add(h);
                });
        m.put("hotSearch", hot);

        // 热议榜
        List<Map<String, Object>> talks = new ArrayList<>();
        for (Discourse d : discourseMapper.selectList(Wrappers.<Discourse>lambdaQuery()
                .eq(Discourse::getStatus, 1).orderByDesc(Discourse::getZan).last("limit 5"))) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("name", d.getName());
            t.put("quote", d.getQuote());
            t.put("zan", d.getZan());
            talks.add(t);
        }
        m.put("hotDiscourse", talks);

        // 山门大事记
        List<Map<String, Object>> events = new ArrayList<>();
        for (Announcement a : announcementMapper.selectList(Wrappers.<Announcement>lambdaQuery()
                .eq(Announcement::getStatus, 1).orderByAsc(Announcement::getSort)
                .orderByDesc(Announcement::getCreateTime).last("limit 8"))) {
            Map<String, Object> e = new LinkedHashMap<>();
            e.put("text", a.getText());
            e.put("time", a.getCreateTime() == null ? "" : a.getCreateTime().toString());
            events.add(e);
        }
        m.put("events", events);
        m.put("elementDist", elementDist());
        m.put("gradeDist", gradeDist());
        m.put("priceTrend", priceTrend());
        m.put("signTrend", signTrend());
        return m;
    }

    /** 五行条目分布（宝库条目按元素统计） */
    private List<Map<String, Object>> elementDist() {
        Map<String, Integer> cnt = new LinkedHashMap<>();
        for (Item it : itemMapper.selectList(null)) {
            cnt.merge(it.getElement() == null ? "huo" : it.getElement(), 1, Integer::sum);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : cnt.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("element", e.getKey());
            m.put("count", e.getValue());
            out.add(m);
        }
        return out;
    }

    /** 品阶分布 */
    private List<Map<String, Object>> gradeDist() {
        Map<String, Integer> cnt = new LinkedHashMap<>();
        for (Item it : itemMapper.selectList(null)) {
            cnt.merge(it.getGradeKey() == null ? "fan" : it.getGradeKey(), 1, Integer::sum);
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : cnt.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("grade", e.getKey());
            m.put("count", e.getValue());
            out.add(m);
        }
        return out;
    }

    /** 近 8 场成交价走势（时间正序） */
    private List<Map<String, Object>> priceTrend() {
        List<AuctionResult> list = auctionResultMapper.selectList(Wrappers.<AuctionResult>lambdaQuery()
                .gt(AuctionResult::getFinalPrice, 0).orderByDesc(AuctionResult::getId).last("limit 8"));
        java.util.Collections.reverse(list);
        List<Map<String, Object>> out = new ArrayList<>();
        for (AuctionResult r : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("roundNo", r.getRoundNo());
            m.put("title", r.getItemTitle());
            m.put("price", r.getFinalPrice());
            out.add(m);
        }
        return out;
    }

    /** 近 7 天签到趋势 */
    private List<Map<String, Object>> signTrend() {
        LocalDate today = LocalDate.now();
        List<SignIn> list = signInMapper.selectList(Wrappers.<SignIn>lambdaQuery()
                .ge(SignIn::getSignDate, today.minusDays(6)).orderByAsc(SignIn::getSignDate));
        Map<String, Integer> cnt = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            cnt.put(today.minusDays(i).toString(), 0);
        }
        for (SignIn s : list) {
            if (s.getSignDate() != null) {
                cnt.merge(s.getSignDate().toString(), 1, Integer::sum);
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, Integer> e : cnt.entrySet()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("date", e.getKey());
            m.put("count", e.getValue());
            out.add(m);
        }
        return out;
    }

    /**
     * 导航栏徽标：一次请求拿齐所有入口的状态，前端 5 秒刷一次即可。
     */
    @Override
    public Map<String, Object> badges(String authorization) {
        Map<String, Object> m = new LinkedHashMap<>();
        Long online = onlineUserMapper.selectCount(Wrappers.<OnlineUser>lambdaQuery()
                .ge(OnlineUser::getLastActiveTime, LocalDateTime.now().minusMinutes(10)));
        m.put("online", (online == null ? 0 : online) + sseService.subscriberCount());
        m.put("treasures", itemMapper.selectCount(null));

        // 解析令牌拿个人状态（未登录则只给公共统计）
        String t = authorization;
        if (t != null && t.startsWith("Bearer ")) {
            t = t.substring(7);
        }
        Object[] parsed = TokenUtil.parse(t);
        if (parsed == null) {
            m.put("unread", 0);
            m.put("hasSpirit", false);
            m.put("myBids", 0);
            m.put("mine", false);
            return m;
        }
        Long uid = (Long) parsed[0];
        Long unread = messageMapper.selectCount(Wrappers.<Message>lambdaQuery()
                .eq(Message::getIsRead, 0).eq(Message::getToId, uid));
        m.put("unread", unread == null ? 0 : unread);
        Long spirits = spiritProfileMapper.selectCount(
                Wrappers.<SpiritProfile>lambdaQuery().eq(SpiritProfile::getUserId, uid));
        m.put("hasSpirit", spirits != null && spirits > 0);
        Long bids = auctionBidMapper.selectCount(
                Wrappers.<AuctionBid>lambdaQuery().eq(AuctionBid::getUserId, uid));
        m.put("myBids", bids == null ? 0 : bids);
        m.put("mine", true);
        return m;
    }
}
