package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.*;
import com.fenglin.springboottest.mapper.*;
import com.fenglin.springboottest.service.MarketService;
import com.fenglin.springboottest.service.SpiritWalletService;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 市集实现：三铺分类 + 品质定价 + 实名购买。
 *
 * <p>品质基准价：凡 10 / 黄 30 / 玄 100 / 地 300 / 天 900 / 圣 2700 灵石，
 * 摊主可在此基准上下浮动；购得的物品以 item_ref 关联 wushan_item 直接进背包，
 * 供炼丹、炼器、布阵消耗。</p>
 */
@Service
public class MarketServiceImpl implements MarketService {

    private static final int MAX_PAGE_SIZE = 50;

    /** 摊位顺序与文案，与 db/upgrade-v2-items.sql 的 shop_key 一致 */
    private static final String[][] SHOPS = {
            {"all", "全 部 摊 位", "雾山市集 · 各铺杂陈"},
            {"dan_cai", "丹 药 材 料 铺", "药庐自采自炮 · 炼丹主材"},
            {"qi_cai", "炼 器 材 料 铺", "金铁晶砂 · 炼器之骨"},
            {"zhen_cai", "阵 法 材 料 铺", "阵纹墨砂 · 布阵之基"},
            {"danyao", "丹 药 铺", "成丹现售 · 按品阶论价"},
            {"faqi", "法 器 铺", "兵刃法宝 · 器成待主"},
            {"zahuo", "杂 货 铺", "山货零物 · 童叟无欺"}
    };

    private final MarketItemMapper marketItemMapper;
    private final CategoryMapper categoryMapper;
    private final GradeMapper gradeMapper;
    private final ItemMapper itemMapper;
    private final UserBagMapper userBagMapper;
    private final TradeLogMapper tradeLogMapper;
    private final UserMapper userMapper;
    private final SpiritWalletService wallet;

    public MarketServiceImpl(MarketItemMapper marketItemMapper,
                             CategoryMapper categoryMapper,
                             GradeMapper gradeMapper,
                             ItemMapper itemMapper,
                             UserBagMapper userBagMapper,
                             TradeLogMapper tradeLogMapper,
                             UserMapper userMapper,
                             SpiritWalletService wallet) {
        this.marketItemMapper = marketItemMapper;
        this.categoryMapper = categoryMapper;
        this.gradeMapper = gradeMapper;
        this.itemMapper = itemMapper;
        this.userBagMapper = userBagMapper;
        this.tradeLogMapper = tradeLogMapper;
        this.userMapper = userMapper;
        this.wallet = wallet;
    }

