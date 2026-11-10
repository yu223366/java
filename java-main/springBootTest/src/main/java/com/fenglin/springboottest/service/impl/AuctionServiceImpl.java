package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.*;
import com.fenglin.springboottest.mapper.*;
import com.fenglin.springboottest.service.AuctionService;
import com.fenglin.springboottest.service.SseService;
import com.fenglin.springboottest.service.SpiritWalletService;
import com.fenglin.springboottest.util.TokenUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拍卖行核心服务（单例内存状态机 + 数据库留痕）。
 *
 * <p>规则（参数来自 application.properties 的 wushan.auction.*）：
 * 每 10 分钟一场、提前 1 分钟通知所有在线用户、每场 5 件按品级由低到高、
 * 起拍 100 灵石、加价 10/50/100/500、出价后重置 5 秒倒计时，
 * 无人加价即落槌归最高出价者；只有注册且在线的用户可以出价。</p>
 *
 * <p>为什么用内存状态机：倒计时是秒级的、所有客户端必须看到同一份状态，
 * 用数据库轮询做秒级同步既慢又容易错乱；数据库只负责场次、出价流水、成交记录留痕，
 * 服务重启后从最近的 round_no 继续编号，下一场自动重新开场。</p>
 */
@Service
public class AuctionServiceImpl implements AuctionService {

    private static final Logger log = LoggerFactory.getLogger(AuctionServiceImpl.class);

    /** 允许的加价档（需求固定） */
    private static final List<Integer> INCREMENTS = List.of(10, 50, 100, 500);

    /**
     * 可拍卖的分类：功法(1) 及其子类 心法/身法/术法/阵法(4~7)、丹药(10)、法器(11)。
     * 即需求里的「功法、法宝、丹药」。
     */
    private static final List<Long> AUCTION_CATEGORY_IDS = List.of(1L, 4L, 5L, 6L, 7L, 10L, 11L);

    /** 在线判定窗口：前端每 2~3 秒轮询 status，30 秒无心跳视为离线 */
    private static final long ONLINE_TIMEOUT_MS = 30_000L;

    private final Object lock = new Object();

    @Value("${wushan.auction.round-interval-seconds:600}")
    private int roundIntervalSeconds;
    @Value("${wushan.auction.notify-before-seconds:60}")
    private int notifyBeforeSeconds;
    @Value("${wushan.auction.countdown-seconds:5}")
    private int countdownSeconds;
    @Value("${wushan.auction.batch-size:5}")
    private int batchSize;
    @Value("${wushan.auction.start-price:100}")
    private int startPrice;
    @Value("${wushan.auction.item-gap-seconds:3}")
    private int itemGapSeconds;
    @Value("${wushan.auction.first-round-delay-seconds:30}")
    private int firstRoundDelaySeconds;

    private final ItemMapper itemMapper;
    private final GradeMapper gradeMapper;
    private final CategoryMapper categoryMapper;
    private final UserMapper userMapper;
    private final AuctionRoundMapper auctionRoundMapper;
    private final AuctionBidMapper auctionBidMapper;
    private final AuctionResultMapper auctionResultMapper;
    private final OnlineUserMapper onlineUserMapper;
    private final AuctionNoticeMapper auctionNoticeMapper;
    private final AuctionWatchMapper auctionWatchMapper;
    private final SseService sseService;
    private final SpiritWalletService wallet;

    @Value("${wushan.auction.commission-percent:5}")
    private int commissionPercent;

