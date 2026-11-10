package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.*;
import com.fenglin.springboottest.mapper.*;
import com.fenglin.springboottest.service.SpiritWalletService;
import com.fenglin.springboottest.service.WorldService;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 山门世界玩法实现：背包 / 秘境 / 炼丹 / 对决 / 私信。
 *
 * <p>秘境探索用内存会话保存当前进度（每个山友一次只允许一段未完的探索），
 * 结算后才落库；背包变更一律走条件更新，避免材料被重复扣减。</p>
 */
@Service
public class WorldServiceImpl implements WorldService {

    /** 进入秘境一次的消耗（灵石） */
    private static final int EXPLORE_COST = 10;
    /** 一次秘境探索共几段 */
    private static final int EXPLORE_STEPS = 3;
    /** 对决题目数 */
    private static final int DUEL_QUESTIONS = 5;
    /** 炼材类分类 id，秘境掉落从这里出 */
    private static final long MATERIAL_CATEGORY = 9L;

    /* ------------------------------ 事件库 ------------------------------ */
    private static final class Choice {
        final String text;
        final String hint;
        final int spirit;
        final int itemCount;

        Choice(String text, String hint, int spirit, int itemCount) {
            this.text = text;
            this.hint = hint;
            this.spirit = spirit;
            this.itemCount = itemCount;
        }
    }

    private static final class Node {
        final String title;
        final String desc;
        final List<Choice> choices;

        Node(String title, String desc, List<Choice> choices) {
            this.title = title;
            this.desc = desc;
            this.choices = choices;
        }
    }

    private static final List<Node> NODES = List.of(
            new Node("遗 府", "前殿石台上落着数件旧物，蒙尘已久。", List.of(
                    new Choice("尽数取走", "或有所得", 5, 2),
                    new Choice("只取其一", "稳妥为上", 0, 1))),
            new Node("险 径", "甬道尽头传来兽吼，暗处有物在动。", List.of(
                    new Choice("拔剑迎战", "胜则有所得", 15, 1),
                    new Choice("屏息绕行", "留得此身在", -5, 0))),
            new Node("灵 泉", "一线灵泉自石缝涌出，水气氤氲。", List.of(
                    new Choice("汲取灵泉", "身心俱畅", 20, 0),
                    new Choice("洗炼肉身", "或有所获", 8, 1))),
            new Node("石 室", "石室四壁刻着一段残缺功法。", List.of(
                    new Choice("静心参悟", "水到渠成", 25, 0),
                    new Choice("拓印碑文", "留作日后", 6, 2)))
    );

    /** 每个山友的探索会话 */
    private static final class Session {
        Long realmId;
        String realmName;
        int step;
        List<String> path = new ArrayList<>();
        int spirit;
        Map<Long, Integer> items = new LinkedHashMap<>();
    }

    private final Map<Long, Session> sessions = new ConcurrentHashMap<>();

    private final UserBagMapper userBagMapper;
    private final ExploreLogMapper exploreLogMapper;
    private final RecipeMapper recipeMapper;
    private final DuelMapper duelMapper;
    private final MessageMapper messageMapper;
    private final ItemMapper itemMapper;
    private final RealmMapper realmMapper;
    private final CategoryMapper categoryMapper;
    private final GradeMapper gradeMapper;
    private final UserMapper userMapper;
    private final OnlineUserMapper onlineUserMapper;
    private final SpiritWalletService wallet;
    private final ObjectMapper objectMapper;

    public WorldServiceImpl(UserBagMapper userBagMapper,
                            ExploreLogMapper exploreLogMapper,
                            RecipeMapper recipeMapper,
                            DuelMapper duelMapper,
                            MessageMapper messageMapper,
                            ItemMapper itemMapper,
                            RealmMapper realmMapper,
                            CategoryMapper categoryMapper,
                            GradeMapper gradeMapper,
                            UserMapper userMapper,
                            OnlineUserMapper onlineUserMapper,
                            SpiritWalletService wallet,
                            ObjectMapper objectMapper) {
        this.userBagMapper = userBagMapper;
        this.exploreLogMapper = exploreLogMapper;
        this.recipeMapper = recipeMapper;
        this.duelMapper = duelMapper;
        this.messageMapper = messageMapper;
        this.itemMapper = itemMapper;
        this.realmMapper = realmMapper;
        this.categoryMapper = categoryMapper;
        this.gradeMapper = gradeMapper;
        this.userMapper = userMapper;
        this.onlineUserMapper = onlineUserMapper;
        this.wallet = wallet;
        this.objectMapper = objectMapper;
    }

