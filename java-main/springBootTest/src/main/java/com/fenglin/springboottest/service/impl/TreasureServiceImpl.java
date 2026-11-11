package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.*;
import com.fenglin.springboottest.mapper.*;
import com.fenglin.springboottest.service.EventService;
import com.fenglin.springboottest.service.SpiritWalletService;
import com.fenglin.springboottest.service.TreasureService;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 宗门宝库与寄售实现。
 *
 * <p>所有涉及背包、库存、贡献点的增减都先做条件校验再落库；
 * 灵石的收付统一走 SpiritWalletService，保证流水可查。</p>
 */
@Service
public class TreasureServiceImpl implements TreasureService {

    /** 新宗门首次打开宝库时自动铺 12 件基础藏品 */
    private static final int INIT_STOCK_KINDS = 12;

    private final SectMapper sectMapper;
    private final SectMemberMapper sectMemberMapper;
    private final SectStockMapper sectStockMapper;
    private final SectStockLogMapper sectStockLogMapper;
    private final ConsignMapper consignMapper;
    private final UserBagMapper userBagMapper;
    private final ItemMapper itemMapper;
    private final GradeMapper gradeMapper;
    private final UserMapper userMapper;
    private final SpiritWalletService wallet;
    private final EventService eventService;

    public TreasureServiceImpl(SectMapper sectMapper,
                               SectMemberMapper sectMemberMapper,
                               SectStockMapper sectStockMapper,
                               SectStockLogMapper sectStockLogMapper,
                               ConsignMapper consignMapper,
                               UserBagMapper userBagMapper,
                               ItemMapper itemMapper,
                               GradeMapper gradeMapper,
                               UserMapper userMapper,
                               SpiritWalletService wallet,
                               EventService eventService) {
        this.sectMapper = sectMapper;
        this.sectMemberMapper = sectMemberMapper;
        this.sectStockMapper = sectStockMapper;
        this.sectStockLogMapper = sectStockLogMapper;
        this.consignMapper = consignMapper;
        this.userBagMapper = userBagMapper;
        this.itemMapper = itemMapper;
        this.gradeMapper = gradeMapper;
        this.userMapper = userMapper;
        this.wallet = wallet;
        this.eventService = eventService;
    }

