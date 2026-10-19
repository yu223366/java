package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.controller.dto.*;
import com.fenglin.springboottest.entity.*;
import com.fenglin.springboottest.mapper.*;
import com.fenglin.springboottest.service.HomeService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 首页业务实现：导航 / 热词 / 公告 / 四时图卷 / 论道榜 / 分类 / 品阶 / 组合搜索。
 * 搜索支持分级分类：分类按一级含二级自动下钻，品阶支持精确与「该品及以上」。
 */
@Service
public class HomeServiceImpl implements HomeService {

    private static final int MAX_PAGE_SIZE = 50;

    private final HomeNavMapper homeNavMapper;
    private final HotKeywordMapper hotKeywordMapper;
    private final AnnouncementMapper announcementMapper;
    private final SceneMapper sceneMapper;
    private final DiscourseMapper discourseMapper;
    private final CategoryMapper categoryMapper;
    private final GradeMapper gradeMapper;
    private final ItemMapper itemMapper;
    private final SearchLogMapper searchLogMapper;
    private final UserMapper userMapper;
    private final MarketItemMapper marketItemMapper;
    private final TradeLogMapper tradeLogMapper;

    public HomeServiceImpl(HomeNavMapper homeNavMapper,
                           HotKeywordMapper hotKeywordMapper,
                           AnnouncementMapper announcementMapper,
                           SceneMapper sceneMapper,
                           DiscourseMapper discourseMapper,
                           CategoryMapper categoryMapper,
                           GradeMapper gradeMapper,
                           ItemMapper itemMapper,
                           SearchLogMapper searchLogMapper,
                           UserMapper userMapper,
                           MarketItemMapper marketItemMapper,
                           TradeLogMapper tradeLogMapper) {
        this.homeNavMapper = homeNavMapper;
        this.hotKeywordMapper = hotKeywordMapper;
        this.announcementMapper = announcementMapper;
        this.sceneMapper = sceneMapper;
        this.discourseMapper = discourseMapper;
        this.categoryMapper = categoryMapper;
        this.gradeMapper = gradeMapper;
        this.itemMapper = itemMapper;
        this.searchLogMapper = searchLogMapper;
        this.userMapper = userMapper;
        this.marketItemMapper = marketItemMapper;
        this.tradeLogMapper = tradeLogMapper;
    }

    /* ------------------------------ 用户信息 ------------------------------ */