    /* ------------------------------ 背包 ------------------------------ */
    @Override
    public Map<String, Object> bag(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        List<UserBag> bags = userBagMapper.selectList(Wrappers.<UserBag>lambdaQuery()
                .eq(UserBag::getUserId, userId).orderByAsc(UserBag::getItemId));
        Map<Long, Item> itemMap = itemMap();
        Map<String, String> gradeNames = gradeNameMap();
        List<Map<String, Object>> items = new ArrayList<>();
        for (UserBag b : bags) {
            Item it = itemMap.get(b.getItemId());
            if (it == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("itemId", b.getItemId());
            m.put("title", it.getTitle());
            m.put("count", b.getCount());
            m.put("element", it.getElement());
            m.put("grade", it.getGradeKey());
            m.put("gradeLabel", gradeNames.getOrDefault(it.getGradeKey(), "凡品"));
            m.put("summary", it.getSummary() == null ? "" : it.getSummary());
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("balance", wallet.balance(userId));
        return out;
    }

    /** 背包增加物品 */
    private void addItem(Long userId, Long itemId, int count) {
        if (count <= 0) return;
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

    /** 背包扣物品：材料不足返回 false */
    private boolean removeItem(Long userId, Long itemId, int count) {
        if (count <= 0) return true;
        int rows = userBagMapper.update(null, Wrappers.<UserBag>lambdaUpdate()
                .eq(UserBag::getUserId, userId)
                .eq(UserBag::getItemId, itemId)
                .ge(UserBag::getCount, count)
                .setSql("count = count - " + count));
        return rows > 0;
    }

    private int bagCount(Long userId, Long itemId) {
        UserBag bag = userBagMapper.selectOne(Wrappers.<UserBag>lambdaQuery()
                .eq(UserBag::getUserId, userId).eq(UserBag::getItemId, itemId));
        return bag == null || bag.getCount() == null ? 0 : bag.getCount();
    }

    /* ------------------------------ 秘境 ------------------------------ */
    @Override
    public List<Map<String, Object>> realms() {
        List<Realm> list = realmMapperList();
        Map<String, String> gradeNames = gradeNameMap();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Realm r : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("element", r.getElement());
            m.put("grade", r.getGradeKey());
            m.put("gradeLabel", gradeNames.getOrDefault(r.getGradeKey(), "凡品"));
            m.put("summary", r.getSummary() == null ? "" : r.getSummary());
            m.put("cost", EXPLORE_COST);
            out.add(m);
        }
        return out;
    }

    private List<Realm> realmMapperList() {
        return realmMapper.selectList(Wrappers.<Realm>lambdaQuery()
                .eq(Realm::getStatus, 1).orderByAsc(Realm::getSort));
    }

    @Override
    public Map<String, Object> exploreStart(String authorization, Long realmId) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        Realm realm = realmMapper.selectById(realmId);
        if (realm == null || realm.getStatus() == null || realm.getStatus() != 1) {
            throw new BizException("此地秘境尚未开启");
        }
        if (sessions.containsKey(userId)) {
            throw new BizException("你尚在一处秘境未归，先了结前事");
        }
        wallet.deduct(userId, EXPLORE_COST, "进入秘境 · " + realm.getName());

        Session s = new Session();
        s.realmId = realmId;
        s.realmName = realm.getName();
        s.step = 1;
        sessions.put(userId, s);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("step", s.step);
        out.put("total", EXPLORE_STEPS);
        out.put("cost", EXPLORE_COST);
        out.put("node", nodeJson(randomNode()));
        return out;
    }

    @Override
    public Map<String, Object> exploreChoose(String authorization, Integer choiceIdx) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        Session s = sessions.get(userId);
        if (s == null) {
            throw new BizException("你并未在秘境中");
        }
        Node node = randomNode();
        if (choiceIdx == null || choiceIdx < 0 || choiceIdx >= node.choices.size()) {
            throw new BizException("请选择一个应对");
        }
        Choice c = node.choices.get(choiceIdx);
        s.path.add(node.title + " → " + c.text);
        s.spirit += c.spirit;
        if (c.itemCount > 0) {
            Long itemId = randomMaterialId();
            if (itemId != null) {
                s.items.merge(itemId, c.itemCount, Integer::sum);
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        if (s.step < EXPLORE_STEPS) {
            s.step += 1;
            out.put("done", false);
            out.put("step", s.step);
            out.put("total", EXPLORE_STEPS);
            out.put("node", nodeJson(node));
            out.put("gain", c.spirit);
            out.put("path", s.path);
            return out;
        }

        // 结算：物品入包，灵石入账，落库
        for (Map.Entry<Long, Integer> e : s.items.entrySet()) {
            addItem(userId, e.getKey(), e.getValue());
        }
        if (s.spirit != 0) {
            if (s.spirit > 0) {
                wallet.add(userId, s.spirit, "秘境所得 · " + s.realmName);
            } else {
                try {
                    wallet.deduct(userId, -s.spirit, "秘境损耗 · " + s.realmName);
                } catch (BizException ignored) {
                    // 余额不足时免扣，不阻断结算
                }
            }
        }
        ExploreLog log = new ExploreLog();
        log.setUserId(userId);
        log.setRealmId(s.realmId);
        log.setRealmName(s.realmName);
        log.setPathJson(toJson(s.path));
        log.setRewardsJson(toJson(rewardText(s.items, s.spirit)));
        log.setCostSpirit(EXPLORE_COST);
        log.setCreateTime(LocalDateTime.now());
        exploreLogMapper.insert(log);
        sessions.remove(userId);

        out.put("done", true);
        out.put("step", EXPLORE_STEPS);
        out.put("total", EXPLORE_STEPS);
        out.put("path", s.path);
        out.put("rewards", rewardText(s.items, s.spirit));
        out.put("balance", wallet.balance(userId));
        return out;
    }

    @Override
    public List<Map<String, Object>> exploreLogs(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        List<ExploreLog> list = exploreLogMapper.selectList(Wrappers.<ExploreLog>lambdaQuery()
                .eq(ExploreLog::getUserId, userId).orderByDesc(ExploreLog::getId).last("limit 20"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (ExploreLog l : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("realmName", l.getRealmName());
            m.put("path", l.getPathJson());
            m.put("rewards", l.getRewardsJson());
            m.put("cost", l.getCostSpirit());
            m.put("time", l.getCreateTime() == null ? "" : l.getCreateTime().toString());
            out.add(m);
        }
        return out;
    }

    private Node randomNode() {
        return NODES.get(new Random().nextInt(NODES.size()));
    }

    private Long randomMaterialId() {
        List<Item> mats = itemMapper.selectList(Wrappers.<Item>lambdaQuery()
                .eq(Item::getCategoryId, MATERIAL_CATEGORY).eq(Item::getStatus, 1)
                .orderByAsc(Item::getId).last("limit 60"));
        if (mats.isEmpty()) return null;
        return mats.get(new Random().nextInt(mats.size())).getId();
    }

    private Map<String, Object> nodeJson(Node node) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("title", node.title);
        m.put("desc", node.desc);
        List<Map<String, Object>> choices = new ArrayList<>();
        for (Choice c : node.choices) {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("text", c.text);
            cm.put("hint", c.hint);
            choices.add(cm);
        }
        m.put("choices", choices);
        return m;
    }

    private List<Map<String, Object>> rewardText(Map<Long, Integer> items, int spirit) {
        Map<Long, Item> itemMap = itemMap();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : items.entrySet()) {
            Item it = itemMap.get(e.getKey());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", it == null ? "未知之物" : it.getTitle());
            m.put("count", e.getValue());
            out.add(m);
        }
        if (spirit != 0) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", spirit > 0 ? "灵石" : "灵石损耗");
            m.put("count", Math.abs(spirit));
            out.add(m);
        }
        return out;
    }

    /* ------------------------------ 炼丹炼器 ------------------------------ */
    @Override
    public List<Map<String, Object>> recipes(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        List<Recipe> list = recipeMapper.selectList(Wrappers.<Recipe>lambdaQuery()
                .eq(Recipe::getStatus, 1).orderByAsc(Recipe::getId));
        Map<Long, Item> itemMap = itemMap();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Recipe r : list) {
            Item outItem = itemMap.get(r.getOutputItemId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("name", r.getName());
            m.put("category", r.getCategory());
            m.put("outputItemId", r.getOutputItemId());
            m.put("outputTitle", outItem == null ? "未知之物" : outItem.getTitle());
            m.put("outputCount", r.getOutputCount());
            m.put("costSpirit", r.getCostSpirit());
            m.put("description", r.getDescription() == null ? "" : r.getDescription());
            List<Map<String, Object>> mats = new ArrayList<>();
            boolean enough = true;
            for (Map<String, Object> mat : parseMaterials(r.getMaterialsJson())) {
                Long mid = ((Number) mat.get("itemId")).longValue();
                int need = ((Number) mat.get("count")).intValue();
                int have = bagCount(userId, mid);
                Item it = itemMap.get(mid);
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("itemId", mid);
                mm.put("title", it == null ? "未知之物" : it.getTitle());
                mm.put("need", need);
                mm.put("have", have);
                mats.add(mm);
                if (have < need) enough = false;
            }
            m.put("materials", mats);
            m.put("enough", enough);
            m.put("balance", wallet.balance(userId));
            out.add(m);
        }
        return out;
    }

    @Override
    public Map<String, Object> craft(String authorization, Long recipeId) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        Recipe r = recipeMapper.selectById(recipeId);
        if (r == null || r.getStatus() == null || r.getStatus() != 1) {
            throw new BizException("此方已失传");
        }
        for (Map<String, Object> mat : parseMaterials(r.getMaterialsJson())) {
            Long mid = ((Number) mat.get("itemId")).longValue();
            int need = ((Number) mat.get("count")).intValue();
            if (!removeItem(userId, mid, need)) {
                throw new BizException("材料不足，还缺「" + itemTitle(mid) + "」" + need + " 份");
            }
        }
        wallet.deduct(userId, r.getCostSpirit() == null ? 0 : r.getCostSpirit(), "炼制 · " + r.getName());
        addItem(userId, r.getOutputItemId(), r.getOutputCount() == null ? 1 : r.getOutputCount());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("name", r.getName());
        out.put("outputTitle", itemTitle(r.getOutputItemId()));
        out.put("outputCount", r.getOutputCount());
        out.put("balance", wallet.balance(userId));
        return out;
    }