    public AuctionServiceImpl(ItemMapper itemMapper,
                              GradeMapper gradeMapper,
                              CategoryMapper categoryMapper,
                              UserMapper userMapper,
                              AuctionRoundMapper auctionRoundMapper,
                              AuctionBidMapper auctionBidMapper,
                              AuctionResultMapper auctionResultMapper,
                              OnlineUserMapper onlineUserMapper,
                              AuctionNoticeMapper auctionNoticeMapper,
                              AuctionWatchMapper auctionWatchMapper,
                              SseService sseService,
                              SpiritWalletService wallet) {
        this.itemMapper = itemMapper;
        this.gradeMapper = gradeMapper;
        this.categoryMapper = categoryMapper;
        this.userMapper = userMapper;
        this.auctionRoundMapper = auctionRoundMapper;
        this.auctionBidMapper = auctionBidMapper;
        this.auctionResultMapper = auctionResultMapper;
        this.onlineUserMapper = onlineUserMapper;
        this.auctionNoticeMapper = auctionNoticeMapper;
        this.auctionWatchMapper = auctionWatchMapper;
        this.sseService = sseService;
        this.wallet = wallet;
    }

    /* ------------------------------ 内存状态 ------------------------------ */
    private static final class State {
        /** waiting=候场 / bidding=竞价中 / gap=落槌间歇 */
        String phase = "waiting";
        int roundNo = 0;
        List<Map<String, Object>> items = new ArrayList<>();
        int index = 0;
        int price = 0;
        Long leaderId;
        String leaderName = "";
        int countdown = 0;
        int gapLeft = 0;
        long nextRoundAt;
        boolean notified = false;
        String noticeText = "";
        long noticeUntil = 0L;
        String lastResult = "";
    }

    private final State state = new State();

