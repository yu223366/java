package com.fenglin.springboottest.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fenglin.springboottest.common.BizException;
import com.fenglin.springboottest.entity.*;
import com.fenglin.springboottest.mapper.*;
import com.fenglin.springboottest.service.EventService;
import com.fenglin.springboottest.service.GameService;
import com.fenglin.springboottest.service.SpiritWalletService;
import com.fenglin.springboottest.util.TokenUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 山门玩法实现：测灵根 / 每日签到 / 每日一题。
 *
 * <p>测灵根的题目与答案都放在服务端，前端只渲染选项并提交下标，
 * 避免改前端就能伪造灵根；每日一题的题目按日期随机生成后落库，
 * 保证所有山友面对同一道题。</p>
 */
@Service
public class GameServiceImpl implements GameService {

    /** 连续签到 1~7 天的灵石奖励 */
    private static final int[] SIGN_REWARD = {10, 20, 30, 40, 50, 60, 100};

    /** 重测灵根：首测免费，再测每次 100 灵石，且每日至多 5 次 */
    private static final int RETEST_COST = 100;
    private static final int RETEST_DAILY_LIMIT = 5;

    /** 每旬一题：十分钟一轮，答对得 5 灵石 */
    private static final int SLOT_MINUTES = 10;
    private static final int ANSWER_REWARD = 5;

    /** 练功：30 秒一次，每次 3 灵石 + 10 修为，满 100 修为升一重 */
    private static final int PRACTICE_COOLDOWN_SECONDS = 30;
    private static final int PRACTICE_SPIRIT = 3;
    private static final int PRACTICE_EXP = 10;
    private static final int PRACTICE_LEVEL_UP_SPIRIT = 20;

    /** 测灵根题库：每题 4 个选项，各对应一种五行倾向 */
    private static final class Quiz {
        final String title;
        final String[] options;
        final String[] elements;

        Quiz(String title, String[] options, String[] elements) {
            this.title = title;
            this.options = options;
            this.elements = elements;
        }
    }

    private static final List<Quiz> QUIZ = List.of(
            new Quiz("遇险时，你的第一反应是？",
                    new String[]{"挡在同伴身前", "先看清退路", "直取要害", "静观其变"},
                    new String[]{"huo", "tu", "jin", "shui"}),
            new Quiz("修行路上，你更看重什么？",
                    new String[]{"根骨", "心性", "悟性", "感应"},
                    new String[]{"tu", "shui", "jin", "feng"}),
            new Quiz("你独爱哪般景致？",
                    new String[]{"烈焰焚天", "松林深处", "长风过岭", "寒潭照月"},
                    new String[]{"huo", "mu", "feng", "shui"}),
            new Quiz("面对万灵石与侠名，你？",
                    new String[]{"淡然弃之", "兼取之", "守护同门", "开拓基业"},
                    new String[]{"shui", "huo", "tu", "jin"}),
            new Quiz("你的短处在于？",
                    new String[]{"性急如火", "多疑如雾", "固执如山", "孤高如锋"},
                    new String[]{"huo", "shui", "tu", "jin"}),
            new Quiz("若择一兵器在手？",
                    new String[]{"长剑", "折扇", "灵杖", "灵珠"},
                    new String[]{"jin", "feng", "mu", "dian"}),
            new Quiz("师父授你绝技，你要哪一门？",
                    new String[]{"攻伐", "身法", "疗愈", "护体"},
                    new String[]{"jin", "feng", "mu", "tu"}),
            new Quiz("若得灵根，你愿为哪般？",
                    new String[]{"至刚锋锐", "至柔流转", "长生不朽", "焚天灭地"},
                    new String[]{"jin", "shui", "mu", "huo"})
    );

    private final SpiritProfileMapper spiritProfileMapper;
    private final SignInMapper signInMapper;
    private final QuestionSlotMapper questionSlotMapper;
    private final QuestionAnswerMapper questionAnswerMapper;
    private final PracticeMapper practiceMapper;
    private final SpiritRootMapper spiritRootMapper;
    private final GradeMapper gradeMapper;
    private final ItemMapper itemMapper;
    private final UserMapper userMapper;
    private final SpiritWalletService wallet;
    private final EventService eventService;
    private final ObjectMapper objectMapper;