    /* ------------------------------ 五行问对决 ------------------------------ */
    @Override
    public Map<String, Object> duelStart(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        String username = (String) u[1];
        // 找一位近期在线的其他山友
        List<OnlineUser> onlines = onlineUserMapper.selectList(Wrappers.<OnlineUser>lambdaQuery()
                .ne(OnlineUser::getUserId, userId)
                .ge(OnlineUser::getLastActiveTime, LocalDateTime.now().minusMinutes(10))
                .orderByDesc(OnlineUser::getLastActiveTime).last("limit 1"));
        if (onlines.isEmpty()) {
            throw new BizException("此刻山门内没有他人在线，无人应战");
        }
        OnlineUser op = onlines.get(0);
        List<Map<String, Object>> questions = buildDuelQuestions();
        if (questions.isEmpty()) {
            throw new BizException("题库暂空，稍后再来");
        }
        Duel d = new Duel();
        d.setChallengerId(userId);
        d.setChallengerName(username);
        d.setOpponentId(op.getUserId());
        d.setOpponentName(op.getUsername());
        d.setStatus(0);
        d.setAnswered(0);
        d.setQuestionJson(toJson(questions));
        d.setMyScore(0);
        d.setOpScore(0);
        d.setResult("");
        d.setRewardSpirit(0);
        d.setCreateTime(LocalDateTime.now());
        duelMapper.insert(d);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("duelId", d.getId());
        out.put("opponentName", op.getUsername());
        out.put("questions", publicQuestions(questions));
        out.put("total", questions.size());
        return out;
    }