    @Override
    public List<Map<String, Object>> shops() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (String[] s : SHOPS) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", s[0]);
            m.put("label", s[1]);
            m.put("desc", s[2]);
            Long cnt = "all".equals(s[0])
                    ? marketItemMapper.selectCount(Wrappers.<MarketItem>lambdaQuery().eq(MarketItem::getStatus, 1))
                    : marketItemMapper.selectCount(Wrappers.<MarketItem>lambdaQuery()
                            .eq(MarketItem::getStatus, 1).eq(MarketItem::getShopKey, s[0]));
            m.put("count", cnt == null ? 0 : cnt.intValue());
            out.add(m);
        }
        return out;
    }

    @Override
    public Map<String, Object> items(String keyword, String element, String grade, String shop,
                                     String category, Integer page, Integer size) {
        long p = page == null || page < 1 ? 1 : page;
        long s = size == null || size < 1 ? 12 : Math.min(size, MAX_PAGE_SIZE);
        String kw = blankToNull(keyword);
        String shopKey = blankToNull(shop);
        String gradeKey = blankToNull(grade);
        String el = blankToNull(element);
        String catKey = blankToNull(category);

        // 声明为 final，lambda 内只 add 不重新赋值（Java 要求 effectively final）
        final List<Long> categoryIds = new ArrayList<>();
        if (catKey != null && !"all".equals(catKey)) {
            Category cat = categoryMapper.selectOne(Wrappers.<Category>lambdaQuery().eq(Category::getCatKey, catKey));
            if (cat == null) categoryIds.add(-1L);
            else {
                categoryIds.add(cat.getId());
                categoryMapper.selectList(Wrappers.<Category>lambdaQuery().eq(Category::getParentId, cat.getId()))
                        .forEach(c -> categoryIds.add(c.getId()));
            }
        }

        Page<MarketItem> pager = marketItemMapper.selectPage(new Page<>(p, s),
                Wrappers.<MarketItem>lambdaQuery()
                        .eq(MarketItem::getStatus, 1)
                        .eq(shopKey != null && !"all".equals(shopKey), MarketItem::getShopKey, shopKey)
                        .eq(el != null && !"all".equals(el), MarketItem::getElement, el)
                        .eq(gradeKey != null && !"all".equals(gradeKey), MarketItem::getGradeKey, gradeKey)
                        .in(!categoryIds.isEmpty(), MarketItem::getCategoryId, categoryIds)
                        .and(kw != null, w -> w.like(MarketItem::getTitle, kw).or().like(MarketItem::getSummary, kw))
                        .orderByAsc(MarketItem::getGradeKey)
                        .orderByAsc(MarketItem::getPrice)
                        .orderByAsc(MarketItem::getId));

        List<Map<String, Object>> list = new ArrayList<>();
        for (MarketItem m : pager.getRecords()) list.add(view(m, true));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", pager.getTotal());
        out.put("page", p);
        out.put("size", s);
        out.put("items", list);
        return out;
    }

    @Override
    public Map<String, Object> detail(Long id) {
        if (id == null) throw new BizException("未指定物品");
        MarketItem m = marketItemMapper.selectById(id);
        if (m == null) throw new BizException("查无此物");
        Map<String, Object> out = view(m, false);
        out.put("shopLabel", shopLabel(m.getShopKey()));
        out.put("qualityRule", "按品阶定价：凡品 10 · 黄品 30 · 玄品 100 · 地品 300 · 天品 900 · 圣品 2700 灵石");
        return out;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> buy(String authorization, Long id, Integer count) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        String username = (String) u[1];
        if (id == null) throw new BizException("未指定物品");
        int c = count == null || count < 1 ? 1 : Math.min(count, 99);
        MarketItem m = marketItemMapper.selectById(id);
        if (m == null || m.getStatus() == null || m.getStatus() != 1) throw new BizException("此物已下架");
        int total = (m.getPrice() == null ? 0 : m.getPrice()) * c;
        // 条件更新防超卖；先扣库存再扣灵石，扣灵石失败会回滚库存
        int rows = marketItemMapper.update(null, Wrappers.<MarketItem>lambdaUpdate()
                .eq(MarketItem::getId, id).ge(MarketItem::getStock, c)
                .setSql("stock = stock - " + c).setSql("sold = sold + " + c));
        if (rows == 0) throw new BizException("手慢一步，此物已被抢空");
        wallet.deduct(userId, total, "市集购得 · " + m.getTitle() + " ×" + c);

        // 购得之物入背包：优先用 item_ref，找不到就按同名宝库条目兜底
        Long ref = m.getItemRef();
        if (ref == null) {
            Item same = itemMapper.selectOne(Wrappers.<Item>lambdaQuery()
                    .eq(Item::getTitle, m.getTitle()).last("limit 1"));
            ref = same == null ? null : same.getId();
        }
        if (ref != null) addItem(userId, ref, c);

        try {
            TradeLog log = new TradeLog();
            log.setItemId(id);
            log.setItemTitle(m.getTitle());
            log.setBuyerId(userId);
            log.setBuyer(username);
            log.setBuyCount(c);
            log.setTotalPrice(total);
            tradeLogMapper.insert(log);
        } catch (Exception ignore) {
            // 日志失败不影响成交
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("title", m.getTitle());
        out.put("count", c);
        out.put("spent", total);
        out.put("balance", wallet.balance(userId));
        out.put("itemRef", ref);
        out.put("stock", Math.max((m.getStock() == null ? 0 : m.getStock()) - c, 0));
        out.put("sold", (m.getSold() == null ? 0 : m.getSold()) + c);
        out.put("shopLabel", shopLabel(m.getShopKey()));
        return out;
    }

    /* ------------------------------ 内部工具 ------------------------------ */
    private Map<String, Object> view(MarketItem m, boolean brief) {
        Map<String, Object> vo = new LinkedHashMap<>();
        vo.put("id", m.getId());
        vo.put("title", m.getTitle());
        vo.put("desc", nullToEmpty(m.getSummary()));
        vo.put("element", m.getElement());
        vo.put("price", m.getPrice());
        vo.put("stock", m.getStock());
        vo.put("seller", m.getSeller());
        vo.put("sold", m.getSold());
        vo.put("shopKey", m.getShopKey());
        vo.put("shopLabel", shopLabel(m.getShopKey()));
        vo.put("itemRef", m.getItemRef());
        Grade g = gradeMapper.selectOne(Wrappers.<Grade>lambdaQuery().eq(Grade::getGradeKey, m.getGradeKey()));
        vo.put("grade", m.getGradeKey());
        vo.put("gradeLabel", g == null ? "凡品" : g.getName());
        vo.put("gradePrice", m.getGradePrice() == null ? 0 : m.getGradePrice());
        vo.put("acquire", nullToEmpty(m.getAcquire()));
        // 列表只给摘要，详情接口给全文
        String detail = nullToEmpty(m.getDetail());
        if (brief && detail.length() > 120) detail = detail.substring(0, 120) + "…";
        vo.put("detail", detail.isEmpty() ? nullToEmpty(m.getSummary()) : detail);
        if (!brief) {
            Category cat = m.getCategoryId() == null ? null : categoryMapper.selectById(m.getCategoryId());
            vo.put("categoryLabel", cat == null ? "" : cat.getName());
            Item it = m.getItemRef() == null ? null : itemMapper.selectById(m.getItemRef());
            vo.put("effect", it == null ? "购得入背包，可作炼丹 / 炼器 / 布阵之用" : nullToEmpty(it.getEffect()));
            vo.put("itemAcquire", it == null ? "" : nullToEmpty(it.getAcquire()));
        }
        return vo;
    }

    private String shopLabel(String key) {
        for (String[] s : SHOPS) if (Objects.equals(s[0], key)) return s[1];
        return "杂 货 铺";
    }

    private void addItem(Long userId, Long itemId, int count) {
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

    private static String blankToNull(String s) {
        return s == null || s.isBlank() || "all".equals(s.trim()) ? null : s.trim();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private Object[] requireUser(String authorization) {
        String t = authorization;
        if (t != null && t.startsWith("Bearer ")) t = t.substring(7);
        Object[] parsed = TokenUtil.parse(t);
        if (parsed == null) throw new BizException("请先入境，方可在市集交易");
        User user = userMapper.selectById((Long) parsed[0]);
        if (user == null || (user.getStatus() != null && user.getStatus() == 0)) {
            throw new BizException("查无此山籍，请重新入境");
        }
        return parsed;
    }
}