    public GameServiceImpl(SpiritProfileMapper spiritProfileMapper,
                            SignInMapper signInMapper,
                            QuestionSlotMapper questionSlotMapper,
                            QuestionAnswerMapper questionAnswerMapper,
                            PracticeMapper practiceMapper,
                            SpiritRootMapper spiritRootMapper,
                            GradeMapper gradeMapper,
                            ItemMapper itemMapper,
                            UserMapper userMapper,
                            SpiritWalletService wallet,
                            EventService eventService,
                            ObjectMapper objectMapper) {
        this.spiritProfileMapper = spiritProfileMapper;
        this.signInMapper = signInMapper;
        this.questionSlotMapper = questionSlotMapper;
        this.questionAnswerMapper = questionAnswerMapper;
        this.practiceMapper = practiceMapper;
        this.spiritRootMapper = spiritRootMapper;
        this.gradeMapper = gradeMapper;
        this.itemMapper = itemMapper;
        this.userMapper = userMapper;
        this.wallet = wallet;
        this.eventService = eventService;
        this.objectMapper = objectMapper;
    }

    /* ------------------------------ 测灵根 ------------------------------ */
    @Override
    public Map<String, Object> spiritQuiz() {
        List<Map<String, Object>> qs = new ArrayList<>();
        for (int i = 0; i < QUIZ.size(); i++) {
            Quiz q = QUIZ.get(i);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", i);
            m.put("title", q.title);
            m.put("options", Arrays.asList(q.options));
            qs.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("questions", qs);
        return out;
    }

    @Override
    public Map<String, Object> submitSpirit(String authorization, List<Integer> answers) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        String username = (String) u[1];
        if (answers == null || answers.size() != QUIZ.size()) {
            throw new BizException("请答完 " + QUIZ.size() + " 题");
        }
        // 重测不再无限免费：首测免费，再测每次 100 灵石 + 每日 5 次上限
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long todayCount = spiritProfileMapper.selectCount(Wrappers.<SpiritProfile>lambdaQuery()
                .eq(SpiritProfile::getUserId, userId)
                .ge(SpiritProfile::getCreateTime, todayStart));
        Long totalCount = spiritProfileMapper.selectCount(Wrappers.<SpiritProfile>lambdaQuery()
                .eq(SpiritProfile::getUserId, userId));
        boolean firstTime = totalCount == null || totalCount == 0;
        if (!firstTime) {
            if (todayCount != null && todayCount >= RETEST_DAILY_LIMIT) {
                throw new BizException("今日已重测 " + RETEST_DAILY_LIMIT + " 次，明日再来");
            }
            wallet.deduct(userId, RETEST_COST, "重测灵根");
        }
        Map<String, Integer> scoreMap = new LinkedHashMap<>();
        for (int i = 0; i < QUIZ.size(); i++) {
            Integer pick = answers.get(i);
            if (pick == null || pick < 0 || pick >= QUIZ.get(i).options.length) {
                throw new BizException("第 " + (i + 1) + " 题未作答");
            }
            scoreMap.merge(QUIZ.get(i).elements[pick], 1, Integer::sum);
        }
        String element = "huo";
        int max = -1;
        for (Map.Entry<String, Integer> e : scoreMap.entrySet()) {
            if (e.getValue() > max) {
                max = e.getValue();
                element = e.getKey();
            }
        }
        // 得分越集中，品阶越高：7 分以上为异变，5~6 为天品，4 分为罕见，3 分及以下为普通杂根
        int rare = max >= 7 ? 4 : max >= 5 ? 3 : max >= 4 ? 2 : 1;
        SpiritRoot root = pickRoot(element, rare);

        SpiritProfile p = new SpiritProfile();
        p.setUserId(userId);
        p.setRootName(root.getName());
        p.setElement(root.getElement());
        p.setGradeKey(root.getGradeKey());
        p.setRareLevel(rare);
        p.setScore(max);
        p.setSummary(root.getSummary() == null ? "" : root.getSummary());
        p.setCreateTime(LocalDateTime.now());
        spiritProfileMapper.insert(p);

        eventService.record("山友 " + username + " 灵根鉴定完成，得「" + root.getName()
                + "」（" + rareLabel(rare) + "）。");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rootName", root.getName());
        out.put("element", root.getElement());
        out.put("gradeKey", root.getGradeKey());
        out.put("gradeLabel", gradeName(root.getGradeKey()));
        out.put("rareLevel", rare);
        out.put("rareLabel", rareLabel(rare));
        out.put("score", max);
        out.put("summary", root.getSummary() == null ? "" : root.getSummary());
        out.put("retestCost", firstTime ? 0 : RETEST_COST);
        out.put("balance", wallet.balance(userId));
        return out;
    }