    @Override
    public Map<String, Object> duelAnswer(String authorization, Long duelId, Integer choiceIdx) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        Duel d = duelMapper.selectById(duelId);
        if (d == null || !Objects.equals(d.getChallengerId(), userId)) {
            throw new BizException("查无此局");
        }
        if (d.getStatus() != null && d.getStatus() == 1) {
            throw new BizException("此局已了");
        }
        List<Map<String, Object>> questions = parseList(d.getQuestionJson());
        // 已答对数与已答题数要分开：答错也必须推进，否则会一直卡在第一题
        int answered = d.getAnswered() == null ? 0 : d.getAnswered();
        int myScore = d.getMyScore() == null ? 0 : d.getMyScore();
        int index = answered;
        if (index >= questions.size()) {
            throw new BizException("题目已答尽");
        }
        Map<String, Object> q = questions.get(index);
        List<?> options = (List<?>) q.get("options");
        Integer answer = ((Number) q.get("answerIdx")).intValue();
        if (choiceIdx == null || choiceIdx < 0 || choiceIdx >= options.size()) {
            throw new BizException("请选择一个答案");
        }
        boolean correct = choiceIdx.equals(answer);
        d.setAnswered(answered + 1);
        d.setMyScore(correct ? myScore + 1 : myScore);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("correct", correct);
        out.put("score", d.getMyScore());
        out.put("total", questions.size());