    /** userId -> 最近一次心跳时间（毫秒） */
    private final Map<Long, Long> onlineAt = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        synchronized (lock) {
            List<AuctionRound> last = auctionRoundMapper.selectList(
                    Wrappers.<AuctionRound>lambdaQuery()
                            .orderByDesc(AuctionRound::getRoundNo).last("limit 1"));
            state.roundNo = last.isEmpty() || last.get(0).getRoundNo() == null
                    ? 0 : last.get(0).getRoundNo();
            state.phase = "waiting";
            state.nextRoundAt = System.currentTimeMillis() + firstRoundDelaySeconds * 1000L;
            log.info("拍卖行已启动：第 {} 场将在 {} 秒后开拍",
                    state.roundNo + 1, firstRoundDelaySeconds);
        }
    }

    /* ------------------------------ 每秒调度 ------------------------------ */
    @Scheduled(fixedDelay = 1000)
    public void tick() {
        try {
            synchronized (lock) {
                long now = System.currentTimeMillis();
                State s = state;
                if ("waiting".equals(s.phase)) {
                    long left = s.nextRoundAt - now;
                    // 提前 1 分钟通知所有在线用户（在线用户轮询 status 时即可收到）
                    if (!s.notified && left <= notifyBeforeSeconds * 1000L && left > 0) {
                        s.notified = true;
                        s.noticeText = "第 " + (s.roundNo + 1)
                                + " 场拍卖将在 1 分钟后开拍，请在线山友准备参与！";
                        s.noticeUntil = s.nextRoundAt + 8_000L;
                        saveNotice(s.roundNo + 1, s.noticeText);
                        sseService.broadcast("notify", s.noticeText);
                        log.info("拍卖通知：{}", s.noticeText);
                    }
                    if (left <= 0) {
                        startRound(now);
                    }
                } else if ("bidding".equals(s.phase)) {
                    s.countdown -= 1;
                    if (s.countdown <= 0) {
                        hammer();
                    }
                } else if ("gap".equals(s.phase)) {
                    s.gapLeft -= 1;
                    if (s.gapLeft <= 0) {
                        nextItem(now);
                    }
                }
            }
        } catch (Exception e) {
            // 单次调度异常不能让整个定时线程停摆
            log.error("拍卖调度异常", e);
        }
    }

    /* ------------------------------ 场次流程 ------------------------------ */
    private void startRound(long now) {
        State s = state;
        s.roundNo += 1;
        List<Item> pool = loadPool();
        // 池子不够一场时，把已拍出的物品重新放回池，保证长期有得拍
        if (pool.size() < batchSize) {
            resetAuctionFlags();
            pool = loadPool();
        }
        List<Map<String, Object>> picked = pickItems(pool);
        if (picked.isEmpty()) {
            s.phase = "waiting";
            s.nextRoundAt = now + roundIntervalSeconds * 1000L;
            s.notified = false;
            s.noticeText = "暂无可拍物品，下一场稍后再试";
            s.noticeUntil = now + 10_000L;
            return;
        }
        s.items = picked;
        s.index = 0;
        s.price = startPrice;
        s.leaderId = null;
        s.leaderName = "";
        s.countdown = countdownSeconds;
        s.phase = "bidding";
        s.notified = false;
        s.lastResult = "";
        s.noticeText = "第 " + s.roundNo + " 场开拍，共 " + s.items.size() + " 件拍品！";
        s.noticeUntil = now + 10_000L;
        saveRound(s.roundNo, s.items.size());
        sseService.broadcast("round-start", "第 " + s.roundNo + " 场开拍，共 " + s.items.size() + " 件拍品！");
        // 捡漏提醒：拍品里有被关注的东西，推给关注者
        for (Map<String, Object> it : s.items) {
            Long watchers = auctionWatchMapper.selectCount(Wrappers.<AuctionWatch>lambdaQuery()
                    .eq(AuctionWatch::getItemTitle, String.valueOf(it.get("title"))));
            if (watchers != null && watchers > 0) {
                sseService.broadcast("watch", "「" + it.get("title") + "」再度上拍，"
                        + watchers + " 位山友在候，莫要错过。");
            }
        }
        log.info("第 {} 场开拍，拍品 {} 件", s.roundNo, s.items.size());
    }

    /** 落槌：归最高出价者，并把该物品 auction 置 1（不再入池） */
    @SuppressWarnings("unchecked")
    private void hammer() {
        State s = state;
        Map<String, Object> cur = currentItem(s);
        if (cur != null) {
            long itemId = ((Number) cur.get("id")).longValue();
            AuctionResult r = new AuctionResult();
            r.setRoundNo(s.roundNo);
            r.setItemId(itemId);
            r.setItemTitle(String.valueOf(cur.get("title")));
            if (s.leaderId != null) {
                r.setWinnerId(s.leaderId);
                r.setWinnerName(s.leaderName);
                r.setFinalPrice(s.price);
            } else {
                r.setWinnerName("");
                r.setFinalPrice(0);
            }
            r.setHammerTime(LocalDateTime.now());
            auctionResultMapper.insert(r);

            Item patch = new Item();
            patch.setId(itemId);
            patch.setAuction(1);
            itemMapper.updateById(patch);

            s.lastResult = s.leaderId == null
                    ? "流拍 · " + cur.get("title") + " 无人出价"
                    : "落槌 · " + cur.get("title") + " 由 " + s.leaderName
                    + " 以 " + s.price + " 灵石拍得";
            // 成交佣金：落槌时从得主身上扣除
            if (s.leaderId != null && commissionPercent > 0) {
                int commission = s.price * commissionPercent / 100;
                if (commission > 0) {
                    try {
                        wallet.deduct(s.leaderId, commission, "拍卖佣金 · " + cur.get("title"));
                        s.lastResult += "（佣金 " + commission + " 灵石）";
                    } catch (BizException ignored) {
                        // 灵石不足时免扣，不影响落槌
                    }
                }
            }
            log.info("第 {} 场第 {} 件：{}", s.roundNo, s.index + 1, s.lastResult);
        }
        s.phase = "gap";
        s.gapLeft = itemGapSeconds;
        sseService.broadcast("hammer", s.lastResult);
    }

    private void nextItem(long now) {
        State s = state;
        if (s.index >= s.items.size() - 1) {
            finishRound(s.roundNo);
            s.phase = "waiting";
            s.nextRoundAt = now + roundIntervalSeconds * 1000L;
            s.notified = false;
            s.noticeText = "本场已毕，下一场 " + (roundIntervalSeconds / 60) + " 分钟后开拍";
            s.noticeUntil = now + 10_000L;
            s.index = 0;
            log.info("第 {} 场结束", s.roundNo);
        } else {
            s.index += 1;
            s.price = startPrice;
            s.leaderId = null;
            s.leaderName = "";
            s.countdown = countdownSeconds;
            s.phase = "bidding";
        }
    }

    /* ------------------------------ 拍品池 ------------------------------ */
    private List<Item> loadPool() {
        List<Item> all = itemMapper.selectList(Wrappers.<Item>lambdaQuery()
                .eq(Item::getAuction, 0)
                .eq(Item::getStatus, 1)
                .in(Item::getCategoryId, AUCTION_CATEGORY_IDS));
        Map<String, Integer> rank = gradeRankMap();
        all.sort(Comparator
                .comparingInt((Item i) -> rank.getOrDefault(i.getGradeKey(), 99))
                .thenComparingLong(i -> i.getId() == null ? 0L : i.getId()));
        return all;
    }

    /** 每个品阶先各取一件（保证一场内由低到高），不足再用低品阶补齐 */
    private List<Map<String, Object>> pickItems(List<Item> pool) {
        List<Item> picked = new ArrayList<>();
        Set<String> usedGrades = new HashSet<>();
        for (Item it : pool) {
            if (picked.size() >= batchSize) break;
            String g = it.getGradeKey() == null ? "" : it.getGradeKey();
            if (usedGrades.add(g)) picked.add(it);
        }
        Set<Long> pickedIds = new HashSet<>();
        for (Item it : picked) pickedIds.add(it.getId());
        for (Item it : pool) {
            if (picked.size() >= batchSize) break;
            if (pickedIds.add(it.getId())) picked.add(it);
        }

        Map<Long, String> catNames = categoryNameMap();
        Map<String, String> gradeNames = gradeNameMap();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Item it : picked) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", it.getId());
            m.put("title", it.getTitle());
            m.put("desc", it.getSummary() == null ? "" : it.getSummary());
            m.put("element", it.getElement() == null ? "huo" : it.getElement());
            m.put("grade", it.getGradeKey());
            m.put("gradeLabel", gradeNames.getOrDefault(it.getGradeKey(), "凡品"));
            m.put("categoryId", it.getCategoryId());
            m.put("categoryName", catNames.getOrDefault(it.getCategoryId(), "功法"));
            m.put("startPrice", startPrice);
            out.add(m);
        }
        return out;
    }

    private void resetAuctionFlags() {
        Item patch = new Item();
        patch.setAuction(0);
        itemMapper.update(patch, Wrappers.<Item>lambdaUpdate().eq(Item::getAuction, 1));
    }

    private Map<String, Integer> gradeRankMap() {
        Map<String, Integer> m = new HashMap<>();
        for (Grade g : gradeMapper.selectList(null)) {
            m.put(g.getGradeKey(), g.getGradeRank() == null ? 99 : g.getGradeRank());
        }
        return m;
    }

    private Map<String, String> gradeNameMap() {
        Map<String, String> m = new HashMap<>();
        for (Grade g : gradeMapper.selectList(null)) m.put(g.getGradeKey(), g.getName());
        return m;
    }

    private Map<Long, String> categoryNameMap() {
        Map<Long, String> m = new HashMap<>();
        for (Category c : categoryMapper.selectList(null)) m.put(c.getId(), c.getName());
        return m;
    }

    /* ------------------------------ 数据库留痕 ------------------------------ */
    private void saveRound(int roundNo, int count) {
        AuctionRound r = new AuctionRound();
        r.setRoundNo(roundNo);
        r.setStatus(1);
        r.setItemCount(count);
        r.setStartTime(LocalDateTime.now());
        r.setCreateTime(LocalDateTime.now());
        auctionRoundMapper.insert(r);
    }

    private void finishRound(int roundNo) {
        AuctionRound r = auctionRoundMapper.selectOne(
                Wrappers.<AuctionRound>lambdaQuery().eq(AuctionRound::getRoundNo, roundNo));
        if (r != null) {
            r.setStatus(2);
            r.setEndTime(LocalDateTime.now());
            auctionRoundMapper.updateById(r);
        }
    }

    private void saveNotice(int roundNo, String content) {
        Long cnt = auctionNoticeMapper.selectCount(
                Wrappers.<AuctionNotice>lambdaQuery().eq(AuctionNotice::getRoundNo, roundNo));
        if (cnt != null && cnt > 0) return;
        AuctionNotice n = new AuctionNotice();
        n.setRoundNo(roundNo);
        n.setContent(content);
        n.setNotifyTime(LocalDateTime.now());
        auctionNoticeMapper.insert(n);
    }

    private void upsertOnline(Long userId, String username) {
        OnlineUser ou = new OnlineUser();
        ou.setUserId(userId);
        ou.setUsername(username);
        ou.setLastActiveTime(LocalDateTime.now());
        if (onlineUserMapper.selectById(userId) == null) {
            onlineUserMapper.insert(ou);
        } else {
            onlineUserMapper.updateById(ou);
        }
    }

    /* ------------------------------ 对外接口 ------------------------------ */
    @Override
    public Map<String, Object> status(String authorization) {
        Object[] u = parseUser(authorization);
        long now = System.currentTimeMillis();
        if (u != null) {
            Long uid = (Long) u[0];
            onlineAt.put(uid, now);
            upsertOnline(uid, (String) u[1]);
        }
        onlineAt.entrySet().removeIf(e -> now - e.getValue() > ONLINE_TIMEOUT_MS);

        Map<String, Object> m = new LinkedHashMap<>();
        synchronized (lock) {
            State s = state;
            boolean showNotice = !s.noticeText.isEmpty() && now <= s.noticeUntil;
            m.put("phase", s.phase);
            m.put("roundNo", s.roundNo);
            m.put("items", new ArrayList<>(s.items));
            m.put("index", s.index);
            m.put("current", currentItem(s));
            m.put("price", s.price);
            m.put("leader", s.leaderName);
            m.put("countdown", Math.max(0, s.countdown));
            m.put("secondsToNextRound", Math.max(0, (s.nextRoundAt - now) / 1000));
            m.put("notify", showNotice);
            m.put("noticeText", showNotice ? s.noticeText : "");
            m.put("lastResult", s.lastResult);
            m.put("onlineCount", onlineAt.size());
            m.put("loggedIn", u != null);
            m.put("me", u == null ? "" : u[1]);
            m.put("rules", rules());
            m.put("logs", recentBids(s.roundNo));
        }
        return m;
    }

    @Override
    public Map<String, Object> bid(String authorization, Integer increment) {
        Object[] u = parseUser(authorization);
        if (u == null) {
            throw new BizException("请先登录后参与拍卖");
        }
        Long uid = (Long) u[0];
        String name = (String) u[1];
        Long last = onlineAt.get(uid);
        if (last == null || System.currentTimeMillis() - last > ONLINE_TIMEOUT_MS) {
            throw new BizException("您当前不在线，请刷新拍卖行页面后再出价");
        }
        if (increment == null || !INCREMENTS.contains(increment)) {
            throw new BizException("加价档只能是 10 / 50 / 100 / 500 灵石");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        synchronized (lock) {
            State s = state;
            if (!"bidding".equals(s.phase)) {
                throw new BizException("当前不在竞价中，请稍候下一件");
            }
            s.price += increment;
            s.leaderId = uid;
            s.leaderName = name;
            s.countdown = countdownSeconds;
            s.lastResult = "";

            AuctionBid b = new AuctionBid();
            b.setRoundNo(s.roundNo);
            Map<String, Object> cur = currentItem(s);
            if (cur != null) {
                b.setItemId(((Number) cur.get("id")).longValue());
                b.setItemTitle(String.valueOf(cur.get("title")));
            } else {
                b.setItemId(0L);
                b.setItemTitle("");
            }
            b.setUserId(uid);
            b.setUsername(name);
            b.setIncrementAmount(increment);
            b.setPrice(s.price);
            b.setBidTime(LocalDateTime.now());
            auctionBidMapper.insert(b);

            out.put("ok", true);
            out.put("price", s.price);
            out.put("bidder", name);
            out.put("increment", increment);
            out.put("countdown", s.countdown);
        }
        sseService.broadcast("bid", out.get("bidder") + " 加价 " + increment + " → " + out.get("price") + " 灵石");
        return out;
    }

    @Override
    public Map<String, Object> itemDetail(Long itemId) {
        Item it = itemMapper.selectById(itemId);
        if (it == null) {
            throw new BizException("查无此拍品");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", it.getId());
        m.put("title", it.getTitle());
        m.put("desc", it.getSummary() == null ? "" : it.getSummary());
        m.put("element", it.getElement() == null ? "huo" : it.getElement());
        m.put("grade", it.getGradeKey());
        m.put("gradeLabel", gradeNameMap().getOrDefault(it.getGradeKey(), "凡品"));
        m.put("categoryName", categoryNameMap().getOrDefault(it.getCategoryId(), "功法"));
        m.put("startPrice", startPrice);
        m.put("auction", it.getAuction());
        return m;
    }

    @Override
    public List<Map<String, Object>> logs(Integer roundNo) {
        int no = roundNo == null ? state.roundNo : roundNo;
        return recentBids(no);
    }

    /* ------------------------------ 往场回放 / 捡漏关注 ------------------------------ */
    @Override
    public List<Map<String, Object>> history(String authorization, Integer limit) {
        int n = limit == null || limit <= 0 ? 30 : Math.min(limit, 100);
        List<AuctionResult> list = auctionResultMapper.selectList(Wrappers.<AuctionResult>lambdaQuery()
                .orderByDesc(AuctionResult::getId).last("limit " + n));
        List<Map<String, Object>> out = new ArrayList<>();
        for (AuctionResult r : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("roundNo", r.getRoundNo());
            m.put("itemTitle", r.getItemTitle());
            m.put("winnerName", r.getWinnerName() == null || r.getWinnerName().isEmpty() ? "流 拍" : r.getWinnerName());
            m.put("finalPrice", r.getFinalPrice());
            m.put("hammerTime", r.getHammerTime() == null ? "" : r.getHammerTime().toString());
            out.add(m);
        }
        return out;
    }

    @Override
    public Map<String, Object> watch(String authorization, String itemTitle) {
        Object[] u = parseUser(authorization);
        if (u == null) {
            throw new BizException("请先登录后再关注拍品");
        }
        Long userId = (Long) u[0];
        if (itemTitle == null || itemTitle.isBlank()) {
            throw new BizException("请指明要关注的拍品");
        }
        String title = itemTitle.trim();
        Long cnt = auctionWatchMapper.selectCount(Wrappers.<AuctionWatch>lambdaQuery()
                .eq(AuctionWatch::getUserId, userId).eq(AuctionWatch::getItemTitle, title));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("itemTitle", title);
        if (cnt != null && cnt > 0) {
            out.put("already", true);
            return out;
        }
        AuctionWatch w = new AuctionWatch();
        w.setUserId(userId);
        w.setItemTitle(title);
        w.setCreateTime(LocalDateTime.now());
        auctionWatchMapper.insert(w);
        out.put("already", false);
        return out;
    }

    @Override
    public List<Map<String, Object>> watches(String authorization) {
        Object[] u = parseUser(authorization);
        if (u == null) {
            throw new BizException("请先登录");
        }
        Long userId = (Long) u[0];
        List<AuctionWatch> list = auctionWatchMapper.selectList(Wrappers.<AuctionWatch>lambdaQuery()
                .eq(AuctionWatch::getUserId, userId).orderByDesc(AuctionWatch::getId).last("limit 50"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (AuctionWatch w : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemTitle", w.getItemTitle());
            m.put("time", w.getCreateTime() == null ? "" : w.getCreateTime().toString());
            out.add(m);
        }
        return out;
    }

    /** 我的竞拍记录：出过的价、拍到过什么 */
    @Override
    public List<Map<String, Object>> myBids(String authorization) {
        Object[] u = parseUser(authorization);
        if (u == null) {
            throw new BizException("请先登录");
        }
        Long userId = (Long) u[0];
        List<AuctionBid> bids = auctionBidMapper.selectList(Wrappers.<AuctionBid>lambdaQuery()
                .eq(AuctionBid::getUserId, userId).orderByDesc(AuctionBid::getId).last("limit 30"));
        // 拍到的（成交记录里 winner 是我）
        List<AuctionResult> wins = auctionResultMapper.selectList(Wrappers.<AuctionResult>lambdaQuery()
                .eq(AuctionResult::getWinnerId, userId).orderByDesc(AuctionResult::getId).last("limit 30"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (AuctionResult r : wins) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "win");
            m.put("itemTitle", r.getItemTitle());
            m.put("price", r.getFinalPrice());
            m.put("time", r.getHammerTime() == null ? "" : r.getHammerTime().toString());
            out.add(m);
        }
        for (AuctionBid b : bids) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", "bid");
            m.put("itemTitle", b.getItemTitle());
            m.put("price", b.getPrice());
            m.put("increment", b.getIncrementAmount());
            m.put("time", b.getBidTime() == null ? "" : b.getBidTime().toString());
            out.add(m);
        }
        return out;
    }

    /* ------------------------------ 工具 ------------------------------ */
    private Map<String, Object> currentItem(State s) {
        if (s.items.isEmpty()) return null;
        int i = Math.min(Math.max(s.index, 0), s.items.size() - 1);
        return s.items.get(i);
    }

    private List<Map<String, Object>> recentBids(int roundNo) {
        List<AuctionBid> list = auctionBidMapper.selectList(
                Wrappers.<AuctionBid>lambdaQuery()
                        .eq(AuctionBid::getRoundNo, roundNo)
                        .orderByDesc(AuctionBid::getId).last("limit 20"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (AuctionBid b : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("text", b.getUsername() + " 加价 " + b.getIncrementAmount()
                    + " → " + b.getPrice() + " 灵石");
            m.put("username", b.getUsername());
            m.put("increment", b.getIncrementAmount());
            m.put("price", b.getPrice());
            m.put("itemTitle", b.getItemTitle());
            out.add(m);
        }
        return out;
    }

    private Map<String, Object> rules() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("batchSize", batchSize);
        r.put("startPrice", startPrice);
        r.put("increments", INCREMENTS);
        r.put("countdownSeconds", countdownSeconds);
        r.put("itemGapSeconds", itemGapSeconds);
        r.put("roundIntervalSeconds", roundIntervalSeconds);
        r.put("notifyBeforeSeconds", notifyBeforeSeconds);
        return r;
    }

    /** 解析 Bearer 令牌，并确认是有效注册用户 */
    private Object[] parseUser(String authorization) {
        String t = authorization;
        if (t != null && t.startsWith("Bearer ")) {
            t = t.substring(7);
        }
        Object[] parsed = TokenUtil.parse(t);
        if (parsed == null) return null;
        User user = userMapper.selectById((Long) parsed[0]);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) return null;
        return parsed;
    }
}