    @Override
    public Map<String, Object> mySpirit(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        SpiritProfile p = spiritProfileMapper.selectOne(Wrappers.<SpiritProfile>lambdaQuery()
                .eq(SpiritProfile::getUserId, userId)
                .orderByDesc(SpiritProfile::getId).last("limit 1"));
        if (p == null) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("hasProfile", false);
            out.put("retestCost", 0);
            out.put("retestCountToday", 0);
            out.put("retestLimit", RETEST_DAILY_LIMIT);
            out.put("canRetest", true);
            return out;
        }
        Long todayCount = spiritProfileMapper.selectCount(Wrappers.<SpiritProfile>lambdaQuery()
                .eq(SpiritProfile::getUserId, userId)
                .ge(SpiritProfile::getCreateTime, LocalDate.now().atStartOfDay()));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("hasProfile", true);
        out.put("rootName", p.getRootName());
        out.put("element", p.getElement());
        out.put("gradeKey", p.getGradeKey());
        out.put("gradeLabel", gradeName(p.getGradeKey()));
        out.put("rareLevel", p.getRareLevel());
        out.put("rareLabel", rareLabel(p.getRareLevel() == null ? 1 : p.getRareLevel()));
        out.put("score", p.getScore());
        out.put("summary", p.getSummary() == null ? "" : p.getSummary());
        out.put("createTime", p.getCreateTime() == null ? "" : p.getCreateTime().toString());
        out.put("retestCost", RETEST_COST);
        out.put("retestCountToday", todayCount == null ? 0 : todayCount.intValue());
        out.put("retestLimit", RETEST_DAILY_LIMIT);
        out.put("canRetest", (todayCount == null ? 0 : todayCount) < RETEST_DAILY_LIMIT);
        out.put("balance", wallet.balance(userId));
        return out;
    }

    /** 按元素与稀有度挑选对应灵根：该元素的品阶里最高 / 中等 / 最低 */
    private SpiritRoot pickRoot(String element, int rare) {
        List<SpiritRoot> list = spiritRootMapper.selectList(Wrappers.<SpiritRoot>lambdaQuery()
                .eq(SpiritRoot::getElement, element).eq(SpiritRoot::getStatus, 1));
        if (list.isEmpty()) {
            list = spiritRootMapper.selectList(Wrappers.<SpiritRoot>lambdaQuery()
                    .eq(SpiritRoot::getStatus, 1));
            if (list.isEmpty()) {
                throw new BizException("灵根谱尚未录入");
            }
            return list.get(0);
        }
        Map<String, Integer> rank = gradeRankMap();
        list.sort(Comparator.comparingInt(r -> rank.getOrDefault(r.getGradeKey(), 99)));
        switch (rare) {
            case 4:
            case 3:
                return list.get(list.size() - 1);
            case 2:
                return list.get(list.size() / 2);
            default:
                return list.get(0);
        }
    }

    /* ------------------------------ 每日签到 ------------------------------ */
    @Override
    public Map<String, Object> signInStatus(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        LocalDate today = LocalDate.now();
        SignIn todayRec = signInMapper.selectOne(Wrappers.<SignIn>lambdaQuery()
                .eq(SignIn::getUserId, userId).eq(SignIn::getSignDate, today));
        SignIn last = signInMapper.selectOne(Wrappers.<SignIn>lambdaQuery()
                .eq(SignIn::getUserId, userId)
                .orderByDesc(SignIn::getSignDate).last("limit 1"));
        Long total = signInMapper.selectCount(Wrappers.<SignIn>lambdaQuery()
                .eq(SignIn::getUserId, userId));
        int days = todayRec != null ? todayRec.getContinuousDays()
                : (last != null && last.getContinuousDays() != null ? last.getContinuousDays() : 0);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("signed", todayRec != null);
        out.put("continuousDays", days);
        out.put("totalDays", total == null ? 0 : total.intValue());
        out.put("nextReward", SIGN_REWARD[Math.min(Math.max(days + (todayRec != null ? 0 : 1), 1), SIGN_REWARD.length) - 1]);
        out.put("balance", wallet.balance(userId));
        return out;
    }

    @Override
    public Map<String, Object> checkIn(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        String username = (String) u[1];
        LocalDate today = LocalDate.now();
        SignIn exist = signInMapper.selectOne(Wrappers.<SignIn>lambdaQuery()
                .eq(SignIn::getUserId, userId).eq(SignIn::getSignDate, today));
        if (exist != null) {
            throw new BizException("今日已签到，明日再来");
        }
        SignIn last = signInMapper.selectOne(Wrappers.<SignIn>lambdaQuery()
                .eq(SignIn::getUserId, userId)
                .orderByDesc(SignIn::getSignDate).last("limit 1"));
        int days = 1;
        if (last != null && last.getSignDate() != null && last.getSignDate().equals(today.minusDays(1))) {
            days = (last.getContinuousDays() == null ? 1 : last.getContinuousDays()) + 1;
        }
        int reward = SIGN_REWARD[Math.min(days, SIGN_REWARD.length) - 1];

        SignIn si = new SignIn();
        si.setUserId(userId);
        si.setSignDate(today);
        si.setContinuousDays(days);
        si.setRewardSpirit(reward);
        si.setCreateTime(LocalDateTime.now());
        signInMapper.insert(si);
        wallet.add(userId, reward, "每日签到");

        if (days % 7 == 0) {
            eventService.record("山友 " + username + " 连签七日，灵泉所赐 " + reward + " 灵石，山门同贺。");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("reward", reward);
        out.put("continuousDays", days);
        out.put("balance", wallet.balance(userId));
        return out;
    }

    /* ------------------------------ 每旬一题（十分钟一轮） ------------------------------ */
    @Override
    public Map<String, Object> dailyQuestion(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        String slot = slotKeyNow();
        QuestionSlot q = questionSlotMapper.selectOne(Wrappers.<QuestionSlot>lambdaQuery()
                .eq(QuestionSlot::getSlotKey, slot));
        if (q == null) {
            q = generateSlotQuestion(slot);
            questionSlotMapper.insert(q);
        }
        QuestionAnswer ans = questionAnswerMapper.selectOne(Wrappers.<QuestionAnswer>lambdaQuery()
                .eq(QuestionAnswer::getUserId, userId).eq(QuestionAnswer::getSlotKey, slot));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("question", q.getQuestion());
        out.put("options", parseOptions(q.getOptionsJson()));
        out.put("reward", q.getRewardSpirit());
        out.put("answered", ans != null);
        out.put("correct", ans != null && ans.getIsCorrect() != null && ans.getIsCorrect() == 1);
        out.put("slotKey", slot);
        out.put("slotMinutes", SLOT_MINUTES);
        out.put("nextSlotSeconds", secondsToNextSlot());
        out.put("date", LocalDate.now().toString());
        return out;
    }

    @Override
    public Map<String, Object> answerDaily(String authorization, Integer answerIdx) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        String slot = slotKeyNow();
        QuestionAnswer exist = questionAnswerMapper.selectOne(Wrappers.<QuestionAnswer>lambdaQuery()
                .eq(QuestionAnswer::getUserId, userId).eq(QuestionAnswer::getSlotKey, slot));
        if (exist != null) {
            throw new BizException("本轮已答，下一轮 " + SLOT_MINUTES + " 分钟后开启");
        }
        QuestionSlot q = questionSlotMapper.selectOne(Wrappers.<QuestionSlot>lambdaQuery()
                .eq(QuestionSlot::getSlotKey, slot));
        if (q == null) throw new BizException("本轮题目尚未开启");
        List<String> options = parseOptions(q.getOptionsJson());
        if (answerIdx == null || answerIdx < 0 || answerIdx >= options.size()) {
            throw new BizException("请选择一个答案");
        }
        boolean correct = answerIdx.equals(q.getAnswerIdx());
        int reward = correct ? (q.getRewardSpirit() == null ? ANSWER_REWARD : q.getRewardSpirit()) : 0;

        QuestionAnswer a = new QuestionAnswer();
        a.setUserId(userId);
        a.setSlotKey(slot);
        a.setAnswerIdx(answerIdx);
        a.setIsCorrect(correct ? 1 : 0);
        a.setRewardSpirit(reward);
        a.setCreateTime(LocalDateTime.now());
        questionAnswerMapper.insert(a);
        if (correct) wallet.add(userId, reward, "每旬一题");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("correct", correct);
        out.put("answerIdx", q.getAnswerIdx());
        out.put("reward", reward);
        out.put("nextSlotSeconds", secondsToNextSlot());
        out.put("balance", wallet.balance(userId));
        return out;
    }

    /** 十分钟一槽：形如 2026-11-11-1430，同一槽全站同题 */
    private String slotKeyNow() {
        LocalDateTime now = LocalDateTime.now();
        int bucket = (now.getMinute() / SLOT_MINUTES) * SLOT_MINUTES;
        return String.format("%s-%02d%02d", now.toLocalDate(), now.getHour(), bucket);
    }

    /** 距下一轮的秒数（前端倒计时用） */
    private int secondsToNextSlot() {
        LocalDateTime now = LocalDateTime.now();
        int bucket = (now.getMinute() / SLOT_MINUTES) * SLOT_MINUTES;
        LocalDateTime next = now.withMinute(bucket).withSecond(0).withNano(0)
                .plusMinutes(SLOT_MINUTES);
        return Math.max(1, (int) java.time.Duration.between(now, next).getSeconds());
    }

    /** 按时间槽从宝库条目里出一道「描述出自哪部典籍」，同槽题目固定 */
    private QuestionSlot generateSlotQuestion(String slot) {
        List<Item> pool = itemMapper.selectList(Wrappers.<Item>lambdaQuery()
                .eq(Item::getStatus, 1).orderByAsc(Item::getId).last("limit 300"));
        if (pool.size() < 4) throw new BizException("题库不足，暂不开放每旬一题");
        Random rnd = new Random(slot.hashCode());
        Item right = pool.get(rnd.nextInt(pool.size()));
        List<String> options = new ArrayList<>();
        options.add(right.getTitle());
        int guard = 0;
        while (options.size() < 4 && guard++ < 300) {
            Item it = pool.get(rnd.nextInt(pool.size()));
            if (it.getTitle() != null && !options.contains(it.getTitle())) options.add(it.getTitle());
        }
        Collections.shuffle(options, rnd);

        QuestionSlot q = new QuestionSlot();
        q.setSlotKey(slot);
        q.setQuestion("以下描述，出自哪一部典籍？\n"
                + (right.getSummary() == null ? "" : right.getSummary()));
        q.setOptionsJson(toJson(options));
        q.setAnswerIdx(options.indexOf(right.getTitle()));
        q.setRewardSpirit(ANSWER_REWARD);
        q.setCreateTime(LocalDateTime.now());
        return q;
    }

    /* ------------------------------ 练功（每 30 秒一次） ------------------------------ */
    @Override
    public Map<String, Object> practiceStatus(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        Practice p = practiceOf(userId);
        int cd = cooldownOf(p);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("level", p.getLevel() == null ? 1 : p.getLevel());
        out.put("exp", p.getExp() == null ? 0 : p.getExp());
        out.put("expMax", 100);
        out.put("totalCount", p.getTotalCount() == null ? 0 : p.getTotalCount());
        out.put("totalSpirit", p.getTotalSpirit() == null ? 0 : p.getTotalSpirit());
        out.put("cooldownSeconds", cd);
        out.put("cooldown", PRACTICE_COOLDOWN_SECONDS);
        out.put("canPractice", cd <= 0);
        out.put("rewardSpirit", PRACTICE_SPIRIT);
        out.put("rewardExp", PRACTICE_EXP);
        out.put("balance", wallet.balance(userId));
        return out;
    }

    @Override
    public Map<String, Object> practiceRun(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        String username = (String) u[1];
        Practice p = practiceOf(userId);
        int cd = cooldownOf(p);
        if (cd > 0) throw new BizException("气息未平，尚需 " + cd + " 秒方可再练");

        int level = p.getLevel() == null ? 1 : p.getLevel();
        int exp = (p.getExp() == null ? 0 : p.getExp()) + PRACTICE_EXP;
        int gain = PRACTICE_SPIRIT;
        boolean levelUp = false;
        if (exp >= 100) {
            exp -= 100;
            level += 1;
            levelUp = true;
            gain += PRACTICE_LEVEL_UP_SPIRIT;
        }
        p.setLevel(level);
        p.setExp(exp);
        p.setTotalCount((p.getTotalCount() == null ? 0 : p.getTotalCount()) + 1);
        p.setTotalSpirit((p.getTotalSpirit() == null ? 0 : p.getTotalSpirit()) + gain);
        p.setLastTime(LocalDateTime.now());
        practiceMapper.updateById(p);
        wallet.add(userId, gain, levelUp ? "练功突破 · 第 " + level + " 重" : "练功");
        if (levelUp) {
            eventService.record("山友 " + username + " 练功突破，入第 " + level + " 重，山门同贺。");
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("gain", gain);
        out.put("level", level);
        out.put("exp", exp);
        out.put("levelUp", levelUp);
        out.put("totalCount", p.getTotalCount());
        out.put("cooldownSeconds", PRACTICE_COOLDOWN_SECONDS);
        out.put("balance", wallet.balance(userId));
        return out;
    }

    /** 取练功记录，没有就建一条 */
    private Practice practiceOf(Long userId) {
        Practice p = practiceMapper.selectOne(Wrappers.<Practice>lambdaQuery()
                .eq(Practice::getUserId, userId).last("limit 1"));
        if (p != null) return p;
        Practice np = new Practice();
        np.setUserId(userId);
        np.setLevel(1);
        np.setExp(0);
        np.setTotalCount(0);
        np.setTotalSpirit(0);
        np.setCreateTime(LocalDateTime.now());
        practiceMapper.insert(np);
        return np;
    }

    private int cooldownOf(Practice p) {
        if (p.getLastTime() == null) return 0;
        long passed = java.time.Duration.between(p.getLastTime(), LocalDateTime.now()).getSeconds();
        return (int) Math.max(0, PRACTICE_COOLDOWN_SECONDS - passed);
    }

    /* ------------------------------ 灵石钱包 ------------------------------ */
    @Override
    public Map<String, Object> wallet(String authorization) {
        Object[] u = requireUser(authorization);
        Long userId = (Long) u[0];
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("balance", wallet.balance(userId));
        out.put("logs", wallet.logs(userId, 20));
        return out;
    }

    /* ------------------------------ 工具 ------------------------------ */
    private List<String> parseOptions(String json) {
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
            return list == null ? new ArrayList<>() : list;
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

    private Map<String, Integer> gradeRankMap() {
        Map<String, Integer> m = new HashMap<>();
        for (Grade g : gradeMapper.selectList(null)) {
            m.put(g.getGradeKey(), g.getGradeRank() == null ? 99 : g.getGradeRank());
        }
        return m;
    }

    private String gradeName(String key) {
        for (Grade g : gradeMapper.selectList(null)) {
            if (Objects.equals(g.getGradeKey(), key)) return g.getName();
        }
        return "凡品";
    }

    private static String rareLabel(int rare) {
        switch (rare) {
            case 4:
                return "异 变";
            case 3:
                return "天 品";
            case 2:
                return "罕 见";
            default:
                return "普 通";
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