        if (index + 1 >= questions.size()) {
            // 结算：对手答对数取一个合理随机值
            int opScore = new Random().nextInt(questions.size() + 1);
            d.setOpScore(opScore);
            d.setStatus(1);
            String result;
            int reward;
            if (d.getMyScore() > opScore) {
                result = "win";
                reward = 40;
            } else if (d.getMyScore() < opScore) {
                result = "lose";
                reward = 10;
            } else {
                result = "draw";
                reward = 20;
            }
            d.setResult(result);
            d.setRewardSpirit(reward);
            duelMapper.updateById(d);
            wallet.add(userId, reward, "五行对决 · " + (result.equals("win") ? "胜" : result.equals("lose") ? "负" : "平"));
            out.put("finished", true);
            out.put("result", result);
            out.put("opScore", opScore);
            out.put("reward", reward);
            out.put("balance", wallet.balance(userId));
        } else {
            duelMapper.updateById(d);
            out.put("finished", false);
        }
        return out;
    }

    @Override
    public List<Map<String, Object>> duelRecords(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        List<Duel> list = duelMapper.selectList(Wrappers.<Duel>lambdaQuery()
                .and(w -> w.eq(Duel::getChallengerId, userId).or().eq(Duel::getOpponentId, userId))
                .orderByDesc(Duel::getId).last("limit 20"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (Duel d : list) {
            boolean mine = Objects.equals(d.getChallengerId(), userId);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", d.getId());
            m.put("opponent", mine ? d.getOpponentName() : d.getChallengerName());
            m.put("myScore", mine ? d.getMyScore() : d.getOpScore());
            m.put("opScore", mine ? d.getOpScore() : d.getMyScore());
            m.put("result", d.getResult());
            m.put("time", d.getCreateTime() == null ? "" : d.getCreateTime().toString());
            out.add(m);
        }
        return out;
    }

    /** 用宝库条目出题：问条目属于哪一门类 */
    private List<Map<String, Object>> buildDuelQuestions() {
        List<Item> pool = itemMapper.selectList(Wrappers.<Item>lambdaQuery()
                .eq(Item::getStatus, 1).orderByAsc(Item::getId).last("limit 200"));
        if (pool.size() < 4) return new ArrayList<>();
        Map<Long, String> catNames = categoryNameMap();
        List<String> allCats = new ArrayList<>(catNames.values());
        Random rnd = new Random();
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < DUEL_QUESTIONS && !pool.isEmpty(); i++) {
            Item it = pool.get(rnd.nextInt(pool.size()));
            String right = catNames.get(it.getCategoryId());
            if (right == null) continue;
            List<String> options = new ArrayList<>();
            options.add(right);
            int guard = 0;
            while (options.size() < 4 && guard++ < 100 && allCats.size() > options.size()) {
                String c = allCats.get(rnd.nextInt(allCats.size()));
                if (!options.contains(c)) options.add(c);
            }
            Collections.shuffle(options, rnd);
            Map<String, Object> q = new LinkedHashMap<>();
            q.put("title", "「" + it.getTitle() + "」属何门类？");
            q.put("options", options);
            q.put("answerIdx", options.indexOf(right));
            list.add(q);
        }
        return list;
    }

    /** 去掉答案字段后再发给前端 */
    private List<Map<String, Object>> publicQuestions(List<Map<String, Object>> questions) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Map<String, Object> q : questions) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("title", q.get("title"));
            m.put("options", q.get("options"));
            out.add(m);
        }
        return out;
    }

    /* ------------------------------ 传书私信 ------------------------------ */
    @Override
    public Map<String, Object> inbox(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        List<Message> list = messageMapper.selectList(Wrappers.<Message>lambdaQuery()
                .eq(Message::getToId, userId).orderByDesc(Message::getId).last("limit 50"));
        List<Map<String, Object>> msgs = new ArrayList<>();
        int unread = 0;
        for (Message m : list) {
            Map<String, Object> mm = new LinkedHashMap<>();
            mm.put("id", m.getId());
            mm.put("fromName", m.getFromName());
            mm.put("content", m.getContent());
            mm.put("isRead", m.getIsRead());
            mm.put("time", m.getCreateTime() == null ? "" : m.getCreateTime().toString());
            msgs.add(mm);
            if (m.getIsRead() == null || m.getIsRead() == 0) unread++;
        }
        // 可传书对象：其他注册山友
        List<User> others = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .ne(User::getId, userId).eq(User::getStatus, 1).orderByAsc(User::getId).last("limit 12"));
        List<Map<String, Object>> targets = new ArrayList<>();
        for (User ou : others) {
            Map<String, Object> t = new LinkedHashMap<>();
            t.put("username", ou.getUsername());
            t.put("element", ou.getElement());
            targets.add(t);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("messages", msgs);
        out.put("unread", unread);
        out.put("targets", targets);
        return out;
    }

    @Override
    public Map<String, Object> send(String authorization, String toName, String content) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        String username = (String) u[1];
        if (toName == null || toName.isBlank()) {
            throw new BizException("请写明收信人");
        }
        if (content == null || content.isBlank()) {
            throw new BizException("信不可无字");
        }
        if (content.length() > 500) {
            throw new BizException("信文过长，请精简");
        }
        User to = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, toName.trim()).eq(User::getStatus, 1));
        if (to == null) {
            throw new BizException("查无此人「" + toName + "」");
        }
        if (Objects.equals(to.getId(), userId)) {
            throw new BizException("不可自寄");
        }
        Message m = new Message();
        m.setFromId(userId);
        m.setFromName(username);
        m.setToId(to.getId());
        m.setToName(to.getUsername());
        m.setContent(content.trim());
        m.setIsRead(0);
        m.setCreateTime(LocalDateTime.now());
        messageMapper.insert(m);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("id", m.getId());
        out.put("toName", to.getUsername());
        return out;
    }

    @Override
    public Map<String, Object> read(String authorization, Long messageId) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        Message m = messageMapper.selectById(messageId);
        if (m == null || !Objects.equals(m.getToId(), userId)) {
            throw new BizException("查无此信");
        }
        m.setIsRead(1);
        messageMapper.updateById(m);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    /** 一键全部已读 */
    @Override
    public Map<String, Object> readAll(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        messageMapper.update(null, Wrappers.<Message>lambdaUpdate()
                .eq(Message::getToId, userId).eq(Message::getIsRead, 0)
                .set(Message::getIsRead, 1));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    /* ------------------------------ 工具 ------------------------------ */
    private Map<Long, Item> itemMap() {
        Map<Long, Item> m = new HashMap<>();
        for (Item it : itemMapper.selectList(null)) {
            m.put(it.getId(), it);
        }
        return m;
    }

    private String itemTitle(Long itemId) {
        Item it = itemMapper.selectById(itemId);
        return it == null ? "未知之物" : it.getTitle();
    }

    private Map<Long, String> categoryNameMap() {
        Map<Long, String> m = new HashMap<>();
        for (Category c : categoryMapper.selectList(null)) m.put(c.getId(), c.getName());
        return m;
    }

    private Map<String, String> gradeNameMap() {
        Map<String, String> m = new HashMap<>();
        for (Grade g : gradeMapper.selectList(null)) m.put(g.getGradeKey(), g.getName());
        return m;
    }

    private List<Map<String, Object>> parseMaterials(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> parseList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            return "[]";
        }
    }

    /** 解析 Bearer 令牌并确认是有效注册用户 */
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