    /* ------------------------------ 宗门宝库 ------------------------------ */
    @Override
    public Map<String, Object> vault(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        SectMember me = myMember(userId);
        Map<String, Object> out = new LinkedHashMap<>();
        if (me == null) {
            out.put("hasSect", false);
            out.put("tip", "尚无山门，先立宗或投一门下，方可入宗库");
            return out;
        }
        Sect sect = sectMapper.selectById(me.getSectId());
        ensureStock(me.getSectId());

        List<SectStock> stocks = sectStockMapper.selectList(Wrappers.<SectStock>lambdaQuery()
                .eq(SectStock::getSectId, me.getSectId())
                .eq(SectStock::getStatus, 1)
                .orderByAsc(SectStock::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (SectStock s : stocks) {
            Item it = itemMapper.selectById(s.getItemId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", s.getId());
            m.put("itemId", s.getItemId());
            m.put("title", s.getItemTitle());
            m.put("count", s.getCount());
            m.put("contributionPrice", s.getContributionPrice());
            m.put("detail", it == null ? "" : nullToEmpty(it.getDetail()));
            m.put("acquire", it == null ? "宗门宝库贡献兑换" : nullToEmpty(it.getAcquire()));
            m.put("effect", it == null ? "" : nullToEmpty(it.getEffect()));
            m.put("grade", it == null ? "" : nullToEmpty(it.getGradeKey()));
            m.put("gradeLabel", gradeName(it == null ? null : it.getGradeKey()));
            m.put("gradePrice", it == null || it.getGradePrice() == null ? 0 : it.getGradePrice());
            m.put("canRedeem", s.getCount() != null && s.getCount() > 0
                    && (me.getContribution() == null ? 0 : me.getContribution()) >= s.getContributionPrice());
            list.add(m);
        }
        out.put("hasSect", true);
        out.put("sectName", sect == null ? "" : sect.getName());
        out.put("myContribution", me.getContribution() == null ? 0 : me.getContribution());
        out.put("myRole", me.getRole());
        out.put("stocks", list);
        out.put("balance", wallet.balance(userId));
        return out;
    }

    /** 新宗门首次开库时铺一批基础藏品，避免宗库空空 */
    private void ensureStock(Long sectId) {
        Long cnt = sectStockMapper.selectCount(Wrappers.<SectStock>lambdaQuery()
                .eq(SectStock::getSectId, sectId));
        if (cnt != null && cnt > 0) return;
        List<Item> items = itemMapper.selectList(Wrappers.<Item>lambdaQuery()
                .eq(Item::getStatus, 1).orderByAsc(Item::getId).last("limit " + INIT_STOCK_KINDS));
        for (Item it : items) {
            SectStock s = new SectStock();
            s.setSectId(sectId);
            s.setItemId(it.getId());
            s.setItemTitle(it.getTitle());
            s.setCount(5);
            s.setContributionPrice(Math.max(10, gradeRank(it.getGradeKey()) * 20));
            s.setStatus(1);
            s.setCreateTime(LocalDateTime.now());
            sectStockMapper.insert(s);
        }
    }

    @Override
    public Map<String, Object> deposit(String authorization, Long itemId, Integer count, Integer contributionPrice) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        SectMember me = requireMember(userId);
        Item it = requireItem(itemId);
        int c = count == null || count < 1 ? 1 : Math.min(count, 99);
        if (!removeItem(userId, itemId, c)) {
            throw new BizException("背包里没有足够的「" + it.getTitle() + "」");
        }
        int price = contributionPrice == null
                ? Math.max(10, (it.getGradePrice() == null ? 30 : it.getGradePrice()) / 10)
                : Math.max(1, Math.min(contributionPrice, 5000));

        SectStock stock = sectStockMapper.selectOne(Wrappers.<SectStock>lambdaQuery()
                .eq(SectStock::getSectId, me.getSectId()).eq(SectStock::getItemId, itemId));
        if (stock == null) {
            stock = new SectStock();
            stock.setSectId(me.getSectId());
            stock.setItemId(itemId);
            stock.setItemTitle(it.getTitle());
            stock.setCount(c);
            stock.setContributionPrice(price);
            stock.setStatus(1);
            stock.setCreateTime(LocalDateTime.now());
            sectStockMapper.insert(stock);
        } else {
            stock.setCount((stock.getCount() == null ? 0 : stock.getCount()) + c);
            if (contributionPrice != null) stock.setContributionPrice(price);
            stock.setStatus(1);
            sectStockMapper.updateById(stock);
        }
        // 存入宗库也记一点贡献，鼓励成员互通有无
        int reward = Math.max(2, c * price / 4);
        me.setContribution((me.getContribution() == null ? 0 : me.getContribution()) + reward);
        sectMemberMapper.updateById(me);
        writeLog(me.getSectId(), userId, (String) u[1], it.getTitle(), reward, "deposit");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("itemTitle", it.getTitle());
        out.put("count", c);
        out.put("contributionPrice", price);
        out.put("rewardContribution", reward);
        out.put("myContribution", me.getContribution());
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> redeem(String authorization, Long stockId, Integer count) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        SectMember me = requireMember(userId);
        if (stockId == null) throw new BizException("未指定库中藏品");
        int c = count == null || count < 1 ? 1 : Math.min(count, 20);
        SectStock stock = sectStockMapper.selectById(stockId);
        if (stock == null || !Objects.equals(stock.getSectId(), me.getSectId())
                || stock.getStatus() == null || stock.getStatus() != 1) {
            throw new BizException("此物不在你宗门库中");
        }
        int total = stock.getContributionPrice() * c;
        int mine = me.getContribution() == null ? 0 : me.getContribution();
        if (mine < total) throw new BizException("贡献点不足，尚缺 " + (total - mine) + " 点");
        // 条件更新防超兑
        int rows = sectStockMapper.update(null, Wrappers.<SectStock>lambdaUpdate()
                .eq(SectStock::getId, stockId).ge(SectStock::getCount, c)
                .setSql("count = count - " + c));
        if (rows == 0) throw new BizException("库中此物已被同门先取");
        me.setContribution(mine - total);
        sectMemberMapper.updateById(me);
        addItem(userId, stock.getItemId(), c);
        writeLog(me.getSectId(), userId, (String) u[1], stock.getItemTitle(), -total, "redeem");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("itemTitle", stock.getItemTitle());
        out.put("count", c);
        out.put("costContribution", total);
        out.put("myContribution", me.getContribution());
        return out;
    }

    /* ------------------------------ 山友寄售 ------------------------------ */
    @Override
    public List<Map<String, Object>> consigns(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        List<Consign> rows = consignMapper.selectList(Wrappers.<Consign>lambdaQuery()
                .eq(Consign::getStatus, 0).orderByDesc(Consign::getId).last("limit 100"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Consign c : rows) out.add(consignView(c, userId));
        return out;
    }

    @Override
    public List<Map<String, Object>> myConsigns(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        List<Consign> rows = consignMapper.selectList(Wrappers.<Consign>lambdaQuery()
                .eq(Consign::getSellerId, userId).orderByDesc(Consign::getId).last("limit 50"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Consign c : rows) out.add(consignView(c, userId));
        return out;
    }

    @Override
    public Map<String, Object> publish(String authorization, Long itemId, Integer count,
                                       Integer priceSpirit, Long barterItemId, String barterTitle, Integer mode) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        Item it = requireItem(itemId);
        int c = count == null || count < 1 ? 1 : Math.min(count, 99);
        int m = mode == null ? 0 : Math.max(0, Math.min(mode, 2));
        int price = priceSpirit == null ? Math.max(10, it.getGradePrice() == null ? 30 : it.getGradePrice())
                : Math.max(0, Math.min(priceSpirit, 999999));
        if (m != 1 && price <= 0) throw new BizException("灵石价须大于零");
        if (m != 0 && barterItemId == null && (barterTitle == null || barterTitle.isBlank())) {
            throw new BizException("请写明想换何物");
        }
        if (!removeItem(userId, itemId, c)) {
            throw new BizException("背包里没有足够的「" + it.getTitle() + "」");
        }
        String wantTitle = barterItemId == null ? nullToEmpty(barterTitle) : itemTitle(barterItemId);
        Consign c0 = new Consign();
        c0.setSellerId(userId);
        c0.setSellerName((String) u[1]);
        c0.setItemId(itemId);
        c0.setItemTitle(it.getTitle());
        c0.setCount(c);
        c0.setPriceSpirit(price);
        c0.setBarterItemId(barterItemId);
        c0.setBarterTitle(wantTitle);
        c0.setMode(m);
        c0.setStatus(0);
        c0.setCreateTime(LocalDateTime.now());
        consignMapper.insert(c0);
        eventService.record("山友 " + u[1] + " 于宝库寄售「" + it.getTitle() + "」×" + c + "。");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("id", c0.getId());
        out.put("itemTitle", it.getTitle());
        out.put("mode", m);
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> buy(String authorization, Long consignId) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        Consign c = requireConsign(consignId);
        if (Objects.equals(c.getSellerId(), userId)) throw new BizException("不可买自己的挂单");
        if (c.getMode() != null && c.getMode() == 1) throw new BizException("此物只换不卖，请以物换物");
        int total = c.getPriceSpirit() * c.getCount();
        wallet.deduct(userId, total, "宝库寄售 · 购得 " + c.getItemTitle());
        wallet.add(c.getSellerId(), total, "寄售成交 · " + c.getItemTitle());
        addItem(userId, c.getItemId(), c.getCount());
        c.setStatus(1);
        c.setBuyerId(userId);
        c.setBuyerName((String) u[1]);
        consignMapper.updateById(c);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("itemTitle", c.getItemTitle());
        out.put("count", c.getCount());
        out.put("spent", total);
        out.put("balance", wallet.balance(userId));
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> barter(String authorization, Long consignId, Long offerItemId) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        Consign c = requireConsign(consignId);
        if (Objects.equals(c.getSellerId(), userId)) throw new BizException("不可与自己交换");
        if (c.getMode() != null && c.getMode() == 0) throw new BizException("此物只售不换，请用灵石购买");
        if (offerItemId == null) throw new BizException("请选一件自己背包里的物品作交换");
        if (c.getBarterItemId() != null && !Objects.equals(c.getBarterItemId(), offerItemId)) {
            throw new BizException("对方指明要换「" + c.getBarterTitle() + "」");
        }
        if (!removeItem(userId, offerItemId, 1)) {
            throw new BizException("背包里没有「" + itemTitle(offerItemId) + "」");
        }
        // 交换：卖家得所换之物，买家得挂单之物
        addItem(c.getSellerId(), offerItemId, 1);
        addItem(userId, c.getItemId(), c.getCount());
        c.setStatus(1);
        c.setBuyerId(userId);
        c.setBuyerName((String) u[1]);
        consignMapper.updateById(c);
        eventService.record("宝库以物换物：" + u[1] + " 以「" + itemTitle(offerItemId)
                + "」换得 " + c.getSellerName() + " 的「" + c.getItemTitle() + "」。");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("itemTitle", c.getItemTitle());
        out.put("offerTitle", itemTitle(offerItemId));
        return out;
    }

    @Override
    public Map<String, Object> cancel(String authorization, Long consignId) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        Consign c = requireConsign(consignId);
        if (!Objects.equals(c.getSellerId(), userId)) throw new BizException("只能撤自己的挂单");
        addItem(userId, c.getItemId(), c.getCount());
        c.setStatus(2);
        consignMapper.updateById(c);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("itemTitle", c.getItemTitle());
        return out;
    }

    /* ------------------------------ 内部工具 ------------------------------ */
    private Map<String, Object> consignView(Consign c, Long myId) {
        Item it = itemMapper.selectById(c.getItemId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("sellerId", c.getSellerId());
        m.put("sellerName", c.getSellerName());
        m.put("itemId", c.getItemId());
        m.put("itemTitle", c.getItemTitle());
        m.put("count", c.getCount());
        m.put("priceSpirit", c.getPriceSpirit());
        m.put("barterItemId", c.getBarterItemId());
        m.put("barterTitle", c.getBarterTitle());
        m.put("mode", c.getMode());
        m.put("status", c.getStatus());
        m.put("mine", Objects.equals(c.getSellerId(), myId));
        m.put("detail", it == null ? "" : nullToEmpty(it.getDetail()));
        m.put("acquire", it == null ? "" : nullToEmpty(it.getAcquire()));
        m.put("effect", it == null ? "" : nullToEmpty(it.getEffect()));
        m.put("gradeLabel", gradeName(it == null ? null : it.getGradeKey()));
        m.put("gradePrice", it == null || it.getGradePrice() == null ? 0 : it.getGradePrice());
        m.put("time", c.getCreateTime() == null ? "" : c.getCreateTime().toString());
        return m;
    }

    private Consign requireConsign(Long id) {
        if (id == null) throw new BizException("未指定挂单");
        Consign c = consignMapper.selectById(id);
        if (c == null || c.getStatus() == null || c.getStatus() != 0) {
            throw new BizException("此挂单已撤或已成交");
        }
        return c;
    }

    private SectMember requireMember(Long userId) {
        SectMember me = myMember(userId);
        if (me == null) throw new BizException("尚无山门，先立宗或投一门下");
        return me;
    }

    private SectMember myMember(Long userId) {
        return sectMemberMapper.selectOne(Wrappers.<SectMember>lambdaQuery()
                .eq(SectMember::getUserId, userId).last("limit 1"));
    }

    private Item requireItem(Long itemId) {
        if (itemId == null) throw new BizException("未指定物品");
        Item it = itemMapper.selectById(itemId);
        if (it == null || it.getStatus() == null || it.getStatus() != 1) {
            throw new BizException("查无此物");
        }
        return it;
    }

    private String itemTitle(Long itemId) {
        if (itemId == null) return "未知之物";
        Item it = itemMapper.selectById(itemId);
        return it == null ? "未知之物" : it.getTitle();
    }

    private int gradeRank(String key) {
        if (key == null) return 1;
        Grade g = gradeMapper.selectOne(Wrappers.<Grade>lambdaQuery().eq(Grade::getGradeKey, key));
        return g == null || g.getGradeRank() == null ? 1 : g.getGradeRank();
    }

    private String gradeName(String key) {
        if (key == null) return "凡品";
        Grade g = gradeMapper.selectOne(Wrappers.<Grade>lambdaQuery().eq(Grade::getGradeKey, key));
        return g == null ? "凡品" : g.getName();
    }

    private void writeLog(Long sectId, Long userId, String username,
                          String itemTitle, int amount, String action) {
        try {
            SectStockLog l = new SectStockLog();
            l.setSectId(sectId);
            l.setUserId(userId);
            l.setUsername(username);
            l.setItemTitle(itemTitle);
            l.setAmount(amount);
            l.setAction(action);
            l.setCreateTime(LocalDateTime.now());
            sectStockLogMapper.insert(l);
        } catch (Exception ignore) {
            // 流水失败不影响成交
        }
    }

    private void addItem(Long userId, Long itemId, int count) {
        if (count <= 0 || itemId == null) return;
        UserBag bag = userBagMapper.selectOne(Wrappers.<UserBag>lambdaQuery()
                .eq(UserBag::getUserId, userId).eq(UserBag::getItemId, itemId));
        if (bag == null) {
            UserBag nb = new UserBag();
            nb.setUserId(userId);
            nb.setItemId(itemId);
            nb.setCount(count);
            nb.setCreateTime(LocalDateTime.now());
            nb.setUpdateTime(LocalDateTime.now());
            userBagMapper.insert(nb);
        } else {
            bag.setCount((bag.getCount() == null ? 0 : bag.getCount()) + count);
            bag.setUpdateTime(LocalDateTime.now());
            userBagMapper.updateById(bag);
        }
    }

    private boolean removeItem(Long userId, Long itemId, int count) {
        if (count <= 0) return true;
        int rows = userBagMapper.update(null, Wrappers.<UserBag>lambdaUpdate()
                .eq(UserBag::getUserId, userId).eq(UserBag::getItemId, itemId)
                .ge(UserBag::getCount, count)
                .setSql("count = count - " + count));
        return rows > 0;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private Object[] requireUser(String authorization) {
        String t = authorization;
        if (t != null && t.startsWith("Bearer ")) t = t.substring(7);
        Object[] parsed = TokenUtil.parse(t);
        if (parsed == null) throw new BizException("令牌无效，请重新入境");
        User user = userMapper.selectById((Long) parsed[0]);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BizException("查无此山籍，请重新入境");
        }
        return parsed;
    }
}