    @Override
    public UserInfoVo getUserInfo(String token) {
        // 解析本地令牌 base64(userId:username:ts)
        String t = token;
        if (t != null && t.startsWith("Bearer ")) {
            t = t.substring(7);
        }
        Object[] parsed = com.fenglin.springboottest.util.TokenUtil.parse(t);
        if (parsed == null) {
            throw new BizException("令牌无效，请重新入境");
        }
        Long userId = (Long) parsed[0];
        String username = (String) parsed[1];

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException("查无此山籍，请重新入境");
        }
        UserInfoVo vo = new UserInfoVo();
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getUsername());
        vo.setElement(user.getElement());
        return vo;
    }

    /* ------------------------------ 首页展示 ------------------------------ */

    @Override
    public List<Map<String, Object>> getNav() {
        List<HomeNav> rows = homeNavMapper.selectList(
                Wrappers.<HomeNav>lambdaQuery()
                        .eq(HomeNav::getStatus, 1)
                        .orderByAsc(HomeNav::getSort));
        return rows.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>(4);
            m.put("key", n.getNavKey());
            m.put("label", n.getLabel());
            m.put("en", n.getEn());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getHotKeywords() {
        List<HotKeyword> rows = hotKeywordMapper.selectList(
                Wrappers.<HotKeyword>lambdaQuery()
                        .eq(HotKeyword::getStatus, 1)
                        .orderByAsc(HotKeyword::getSort));
        return rows.stream().map(k -> {
            Map<String, Object> m = new LinkedHashMap<>(4);
            m.put("keyword", k.getKeyword());
            m.put("element", k.getElement());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getAnnouncements() {
        List<Announcement> rows = announcementMapper.selectList(
                Wrappers.<Announcement>lambdaQuery()
                        .eq(Announcement::getStatus, 1)
                        .orderByAsc(Announcement::getSort));
        return rows.stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>(4);
            m.put("id", a.getId());
            m.put("text", a.getText());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getScenes() {
        List<Scene> rows = sceneMapper.selectList(
                Wrappers.<Scene>lambdaQuery()
                        .eq(Scene::getStatus, 1)
                        .orderByAsc(Scene::getSort));
        return rows.stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>(8);
            m.put("key", s.getSceneKey());
            m.put("title", s.getTitle());
            m.put("en", s.getEn());
            m.put("season", s.getSeason());
            m.put("desc", s.getSummary());
            m.put("tint", s.getTint());
            return m;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getDiscourse() {
        List<Discourse> rows = discourseMapper.selectList(
                Wrappers.<Discourse>lambdaQuery()
                        .eq(Discourse::getStatus, 1)
                        .orderByAsc(Discourse::getSort));
        return rows.stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>(8);
            m.put("id", d.getId());
            m.put("name", d.getName());
            m.put("element", d.getElement());
            m.put("quote", d.getQuote());
            m.put("zan", d.getZan());
            return m;
        }).collect(Collectors.toList());
    }

    /* ------------------------------ 分类 / 品阶字典 ------------------------------ */

    @Override
    public List<CategoryVo> getCategories() {
        List<Category> all = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery()
                        .eq(Category::getStatus, 1)
                        .orderByAsc(Category::getSort)
                        .orderByAsc(Category::getId));
        Map<Long, List<Category>> byParent = all.stream()
                .collect(Collectors.groupingBy(Category::getParentId));
        return buildCategoryTree(0L, byParent);
    }

    private List<CategoryVo> buildCategoryTree(Long parentId, Map<Long, List<Category>> byParent) {
        List<Category> children = byParent.getOrDefault(parentId, Collections.emptyList());
        List<CategoryVo> result = new ArrayList<>(children.size());
        for (Category c : children) {
            CategoryVo vo = new CategoryVo();
            vo.setKey(c.getCatKey());
            vo.setLabel(c.getName());
            vo.setEn(c.getEn());
            vo.setChildren(buildCategoryTree(c.getId(), byParent));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<GradeVo> getGrades() {
        List<Grade> rows = gradeMapper.selectList(
                Wrappers.<Grade>lambdaQuery()
                        .eq(Grade::getStatus, 1)
                        .orderByAsc(Grade::getSort));
        return rows.stream().map(g -> {
            GradeVo vo = new GradeVo();
            vo.setKey(g.getGradeKey());
            vo.setLabel(g.getName());
            vo.setRank(g.getGradeRank());
            vo.setColor(g.getColor());
            return vo;
        }).collect(Collectors.toList());
    }

    /* ------------------------------ 搜索 ------------------------------ */

    @Override
    public List<String> suggest(String q) {
        String kw = q == null ? "" : q.trim();
        if (kw.isEmpty()) {
            return Collections.emptyList();
        }
        List<Item> rows = itemMapper.selectList(
                Wrappers.<Item>lambdaQuery()
                        .eq(Item::getStatus, 1)
                        .like(Item::getTitle, kw)
                        .orderByAsc(Item::getSort)
                        .last("LIMIT 6"));
        return rows.stream().map(Item::getTitle).collect(Collectors.toList());
    }

    @Override
    public SearchResultVo search(SearchRequest req, Long userId, String username) {
        String kw = req.getKeyword() == null ? "" : req.getKeyword().trim();

        // 分页参数
        long page = req.getPage() == null || req.getPage() < 1 ? 1 : req.getPage();
        long size = req.getSize() == null || req.getSize() < 1 ? 20
                : Math.min(req.getSize(), MAX_PAGE_SIZE);

        // 分类（分级）：传一级分类自动包含其全部子类
        List<Long> categoryIds = resolveCategoryIds(req.getCategory());

        // 品阶：grade 精确；gradeMin = 该品及以上
        Set<String> gradeKeys = resolveGradeKeys(req.getGrade(), req.getGradeMin());

        // 组合查询
        Page<Item> pager = new Page<>(page, size);
        pager = itemMapper.selectPage(pager, Wrappers.<Item>lambdaQuery()
                .eq(Item::getStatus, 1)
                .eq(isNotBlank(req.getElement()) && !"all".equals(req.getElement()),
                        Item::getElement, blankToNull(req.getElement()))
                .in(gradeKeys != null && !gradeKeys.isEmpty(), Item::getGradeKey, gradeKeys)
                .in(categoryIds != null && !categoryIds.isEmpty(), Item::getCategoryId, categoryIds)
                .and(isNotBlank(kw), w -> w.like(Item::getTitle, kw).or().like(Item::getSummary, kw))
                .orderByAsc(Item::getSort)
                .orderByAsc(Item::getId));

        // 分类 / 品阶字典（用于回填中文标签）
        Map<Long, Category> catMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
        Map<String, Grade> gradeMap = gradeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Grade::getGradeKey, g -> g));

        List<ItemVo> items = pager.getRecords().stream().map(it -> toItemVo(it, catMap, gradeMap))
                .collect(Collectors.toList());

        // 记录搜索日志（异步可优化，当前同步插入即可）
        try {
            SearchLog log = new SearchLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setKeyword(kw);
            log.setElement(blankToNull(req.getElement()));
            log.setGradeKey(blankToNull(gradeKeys != null && gradeKeys.size() == 1
                    ? gradeKeys.iterator().next() : req.getGrade()));
            log.setCategoryId(categoryIds != null && categoryIds.size() == 1
                    ? categoryIds.get(0) : null);
            log.setResultCnt((int) pager.getTotal());
            searchLogMapper.insert(log);
        } catch (Exception ignore) {
            // 日志失败不影响主流程
        }

        return SearchResultVo.of(pager.getTotal(), page, size, items);
    }

    /** 分类 key → id 列表；传父类（如 gongfa）自动包含其子类；all/空 返回 null（不过滤） */
    private List<Long> resolveCategoryIds(String categoryKey) {
        String key = blankToNull(categoryKey);
        if (key == null || "all".equals(key)) {
            return null;
        }
        Category cat = categoryMapper.selectOne(
                Wrappers.<Category>lambdaQuery().eq(Category::getCatKey, key));
        if (cat == null) {
            // 传了不存在的分类 → 明确返回空集（必然无结果）
            return Collections.singletonList(-1L);
        }
        List<Long> ids = new ArrayList<>();
        ids.add(cat.getId());
        // 一级分类下钻二级
        List<Category> children = categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery().eq(Category::getParentId, cat.getId()));
        children.forEach(c -> ids.add(c.getId()));
        return ids;
    }

    /** 品阶筛选：grade 精确；gradeMin = 该品及以上（依赖 wushan_grade.grade_rank） */
    private Set<String> resolveGradeKeys(String grade, String gradeMin) {
        String exact = blankToNull(grade);
        String min = blankToNull(gradeMin);
        if (exact == null && min == null) {
            return null;
        }
        if (exact != null && ("all".equals(exact) || exact.isEmpty())) {
            exact = null;
        }
        if (min != null && "all".equals(min)) {
            min = null;
        }
        if (exact != null) {
            return Set.of(exact);
        }
        // 该品及以上
        Grade g = gradeMapper.selectOne(
                Wrappers.<Grade>lambdaQuery().eq(Grade::getGradeKey, min));
        if (g == null) {
            return Set.of("__none__");
        }
        return gradeMapper.selectList(Wrappers.<Grade>lambdaQuery()
                        .ge(Grade::getGradeRank, g.getGradeRank())).stream()
                .map(Grade::getGradeKey)
                .collect(Collectors.toSet());
    }

    private ItemVo toItemVo(Item it, Map<Long, Category> catMap, Map<String, Grade> gradeMap) {
        ItemVo vo = new ItemVo();
        vo.setId(it.getId());
        vo.setTitle(it.getTitle());
        vo.setDesc(it.getSummary());
        vo.setElement(it.getElement());
        Category cat = catMap.get(it.getCategoryId());
        if (cat != null) {
            vo.setCategory(cat.getCatKey());
            vo.setCategoryLabel(cat.getName());
        }
        Grade g = gradeMap.get(it.getGradeKey());
        if (g != null) {
            vo.setGrade(g.getGradeKey());
            vo.setGradeLabel(g.getName());
        }
        return vo;
    }

    private static boolean isNotBlank(String s) {
        return s != null && !s.isBlank() && !"all".equals(s);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /* ------------------------------ 论道堂（导航「论道」） ------------------------------ */

    @Override
    public PageVo<DiscourseVo> discourseList(Integer page, Integer size) {
        long p = page == null || page < 1 ? 1 : page;
        long s = size == null || size < 1 ? 10 : Math.min(size, MAX_PAGE_SIZE);
        Page<Discourse> pager = discourseMapper.selectPage(new Page<>(p, s),
                Wrappers.<Discourse>lambdaQuery()
                        .eq(Discourse::getStatus, 1)
                        .orderByDesc(Discourse::getZan)
                        .orderByAsc(Discourse::getSort));
        List<DiscourseVo> items = pager.getRecords().stream().map(d -> {
            DiscourseVo vo = new DiscourseVo();
            vo.setId(d.getId());
            vo.setName(d.getName());
            vo.setElement(d.getElement());
            vo.setQuote(d.getQuote());
            vo.setZan(d.getZan());
            return vo;
        }).collect(Collectors.toList());
        return PageVo.of(pager.getTotal(), p, s, items);
    }

    @Override
    public DiscourseVo likeDiscourse(Long id) {
        if (id == null) {
            throw new BizException("未指定语录");
        }
        Discourse d = discourseMapper.selectById(id);
        if (d == null || d.getStatus() == null || d.getStatus() != 1) {
            throw new BizException("此语已不在论道台");
        }
        d.setZan((d.getZan() == null ? 0 : d.getZan()) + 1);
        discourseMapper.updateById(d);
        DiscourseVo vo = new DiscourseVo();
        vo.setId(d.getId());
        vo.setName(d.getName());
        vo.setElement(d.getElement());
        vo.setQuote(d.getQuote());
        vo.setZan(d.getZan());
        return vo;
    }

    @Override
    public DiscourseVo publishDiscourse(String token, String quote) {
        String q = blankToNull(quote);
        if (q == null) {
            throw new BizException("语录不可为空");
        }
        if (q.length() > 200) {
            throw new BizException("语录过长（至多 200 字）");
        }
        String t = token;
        if (t != null && t.startsWith("Bearer ")) {
            t = t.substring(7);
        }
        Object[] parsed = com.fenglin.springboottest.util.TokenUtil.parse(t);
        if (parsed == null) {
            throw new BizException("未登录不可登台论道，请先入境");
        }
        Long userId = (Long) parsed[0];
        String username = (String) parsed[1];
        User user = userMapper.selectById(userId);
        String element = user != null && user.getElement() != null ? user.getElement() : "huo";

        Discourse d = new Discourse();
        d.setName(username);
        d.setElement(element);
        d.setQuote(q);
        d.setZan(0);
        d.setSort(999); // 新语垫底
        d.setStatus(1);
        discourseMapper.insert(d);

        DiscourseVo vo = new DiscourseVo();
        vo.setId(d.getId());
        vo.setName(d.getName());
        vo.setElement(d.getElement());
        vo.setQuote(d.getQuote());
        vo.setZan(d.getZan());
        return vo;
    }

    /* ------------------------------ 市集（导航「市集」） ------------------------------ */

    @Override
    public PageVo<MarketItemVo> marketList(String keyword, String element, String grade,
                                           String category, Integer page, Integer size) {
        long p = page == null || page < 1 ? 1 : page;
        long s = size == null || size < 1 ? 12 : Math.min(size, MAX_PAGE_SIZE);
        String kw = blankToNull(keyword);

        // 复用搜索的分级逻辑：品阶精确匹配 + 分类父类下钻
        Set<String> gradeKeys = resolveGradeKeys(grade, null);
        List<Long> categoryIds = resolveCategoryIds(category);

        Page<MarketItem> pager = marketItemMapper.selectPage(new Page<>(p, s),
                Wrappers.<MarketItem>lambdaQuery()
                        .eq(MarketItem::getStatus, 1)
                        .gt(MarketItem::getStock, 0)
                        .eq(isNotBlank(element) && !"all".equals(element),
                                MarketItem::getElement, blankToNull(element))
                        .in(gradeKeys != null && !gradeKeys.isEmpty(),
                                MarketItem::getGradeKey, gradeKeys)
                        .in(categoryIds != null && !categoryIds.isEmpty(),
                                MarketItem::getCategoryId, categoryIds)
                        .and(kw != null, w -> w.like(MarketItem::getTitle, kw).or().like(MarketItem::getSummary, kw))
                        .orderByAsc(MarketItem::getPrice)
                        .orderByAsc(MarketItem::getId));

        Map<Long, Category> catMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));
        Map<String, Grade> gradeMap = gradeMapper.selectList(null).stream()
                .collect(Collectors.toMap(Grade::getGradeKey, g -> g));

        List<MarketItemVo> items = pager.getRecords().stream()
                .map(m -> toMarketVo(m, catMap, gradeMap))
                .collect(Collectors.toList());
        return PageVo.of(pager.getTotal(), p, s, items);
    }

    @Override
    public MarketItemVo buyMarket(Long id, Integer count, Long userId, String username) {
        if (id == null) {
            throw new BizException("未指定物品");
        }
        int c = count == null || count < 1 ? 1 : Math.min(count, 99);
        MarketItem m = marketItemMapper.selectById(id);
        if (m == null || m.getStatus() == null || m.getStatus() != 1) {
            throw new BizException("此物已下架");
        }
        if (m.getStock() == null || m.getStock() < c) {
            throw new BizException("库存不足，仅余 " + Math.max(m.getStock() == null ? 0 : m.getStock(), 0) + " 件");
        }
        // 条件更新防超卖：stock >= c 才扣
        int rows = marketItemMapper.update(null,
                Wrappers.<MarketItem>lambdaUpdate()
                        .eq(MarketItem::getId, id)
                        .ge(MarketItem::getStock, c)
                        .setSql("stock = stock - " + c)
                        .setSql("sold = sold + " + c));
        if (rows == 0) {
            throw new BizException("手慢一步，此物已被抢空");
        }
        // 交易留痕
        try {
            TradeLog log = new TradeLog();
            log.setItemId(id);
            log.setItemTitle(m.getTitle());
            log.setBuyerId(userId);
            log.setBuyer(username);
            log.setBuyCount(c);
            log.setTotalPrice(m.getPrice() * c);
            tradeLogMapper.insert(log);
        } catch (Exception ignore) {
            // 日志失败不影响成交
        }
        return toMarketVo(marketItemMapper.selectById(id), null, null);
    }

    /** 市集实体 → VO（catMap / gradeMap 允许为空，为空时按需单查） */
    private MarketItemVo toMarketVo(MarketItem m,
                                    Map<Long, Category> catMap,
                                    Map<String, Grade> gradeMap) {
        MarketItemVo vo = new MarketItemVo();
        vo.setId(m.getId());
        vo.setTitle(m.getTitle());
        vo.setDesc(m.getSummary());
        vo.setElement(m.getElement());
        vo.setPrice(m.getPrice());
        vo.setStock(m.getStock());
        vo.setSeller(m.getSeller());
        vo.setSold(m.getSold());
        if (m.getGradeKey() != null) {
            vo.setGrade(m.getGradeKey());
            Grade g = gradeMap != null ? gradeMap.get(m.getGradeKey())
                    : gradeMapper.selectOne(Wrappers.<Grade>lambdaQuery().eq(Grade::getGradeKey, m.getGradeKey()));
            if (g != null) {
                vo.setGradeLabel(g.getName());
            }
        }
        if (m.getCategoryId() != null) {
            Category cat = catMap != null ? catMap.get(m.getCategoryId())
                    : categoryMapper.selectById(m.getCategoryId());
            if (cat != null) {
                vo.setCategoryLabel(cat.getName());
            }
        }
        return vo;
    }
}
